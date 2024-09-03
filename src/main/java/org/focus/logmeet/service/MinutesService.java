package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.domain.Minutes;
import org.focus.logmeet.domain.Project;
import org.focus.logmeet.repository.MinutesRepository;
import org.focus.logmeet.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.domain.enums.Status.ACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinutesService {

    private final S3Service s3Service;
    private final MinutesRepository minutesRepository;
    private final ProjectRepository projectRepository;
    private final WebClient webClient;

    // 음성을 업로드 하고, 텍스트 추출 후 회의록 저장
    public Mono<Minutes> processAndUploadVoice(String base64FileData, String minutesName, String fileName, Long projectId) {
        Project project = projectRepository.findById(projectId) // non-blocking 에서 blocking call 이 starvation 을 유발할 수 있다.. 뭐지?
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        // 새로운 Minutes 엔티티 생성
        Minutes minutes = new Minutes();
        minutes.setProject(project);
        minutes.setName(minutesName);
        minutes.setStatus(ACTIVE);

        File tempFile = decodeBase64ToFile(base64FileData, fileName);

        try {
            // 음성 파일을 S3에 업로드
            s3Service.uploadFile("minutes_voice", fileName, tempFile);
            minutes.setVoiceFilePath("minutes_voice/" + fileName);
        } catch (Exception e) {
            log.error("Object Storage에 음성 파일 업로드 중 오류 발생", e);
            throw new BaseException(MINUTES_VOICE_FILE_UPLOAD_ERROR);
        }

        // Flask 서버에 비동기 요청 보내기
        return webClient.post()
                .uri("http://localhost:5001/process_audio") // TODO: 서버 URI 설정 필요
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> Mono.fromCallable(() -> {
                    try {
                        String textFileName = (fileName != null) ? fileName.replace(".wav", ".txt") : "default_name.txt";
                        Path textFilePath = Paths.get(System.getProperty("java.io.tmpdir"), textFileName);

                        Files.writeString(textFilePath, response, StandardCharsets.UTF_8);

                        // 변환된 텍스트를 Object Storage에 업로드
                        s3Service.uploadFile("minutes_text", textFileName, textFilePath.toFile());
                        minutes.setContent(response);

                        // 새로운 Minutes 엔티티 저장
                        minutesRepository.save(minutes);

                    } catch (IOException e) {
                        log.error("텍스트 파일 저장 중 오류 발생", e);
                        throw new BaseException(MINUTES_TEXT_FILE_SAVE_ERROR);
                    } catch (Exception e) {
                        log.error("Object Storage에 텍스트 파일 업로드 중 오류 발생", e);
                        throw new BaseException(MINUTES_TEXT_FILE_UPLOAD_ERROR);
                    }
                    return minutes; // 작업이 완료되면 Minutes 객체를 반환
                }).subscribeOn(Schedulers.boundedElastic()))
                .onErrorResume(e -> {
                    log.error("Flask 서버와의 통신 중 오류 발생", e);
                    return Mono.error(new BaseException(MINUTES_FLASK_SERVER_COMMUNICATION_ERROR));
                });
    }

    public Long uploadPhoto(String base64FileData, String minutesName, String fileName, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        Minutes minutes = new Minutes();
        minutes.setProject(project);
        minutes.setName(minutesName);

        File tempFile = decodeBase64ToFile(base64FileData, fileName);

        try {
            // 사진 파일을 S3에 업로드
            s3Service.uploadFile("minutes_photo", fileName, tempFile);
            minutes.setPhotoFilePath("minutes_photo/" + fileName);
        } catch (Exception e) {
            log.error("Object Storage에 사진 파일 업로드 중 오류 발생", e);
            throw new BaseException(MINUTES_PHOTO_FILE_UPLOAD_ERROR);
        }

        // Minutes 객체 저장
        minutesRepository.save(minutes);
        return minutes.getId();
    }

    public Long saveAndUploadManualEntry(String textContent, String minutesName, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        Minutes minutes = new Minutes();
        minutes.setProject(project);
        minutes.setName(minutesName);
        minutes.setContent(textContent);

        // Minutes 객체 저장
        minutesRepository.save(minutes);
        return minutes.getId();
    }

    // Base64 문자열을 파일로 디코딩하는 메서드
    private File decodeBase64ToFile(String base64FileData, String fileName) {
        // Base64 데이터를 디코딩
        byte[] decodedBytes = Base64.getDecoder().decode(base64FileData);

        // 파일을 임시 디렉터리에 저장
        File tempFile = new File(System.getProperty("java.io.tmpdir"), fileName);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(decodedBytes);
        } catch (IOException e) {
            throw new RuntimeException("파일 디코딩 중 오류가 발생했습니다.", e);
        }

        // 생성된 파일 객체 반환
        return tempFile;
    }
}
