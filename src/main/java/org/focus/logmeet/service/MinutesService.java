package org.focus.logmeet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.minutes.*;
import org.focus.logmeet.controller.dto.schedule.ScheduleDto;
import org.focus.logmeet.domain.*;
import org.focus.logmeet.domain.enums.MinutesType;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.repository.MinutesRepository;
import org.focus.logmeet.repository.ProjectRepository;
import org.focus.logmeet.repository.ScheduleRepository;
import org.focus.logmeet.repository.UserProjectRepository;
import org.focus.logmeet.security.annotation.CurrentUser;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.domain.enums.MinutesType.*;
import static org.focus.logmeet.domain.enums.Role.LEADER;
import static org.focus.logmeet.domain.enums.Status.ACTIVE;
import static org.focus.logmeet.domain.enums.Status.TEMP;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinutesService { //TODO: 현재 유저 정보 검증 로직 중복 최소화 필요

    private final S3Service s3Service;
    private final MinutesRepository minutesRepository;
    private final ProjectRepository projectRepository;
    private final UserProjectRepository userProjectRepository;
    private final ScheduleRepository scheduleRepository;
    private final RestTemplate restTemplate;

    @Value("${flask.server.url}")
    private String flaskServerUrl;

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

    public PreSignedUrlResponse generatePreSignedUrl(String fileName, MinutesType fileType) {
        String directory;
        String contentType;

        // 고유한 파일명 생성
        String uniqueFileName = UUID.randomUUID() + "_" + fileName;

        if (fileType == MinutesType.VOICE) {
            directory = "minutes_voice";
            contentType = "audio/mpeg";
        } else if (fileType == MinutesType.PICTURE) {
            directory = "minutes_photo";
            contentType = "image/jpeg";
        } else {
            throw new BaseException(MINUTES_UNSUPPORTED_TYPE);
        }

        String bucketBaseUrl = "https://kr.object.ncloudstorage.com/logmeet";
        String filePath = bucketBaseUrl + "/" + directory + "/" + uniqueFileName;

        String preSignedUrl = s3Service.generatePreSignedUrl(directory, uniqueFileName, contentType);

        return new PreSignedUrlResponse(preSignedUrl, filePath);
    }
    
    // 파일 업로드 후 임시 회의록 생성
    @Transactional
    public MinutesFileUploadResponse createMinutes(String filePath) {
        log.info("파일 path로 임시 회의록 생성 시도: filePath={}", filePath);

        Minutes minutes = new Minutes();
        MinutesType fileType;
        if (filePath.contains("minutes_voice")) {
            fileType = VOICE;
        } else if (filePath.contains("minutes_photo")) {
            fileType = PICTURE;
        } else {
            fileType = MANUAL;
        }
        minutes.setType(fileType);
        minutes.setStatus(TEMP);  // 임시 상태로 설정
        minutes.setFilePath(filePath);

        String content = switch (fileType) {
            case VOICE -> processVoice(filePath, minutes);
            case PICTURE -> processPicture(filePath, minutes);
            case MANUAL -> "";
        };

        if (content != null) {
            String clearContent = extractClearContent(content, fileType);
            minutes.setClearContent(clearContent);
        }

        log.info("임시 회의록 저장 시도: fileType={}", fileType);
        // 임시 회의록 저장
        minutesRepository.save(minutes);
        log.info("임시 회의록 저장 완료: minutesId={}, fileType={}", minutes.getId(), fileType);

        return new MinutesFileUploadResponse(minutes.getId(), minutes.getFilePath(), minutes.getType());
    }

    // 음성 파일을 Flask 서버에 텍스트 변환 요청 처리
    protected String processVoice(String filePath, Minutes minutes) {
        try {
            String audioProcessingUrl = flaskServerUrl + "/process_audio";
            String content = processFileToText(filePath, audioProcessingUrl);  // 텍스트 변환 요청
            minutes.setContent(content);
            return content;
        } catch (Exception e) {
            log.error("음성 파일 텍스트 처리 중 오류 발생", e);
            throw new BaseException(MINUTES_FLASK_SERVER_COMMUNICATION_ERROR);
        }
    }

    // 사진 파일을 Flask 서버에 이미지 텍스트 변환 요청
    protected String processPicture(String filePath, Minutes minutes) {
        try {
            String imageProcessingUrl = flaskServerUrl + "/process_image";
            String content = processFileToText(filePath, imageProcessingUrl); // 이미지 텍스트 변환 요청
            minutes.setContent(content);
            return content;
        } catch (Exception e) {
            log.error("사진 파일 업로드 또는 텍스트 처리 중 오류 발생", e);
            throw new BaseException(MINUTES_FLASK_SERVER_COMMUNICATION_ERROR);
        }
    }

    // 파일을 Flask 서버에 전송하여 텍스트 변환하는 공통 메서드
    protected String processFileToText(String filePath, String flaskUrl) {
        log.info("파일 텍스트 변환 시도: filePath={}, url={}", filePath, flaskUrl); //TODO: 일정 시간 초과 및 파일 크기에 따라 종료

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HashMap<String, String> requestBody = new HashMap<>();
            requestBody.put("filePath", filePath);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            URI uri = UriComponentsBuilder.fromHttpUrl(flaskUrl)
                    .build().toUri();

            ResponseEntity<String> response = restTemplate.postForEntity(uri, requestEntity, String.class);
            log.info("파일 텍스트 변환 성공: filePath={}", filePath);
            return Objects.requireNonNull(response.getBody());

        } catch (Exception e) {
            log.error("파일 텍스트 변환 중 오류 발생", e);
            throw new BaseException(MINUTES_TEXT_SUMMARY_API_CALL_FAILED);
        }
    }

    @Transactional
    @CurrentUser
    public MinutesSummarizeResult summarizeText(Long minutesId) {
        log.info("텍스트 요약 시도: minutesId={}", minutesId);
        User currentUser = CurrentUserHolder.get();
        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        Minutes minutes = minutesRepository.findById(minutesId)
                .orElseThrow(() -> new BaseException(MINUTES_NOT_FOUND));


        Project project = minutes.getProject();
        if (userProjectRepository.findByUserAndProject(currentUser, project).isEmpty()) {
            throw new BaseException(USER_NOT_IN_PROJECT);
        }

        String extractedText = minutes.getContent();
        try {
            String textSummarizationUrl = flaskServerUrl + "/summarize_text";
            URI uri = UriComponentsBuilder.fromHttpUrl(textSummarizationUrl)
                    .build().toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", extractedText);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<MinutesSummarizeResult> response = restTemplate.postForEntity(
                    uri,
                    requestEntity,
                    MinutesSummarizeResult.class
            );

            // 응답 본문 확인을 위한 디버깅
            log.info("응답 본문: {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                MinutesSummarizeResult responseBody = response.getBody();

                if (responseBody != null && responseBody.getSummarizedText() != null) {
                    log.info("요약 API 호출 성공: 요약된 텍스트={}", responseBody.getSummarizedText());
                    minutes.setSummary(responseBody.getSummarizedText());
                    minutesRepository.save(minutes);

                    List<ScheduleDto> schedules = responseBody.getSchedules();
                    if (schedules != null && !schedules.isEmpty()) {
                        for (ScheduleDto scheduleDto : schedules) {
                            try {
                                LocalDateTime scheduleDate = LocalDateTime.parse(scheduleDto.getExtractedScheduleDate());

                                Schedule schedule = Schedule.builder()
                                        .project(minutes.getProject())
                                        .scheduleDate(scheduleDate)
                                        .content(scheduleDto.getExtractedScheduleContent())
                                        .status(ACTIVE)
                                        .build();
                                scheduleRepository.save(schedule);
                            } catch (DateTimeParseException e) {
                                log.error("잘못된 날짜 형식: {}", scheduleDto.getExtractedScheduleDate(), e);
                                throw new BaseException(SCHEDULE_DATE_FORMAT_INVALID);
                            }
                        }
                    }
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
    @CurrentUser
    public MinutesCreateResponse updateMinutesInfo(Long minutesId, String minutesName, Long projectId) {
        log.info("회의록 정보 업데이트 시도: minutesId={}, minutesName={}, projectId={}", minutesId, minutesName, projectId);

        User currentUser = CurrentUserHolder.get();
        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        Minutes minutes = minutesRepository.findById(minutesId)
                .orElseThrow(() -> new BaseException(MINUTES_NOT_FOUND));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        if (userProjectRepository.findByUserAndProject(currentUser, project).isEmpty()) {
            throw new BaseException(USER_NOT_IN_PROJECT);
        }

        minutes.setName(minutesName);
        minutes.setProject(project);
        minutes.setStatus(ACTIVE);  // ACTIVE 상태로 변경

        minutesRepository.save(minutes);

        return new MinutesCreateResponse(minutes.getId(), project.getId());
    }

    // 수동 입력된 회의록을 저장
    @CurrentUser
    public MinutesCreateResponse saveAndUploadManualEntry(String textContent, String minutesName, Long projectId) {
        log.info("직접 회의록 생성 시도: minutesName={}", minutesName);

        User currentUser = CurrentUserHolder.get();
        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        if (userProjectRepository.findByUserAndProject(currentUser, project).isEmpty()) {
            throw new BaseException(USER_NOT_IN_PROJECT);
        }

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


    @CurrentUser
    public MinutesInfoResult getMinutes(Long minutesId) {
        log.info("회의록 정보 조회: minutesId={}", minutesId);
        User currentUser = CurrentUserHolder.get();
        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        Minutes minutes = minutesRepository.findById(minutesId)
                .orElseThrow(() -> new BaseException(MINUTES_NOT_FOUND));

        Project project = minutes.getProject();
        if (userProjectRepository.findByUserAndProject(currentUser, project).isEmpty()) {
            throw new BaseException(USER_NOT_IN_PROJECT);
        }

        return new MinutesInfoResult(
                minutes.getId(),
                project.getId(),
                project.getName(),
                minutes.getName(),
                minutes.getClearContent(),
                minutes.getFilePath(),
                minutes.getSummary(),
                minutes.getType(),
                minutes.getCreatedAt()
        );
    }

    private String extractClearContent(String content, MinutesType type) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(content);

            if (type == MinutesType.VOICE) {
                JsonNode segmentsNode = rootNode.get("segments");
                if (segmentsNode != null && segmentsNode.isArray()) {
                    StringBuilder formattedText = new StringBuilder();

                    for (JsonNode segment : segmentsNode) {
                        String speaker = segment.get("speaker").asText();
                        String text = segment.get("text").asText();

                        formattedText.append(speaker).append(": ").append(text).append("\n");
                    }

                    return formattedText.toString().trim();
                }
            }

            JsonNode textNode = rootNode.get("text");
            if (textNode != null) {
                return textNode.asText();
            }
        } catch (Exception e) {
            log.error("JSON 파싱 중 오류 발생: {}", e.getMessage());
            throw new BaseException(MINUTES_INVALID_JSON_FORMAT);
        }
        return "";
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
                })
                .sorted(Comparator.comparing(MinutesListResult::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @CurrentUser
    public List<MinutesListResult> getProjectMinutes(Long projectId) {
        log.info("프로젝트에 속한 회의록 조회 시도: projectId={}", projectId);
        User currentUser = CurrentUserHolder.get();
        List<Minutes> minutesList = minutesRepository.findAllByProjectId(projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        UserProject memberProject = userProjectRepository.findByUserAndProject(currentUser, project)
                .orElseThrow(() -> new BaseException(USER_NOT_IN_PROJECT));

        if (minutesList.isEmpty()) {
            log.info("프로젝트에 회의록이 없음: projectId={}", projectId);
            return Collections.emptyList();
        }

        return minutesList.stream().map(minutes ->
                new MinutesListResult(
                        minutes.getId(),
                        projectId,
                        minutes.getName(),
                        memberProject.getColor(),
                        minutes.getType(),
                        minutes.getStatus(),
                        minutes.getCreatedAt()
                )
        ).sorted(Comparator.comparing(MinutesListResult::getCreatedAt).reversed())
        .collect(Collectors.toList());
    }

    @Transactional
    @CurrentUser
    public void deleteMinutes(Long minutesId) {
        log.info("회의록 삭제 시도: minutesId={}", minutesId);
        User currentUser = CurrentUserHolder.get();
        Minutes minutes = minutesRepository.findById(minutesId)
                .orElseThrow(() -> new BaseException(MINUTES_NOT_FOUND));
        Long projectId = minutes.getProject().getId();

        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        UserProject leaderProject = userProjectRepository.findByUserAndProject(currentUser, project)
                .orElseThrow(() -> new BaseException(USER_NOT_IN_PROJECT));

        if (!leaderProject.getRole().equals(LEADER)) {
            log.info("권한이 없는 삭제 시도: projectId={}, userId={}", minutesId, leaderProject.getUser().getId());
            throw new BaseException(USER_NOT_LEADER);
        }

        minutesRepository.delete(minutes);
        log.info("회의록 삭제 성공: minutesId={}", minutesId);
    }
}