package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.minutes.*;
import org.focus.logmeet.domain.Minutes;
import org.focus.logmeet.domain.Project;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.domain.UserProject;
import org.focus.logmeet.domain.enums.MinutesType;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.repository.MinutesRepository;
import org.focus.logmeet.repository.ProjectRepository;
import org.focus.logmeet.repository.UserProjectRepository;
import org.focus.logmeet.security.annotation.CurrentUser;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final UserProjectRepository userProjectRepository;
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
                uploadToS3AndProcessPicture(tempFile, fileName, minutes);  // 이미지 처리 수정
                break;

            default:
                throw new BaseException(MINUTES_TYPE_NOT_FOUND);
        }
        log.info("임시 회의록 저장 시도: fileType={}", fileType);
        // 임시 회의록 저장
        minutesRepository.save(minutes);
        log.info("임시 회의록 저장 완료: minutesId={}, fileType={}", minutes.getId(), fileType);

        return new MinutesFileUploadResponse(minutes.getFilePath(), minutes.getType());
    }

    // 음성 파일을 S3에 업로드 및 Flask 서버에 텍스트 변환 요청 처리
    private void uploadToS3AndProcessVoice(File tempFile, String fileName, Minutes minutes) {
        try {
            String contentType = "audio/mpeg";
            String directory = "minutes_voice";
            s3Service.uploadFile(directory, fileName, tempFile, contentType);
            String fileUrl = generateFileUrl(directory, fileName);
            minutes.setFilePath(fileUrl);

            String content = processFileToText(tempFile, fileName, "http://localhost:5001/process_audio");  // 텍스트 변환 요청
            minutes.setContent(content);
        } catch (Exception e) {
            log.error("음성 파일 업로드 또는 텍스트 처리 중 오류 발생", e);
            throw new BaseException(MINUTES_VOICE_FILE_UPLOAD_ERROR);
        }
    }

    // 사진 파일을 S3에 업로드 및 Flask 서버에 이미지 텍스트 변환 요청
    private void uploadToS3AndProcessPicture(File tempFile, String fileName, Minutes minutes) {
        try {
            String contentType = "image/jpeg";
            String directory = "minutes_photo";
            s3Service.uploadFile(directory, fileName, tempFile, contentType);
            String fileUrl = generateFileUrl(directory, fileName);
            minutes.setFilePath(fileUrl);

            String content = processFileToText(tempFile, fileName, "http://localhost:5001/process_image");  // 이미지 텍스트 변환 요청
            minutes.setContent(content);
        } catch (Exception e) {
            log.error("사진 파일 업로드 또는 텍스트 처리 중 오류 발생", e);
            throw new BaseException(MINUTES_PHOTO_FILE_UPLOAD_ERROR);
        }
    }

    // 파일을 Flask 서버에 전송하여 텍스트 변환하는 공통 메서드
    private String processFileToText(File tempFile, String fileName, String flaskUrl) {
        log.info("파일 텍스트 변환 시도: fileName={}, url={}", fileName, flaskUrl); //TODO: 일정 시간 초과 시 통신 종료

        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new FileSystemResource(tempFile));
            builder.part("fileName", fileName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, HttpEntity<?>> multipartRequest = builder.build();
            HttpEntity<MultiValueMap<String, HttpEntity<?>>> requestEntity = new HttpEntity<>(multipartRequest, headers);

            URI uri = UriComponentsBuilder.fromHttpUrl(flaskUrl)
                    .build().toUri();

            ResponseEntity<String> response = restTemplate.postForEntity(uri, requestEntity, String.class);
            log.info("파일 텍스트 변환 성공: fileName={}", fileName);
            return Objects.requireNonNull(response.getBody());

        } catch (Exception e) {
            log.error("파일 텍스트 변환 중 오류 발생", e);
            throw new BaseException(MINUTES_TEXT_FILE_UPLOAD_ERROR);
        }
    }

    // TODO: GPT 요약 API 호출 메서드 추가 보완 필요
    public MinutesSummarizeResponse summarizeText(String extractedText) {
        log.info("텍스트 요약 시도: extractedText={}", extractedText);

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl("http://localhost:5001/summarize_text")
                    .build().toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", extractedText);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<MinutesSummarizeResponse> response = restTemplate.postForEntity(
                    uri,
                    requestEntity,
                    MinutesSummarizeResponse.class
            );

            // 응답 본문 확인을 위한 디버깅
            log.info("응답 본문: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                MinutesSummarizeResponse responseBody = response.getBody();

                if (responseBody != null && responseBody.getSummarizedText() != null) {
                    log.info("요약 API 호출 성공: 요약된 텍스트={}, 일정 정보={}", responseBody.getSummarizedText(), responseBody.getExtractedSchedule());
                    return responseBody;
                } else {
                    log.error("요약 API 응답에 'summary'가 없음: {}", responseBody);
                    throw new BaseException(MINUTES_TEXT_SUMMARY_MISSING);
                }
            } else {
                log.error("요약 API 호출 실패: 상태 코드={}", response.getStatusCode());
                throw new BaseException(MINUTES_TEXT_SUMMARY_API_CALL_FAILED);
            }

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("텍스트 요약 중 오류 발생", e);
            throw new BaseException(MINUTES_TEXT_SUMMARY_ERROR);
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


    public MinutesInfoResult getMinutes(Long minutesId) {
        log.info("회의록 정보 조회: minutesId={}", minutesId);
        Minutes minutes = minutesRepository.findById(minutesId)
                .orElseThrow(() -> new BaseException(MINUTES_NOT_FOUND));
        return new MinutesInfoResult(minutes.getId(), minutes.getProject().getId(), minutes.getName(), minutes.getContent(), minutes.getFilePath(), minutes.getCreatedAt());
    }

    @Transactional
    @CurrentUser //TODO: @Transactional(readOnly = true) 왜 필요?
    public List<MinutesListResult> getMinutesList() {
        User currentUser = CurrentUserHolder.get();

        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        log.info("회의록 리스트 조회 시도: userId={}", currentUser.getId());

        List<UserProject> userProjects = userProjectRepository.findAllByUser(currentUser);

        return userProjects.stream().flatMap(up -> {
            Project project = up.getProject();
            ProjectColor projectColor = up.getColor();

            List<Minutes> minutesList = minutesRepository.findAllByProjectId(project.getId());

            return minutesList.stream().map(minutes ->
                    new MinutesListResult(
                            minutes.getId(),
                            project.getId(),
                            minutes.getName(),
                            projectColor,
                            minutes.getType(),
                            minutes.getStatus(),
                            minutes.getCreatedAt()
                    )
            );
        }).collect(Collectors.toList());
    }

    public List<MinutesListResult> getProjectMinutes(Long projectId) {
        log.info("프로젝트에 속한 회의록 조회 시도: projectId={}", projectId);

        List<Minutes> minutesList = minutesRepository.findAllByProjectId(projectId);

        boolean exists = projectRepository.existsById(projectId);
        if (!exists) {
            throw new BaseException(PROJECT_NOT_FOUND);
        }

        if (minutesList.isEmpty()) {
            log.info("프로젝트에 회의록이 없음: projectId={}", projectId);
            return Collections.emptyList();
        }

        return minutesList.stream().map(minutes ->
                new MinutesListResult(
                        minutes.getId(),
                        projectId,
                        minutes.getName(),
                        null,
                        minutes.getType(),
                        minutes.getStatus(),
                        minutes.getCreatedAt()
                )
        ).collect(Collectors.toList());
    }


    // 파일 이름을 URL 인코딩하여 실제 URL을 생성하는 메서드
    private String generateFileUrl(String directory, String fileName) {
        String baseUrl = "https://kr.object.ncloudstorage.com/logmeet/";
        try {
            // 파일 이름을 URL 인코딩
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8");
            return baseUrl + directory + "/" + encodedFileName;
        } catch (UnsupportedEncodingException e) {
            log.error("파일 이름 URL 인코딩 중 오류 발생: {}", e.getMessage());
            throw new BaseException(MINUTES_FILE_URL_ENCODING_ERROR);
        }
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