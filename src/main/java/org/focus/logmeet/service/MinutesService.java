package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.minutes.MinutesCreateResponse;
import org.focus.logmeet.domain.Minutes;
import org.focus.logmeet.domain.Project;
import org.focus.logmeet.domain.enums.MinutesType;
import org.focus.logmeet.repository.MinutesRepository;
import org.focus.logmeet.repository.ProjectRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.Objects;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.domain.enums.MinutesType.*;
import static org.focus.logmeet.domain.enums.Status.ACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinutesService {

    private final S3Service s3Service;
    private final MinutesRepository minutesRepository;
    private final ProjectRepository projectRepository;
    private final RestTemplate restTemplate;

    // 음성을 업로드 하고, 텍스트 추출 후 회의록 저장
    @Transactional
    public MinutesCreateResponse processAndUploadVoice(String base64FileData, String minutesName, String fileName, Long projectId) {
        log.info("음성 업로드로 회의록 생성 시도: minutesName={}", minutesName);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        // 새로운 Minutes 엔티티 생성
        Minutes minutes = new Minutes();
        minutes.setProject(project);
        minutes.setName(minutesName);
        minutes.setType(VOICE);

        File tempFile = decodeBase64ToFile(base64FileData, fileName);

        try {
            // 음성 파일을 S3에 업로드
            s3Service.uploadFile("minutes_voice", fileName, tempFile);
            minutes.setVoiceFilePath("minutes_voice/" + fileName);
        } catch (Exception e) {
            log.error("Object Storage에 음성 파일 업로드 중 오류 발생", e);
            throw new BaseException(MINUTES_VOICE_FILE_UPLOAD_ERROR);
        }

        try {
            // 파일을 Flask 서버에 전송
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new FileSystemResource(tempFile));
            builder.part("fileName", fileName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, HttpEntity<?>> multipartRequest = builder.build();
            HttpEntity<MultiValueMap<String, HttpEntity<?>>> requestEntity = new HttpEntity<>(multipartRequest, headers);

            URI uri = UriComponentsBuilder.fromHttpUrl("http://localhost:5001/process_audio")
                    .build().toUri();

            ResponseEntity<String> response = restTemplate.postForEntity(uri, requestEntity, String.class);

            log.info("Flask 서버로부터 변환된 텍스트 수신: {}", response.getBody());

            // 변환된 텍스트를 Minutes 객체에 설정
            minutes.setContent(Objects.requireNonNull(response.getBody()));

            // 새로운 Minutes 엔티티 저장
            minutesRepository.save(minutes);

        } catch (Exception e) {
            log.error("텍스트 처리 중 오류 발생", e);
            throw new BaseException(MINUTES_TEXT_FILE_UPLOAD_ERROR);
        }
        log.info("음성 업로드로 회의록 생성 성공: minutesName={}", minutesName);

        return new MinutesCreateResponse(minutes.getId(), minutes.getProject().getId());
    }

    // 사진을 업로드 하고, 회의록 저장
    public MinutesCreateResponse uploadPhoto(String base64FileData, String minutesName, String fileName, Long projectId) {
        log.info("사진 업로드로 회의록 생성 시도: minutesName={}", minutesName);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        Minutes minutes = new Minutes();
        minutes.setProject(project);
        minutes.setName(minutesName);
        minutes.setType(PICTURE);

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

        log.info("사진 업로드로 회의록 생성 성공: minutesName={}", minutesName);

        return new MinutesCreateResponse(minutes.getId(), minutes.getProject().getId());
    }

    // 수동 입력된 회의록을 저장
    public MinutesCreateResponse saveAndUploadManualEntry(String textContent, String minutesName, Long projectId) {
        log.info("직접 회의록 생성 시도: minutesName={}", minutesName);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        Minutes minutes = new Minutes();
        minutes.setProject(project);
        minutes.setName(minutesName);
        minutes.setContent(textContent);
        minutes.setType(MANUAL);

        // Minutes 객체 저장
        minutesRepository.save(minutes);

        log.info("직접 회의록 생성 성공: minutesName={}", minutesName);

        return new MinutesCreateResponse(minutes.getId(), minutes.getProject().getId());
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
            throw new BaseException(S3_FILE_DECODING_ERROR);
        }
        return tempFile;
    }
}