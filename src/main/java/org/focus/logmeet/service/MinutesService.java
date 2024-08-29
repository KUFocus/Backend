package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinutesService {

    private final S3Service s3Service;
    private final WebClient webClient;

    public Mono<String> processAndUploadVoice(String base64FileData, String fileName) {
        File tempFile = decodeBase64ToFile(base64FileData, fileName);

        try {
            // 음성 파일을 Object Storage에 업로드
            s3Service.uploadFile("minutes_voice", fileName, tempFile);
        } catch (Exception e) {
            log.error("Object Storage에 음성 파일 업로드 중 오류 발생", e);
            throw new BaseException(MINUTES_VOICE_FILE_UPLOAD_ERROR);
        }

        // Flask 서버에 비동기 요청 보내기
        return webClient.post()
                .uri("http://localhost:5001/process_audio") // TODO: 서버 uri 설정 필요
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> Mono.fromCallable(() -> {
                    try {
                        String textFileName = (fileName != null) ? fileName.replace(".wav", ".txt") : "default_name.txt";
                        Path textFilePath = Paths.get(System.getProperty("java.io.tmpdir"), textFileName);

                        Files.writeString(textFilePath, response, StandardCharsets.UTF_8);

                        // 변환된 텍스트를 Object Storage에 업로드
                        s3Service.uploadFile("minutes_text", textFileName, textFilePath.toFile());
                    } catch (IOException e) {
                        log.error("텍스트 파일 저장 중 오류 발생", e);
                        throw new BaseException(MINUTES_TEXT_FILE_SAVE_ERROR);
                    } catch (Exception e) {
                        log.error("Object Storage에 텍스트 파일 업로드 중 오류 발생", e);
                        throw new BaseException(MINUTES_TEXT_FILE_UPLOAD_ERROR);
                    }
                    return "Voice processed and text saved successfully.";
                }).subscribeOn(Schedulers.boundedElastic()))
                .onErrorResume(e -> {
                    log.error("Flask 서버와의 통신 중 오류 발생", e);
                    return Mono.error(new BaseException(MINUTES_FLASK_SERVER_COMMUNICATION_ERROR));
                });
    }

    public void uploadPhoto(String base64FileData, String fileName) {
        File tempFile = decodeBase64ToFile(base64FileData, fileName);

        try {
            // 사진 파일을 Object Storage에 업로드
            s3Service.uploadFile("minutes_text", fileName, tempFile);
        } catch (Exception e) {
            log.error("Object Storage에 사진 파일 업로드 중 오류 발생", e);
            throw new BaseException(MINUTES_PHOTO_FILE_UPLOAD_ERROR);
        }
    }

    private File decodeBase64ToFile(String base64Data, String fileName) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
            File tempFile = new File(System.getProperty("java.io.tmpdir") + "/" + fileName);
            Files.write(tempFile.toPath(), decodedBytes);
            return tempFile;
        } catch (IOException e) {
            log.error("파일 디코딩 중 오류 발생", e);
            throw new BaseException(MINUTES_VOICE_FILE_SAVE_ERROR);
        }
    }
}
