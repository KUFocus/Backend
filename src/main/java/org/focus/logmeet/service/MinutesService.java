package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.minutes.MinutesCreateResponse;
import org.focus.logmeet.controller.dto.minutes.MinutesFileUploadResponse;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.domain.enums.MinutesType.MANUAL;
import static org.focus.logmeet.domain.enums.Status.ACTIVE;
import static org.focus.logmeet.domain.enums.Status.TEMP;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinutesService {

    private final S3Service s3Service;
    private final MinutesRepository minutesRepository;
    private final ProjectRepository projectRepository;
    private final RestTemplate restTemplate;

    // 일정 시간이 지난 임시 회의록을 삭제하는 스케줄러를 추가
    @Scheduled(fixedRate = 1800000)
    @Transactional
    public void deleteOldTemporaryMinutes() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        List<Minutes> oldTemporaryMinutes = minutesRepository.findOldTemporaryMinutes(TEMP, thirtyMinutesAgo);
        for (Minutes minutes : oldTemporaryMinutes) {
            log.info("삭제할 임시 회의록: minutesId={}, createdAt={}", minutes.getId(), minutes.getCreatedAt());
            minutesRepository.delete(minutes);
        }
    }

    // 파일 업로드 후 임시 회의록 생성
    @Transactional
    public MinutesFileUploadResponse uploadFile(String base64FileData, String fileName, MinutesType fileType) {
        log.info("파일 업로드로 임시 회의록 생성 시도: fileType={}", fileType);

        // 새로운 임시 상태의 Minutes 엔티티 생성
        Minutes minutes = new Minutes();
        minutes.setType(fileType);
        minutes.setStatus(TEMP);  // 임시 상태로 설정

        File tempFile = decodeBase64ToFile(base64FileData, fileName);

        switch (fileType) {
            case VOICE:
                uploadToS3AndProcessVoice(tempFile, fileName, minutes);
                break;

            case PICTURE:
                uploadToS3ForPicture(tempFile, fileName, minutes);
                break;

            default:
                throw new BaseException(MINUTES_TYPE_NOT_FOUND);
        }
        log.info("임시 회의록 저장 시도: fileType={}", fileType);
        // 임시 회의록 저장
        minutesRepository.save(minutes);
        log.info("임시 회의록 저장 완료: minutesId={}, fileType={}", minutes.getId(), fileType);

        return new MinutesFileUploadResponse(minutes.getVoiceFilePath(), minutes.getType());
    }

    // 임시 파일을 S3에 업로드 및 Flask 서버에 텍스트 변환 요청 처리
    private void uploadToS3AndProcessVoice(File tempFile, String fileName, Minutes minutes) {
        try {
            s3Service.uploadFile("minutes_voice", fileName, tempFile);
            minutes.setVoiceFilePath("minutes_voice/" + fileName);

            String content = processVoiceFile(tempFile, fileName);  // 텍스트 변환 요청
            minutes.setContent(content);
        } catch (Exception e) {
            log.error("음성 파일 업로드 또는 텍스트 처리 중 오류 발생", e);
            throw new BaseException(MINUTES_VOICE_FILE_UPLOAD_ERROR);
        }
    }

    // 사진 파일을 S3에 업로드하는 로직
    private void uploadToS3ForPicture(File tempFile, String fileName, Minutes minutes) {
        try {
            s3Service.uploadFile("minutes_photo", fileName, tempFile);
            minutes.setPhotoFilePath("minutes_photo/" + fileName);
        } catch (Exception e) {
            log.error("사진 파일 업로드 중 오류 발생", e);
            throw new BaseException(MINUTES_PHOTO_FILE_UPLOAD_ERROR);
        }
    }

    // 음성 파일을 Flask 서버에 전송하여 텍스트 변환하는 메서드
    private String processVoiceFile(File tempFile, String fileName) {
        log.info("음성 파일 텍스트 변환 시도: fileName={}", fileName);

        try {
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
            log.info("음성 파일 텍스트 변환 성공: fileName={}", fileName);
            return Objects.requireNonNull(response.getBody());

        } catch (Exception e) {
            log.error("음성 파일 텍스트 변환 중 오류 발생", e);
            throw new BaseException(MINUTES_TEXT_FILE_UPLOAD_ERROR);
        }
    }

    // 회의록 정보 업데이트
    @Transactional
    public MinutesCreateResponse updateMinutesInfo(Long minutesId, String minutesName, Long projectId) {
        log.info("회의록 정보 업데이트 시도: minutesId={}, minutesName={}, projectId={}", minutesId, minutesName, projectId);

        Minutes minutes = minutesRepository.findById(minutesId)
                .orElseThrow(() -> new BaseException(MINUTES_NOT_FOUND));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        minutes.setName(minutesName);
        minutes.setProject(project);
        minutes.setStatus(ACTIVE);  // ACTIVE 상태로 변경

        minutesRepository.save(minutes);

        return new MinutesCreateResponse(minutes.getId(), project.getId());
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
            log.error("파일 디코딩 중 오류 발생: {}", e.getMessage());
            throw new BaseException(S3_FILE_DECODING_ERROR);
        }
        return tempFile;
    }
}