package org.focus.logmeet.service;

import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.focus.logmeet.controller.dto.minutes.*;
import org.focus.logmeet.controller.dto.schedule.ScheduleDto;
import org.focus.logmeet.domain.*;
import org.focus.logmeet.domain.enums.MinutesType;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.domain.enums.Role;
import org.focus.logmeet.domain.enums.Status;
import org.focus.logmeet.repository.MinutesRepository;
import org.focus.logmeet.repository.ProjectRepository;
import org.focus.logmeet.repository.ScheduleRepository;
import org.focus.logmeet.repository.UserProjectRepository;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.domain.enums.Role.LEADER;
import static org.focus.logmeet.domain.enums.Status.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinutesServiceTest {

    @Mock
    private MinutesRepository minutesRepository;
    @Mock
    private S3Service s3Service;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserProjectRepository userProjectRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    private MinutesService minutesService;

    private User mockUser;
    private Project mockProject;
    private Minutes mockMinutes;
    Minutes minutes = new Minutes();


    @BeforeEach
    void setUp() {
        minutes.setName("테스트 회의록");
        minutes.setContent("테스트 회의록 내용입니다.");
        minutes.setSummary("테스트 회의록 요약본입니다.");
        minutes.setStatus(Status.ACTIVE);
        mockUser = mock(User.class);
        mockProject = mock(Project.class);
        mockMinutes = mock(Minutes.class);
        CurrentUserHolder.set(mockUser);
    }

    @Test
    @DisplayName("Minutes 객체 생성 테스트 (빌더 패턴)")
    void createMinutes_WithBuilder_Success() {
        // given
        Project project = new Project();
        String name = "테스트 회의록";
        String content = "테스트 회의록 내용입니다.";
        String summary = "테스트 회의록 요약본입니다.";
        Status status = Status.ACTIVE;
        MinutesType type = MinutesType.VOICE;
        String filePath = "test/file/path";

        // when
        Minutes builtMinutes = Minutes.builder()
                .name(name)
                .content(content)
                .summary(summary)
                .status(status)
                .type(type)
                .filePath(filePath)
                .project(project)
                .build();

        // then
        assertNotNull(builtMinutes);
        assertEquals(name, builtMinutes.getName());
        assertEquals(content, builtMinutes.getContent());
        assertEquals(summary, builtMinutes.getSummary());
        assertEquals(status, builtMinutes.getStatus());
        assertEquals(type, builtMinutes.getType());
        assertEquals(filePath, builtMinutes.getFilePath());
        assertEquals(project, builtMinutes.getProject());
    }

    @Test
    @DisplayName("회의록 이름 필드 설정 및 반환 테스트")
    void nameFieldTest() {
        // when
        String name = minutes.getName();

        // then
        assertNotNull(name);
        assertEquals("테스트 회의록", name);
    }

    @Test
    @DisplayName("회의록 내용 필드 설정 및 반환 테스트")
    void contentFieldTest() {
        // when
        String content = minutes.getContent();

        // then
        assertNotNull(content);
        assertEquals("테스트 회의록 내용입니다.", content);
    }

    @Test
    @DisplayName("회의록 요약 필드 설정 및 반환 테스트")
    void summaryFieldTest() {
        // when
        String summary = minutes.getSummary();

        // then
        assertNotNull(summary);
        assertEquals("테스트 회의록 요약본입니다.", summary);
    }

    @Test
    @DisplayName("회의록 상태 필드 기본값 테스트")
    void statusFieldDefaultTest() {
        // when
        Status status = minutes.getStatus();

        // then
        assertNotNull(status);
        assertEquals(Status.ACTIVE, status);
    }

    @Test
    @DisplayName("한 시간 넘게 TEMP 상태인 회의록 삭제 테스트")
    void deleteOldTemporaryMinutes_Success() {
        // given
        Minutes temporaryMinutes = mock(Minutes.class);
        when(temporaryMinutes.getId()).thenReturn(1L);
        when(minutesRepository.findOldTemporaryMinutes(eq(TEMP), any(LocalDateTime.class)))
                .thenReturn(List.of(temporaryMinutes));

        // when
        minutesService.deleteOldTemporaryMinutes();

        // then
        verify(minutesRepository).delete(temporaryMinutes);
    }

    @Test
    @DisplayName("파일 업로드를 위한 Pre-signed URL 생성 성공 - VOICE 파일 타입")
    void generatePreSignedUrl_Success_VoiceType() {
        // given
        String fileName = "sample.mp3";
        MinutesType fileType = MinutesType.VOICE;
        String expectedUrlPart = "https://kr.object.ncloudstorage.com/logmeet/minutes_voice/";
        when(s3Service.generatePreSignedUrl(anyString(), anyString(), anyString())).thenReturn(expectedUrlPart + "uuid_sample.mp3");

        // when
        PreSignedUrlResponse response = minutesService.generatePreSignedUrl(fileName, fileType);

        // then
        assertNotNull(response);
        assertTrue(response.getUrl().contains(expectedUrlPart));
        assertTrue(response.getFilePath().contains(expectedUrlPart));
    }

    @Test
    @DisplayName("파일 업로드를 위한 Pre-signed URL 생성 성공 - PICTURE 파일 타입")
    void generatePreSignedUrl_Success_PictureType() {
        // given
        String fileName = "image.jpg";
        MinutesType fileType = MinutesType.PICTURE;
        String expectedUrlPart = "https://kr.object.ncloudstorage.com/logmeet/minutes_photo/";
        when(s3Service.generatePreSignedUrl(anyString(), anyString(), anyString())).thenReturn(expectedUrlPart + "uuid_image.jpg");

        // when
        PreSignedUrlResponse response = minutesService.generatePreSignedUrl(fileName, fileType);

        // then
        assertNotNull(response);
        assertTrue(response.getUrl().contains(expectedUrlPart));
        assertTrue(response.getFilePath().contains(expectedUrlPart));
    }


    @Test
    @DisplayName("지원되지 않는 파일 타입 요청 시 예외 발생")
    void generatePreSignedUrl_UnsupportedFileType_ThrowsException() {
        // given
        String fileName = "document.pdf";
        MinutesType unsupportedFileType = MinutesType.MANUAL;

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.generatePreSignedUrl(fileName, unsupportedFileType));
        assertEquals(BaseExceptionResponseStatus.MINUTES_UNSUPPORTED_TYPE, exception.getStatus());
    }


    @Test
    @DisplayName("음성 파일 업로드 후 임시 회의록 생성 성공")
    void uploadFile_Voice_Success() {
        // given
        String filePath = "minutes_voice/file";

        MinutesService spyMinutesService = spy(minutesService);

        doReturn("{\"content\": \"테스트를 위한 회의 내용입니다.\"}")
                .when(spyMinutesService).processFileToText(anyString(), anyString());

        when(minutesRepository.save(any(Minutes.class))).thenAnswer(invocation -> {
            Minutes testMinutes = invocation.getArgument(0);
            testMinutes.setId(1L);
            return testMinutes;
        });

        // when
        MinutesFileUploadResponse response = spyMinutesService.createMinutes(filePath);

        // then
        assertNotNull(response);
        assertEquals(MinutesType.VOICE, response.getFileType());
        verify(minutesRepository).save(any(Minutes.class));
    }

    @Test
    @DisplayName("사진 파일 업로드 후 임시 회의록 생성 성공")
    void uploadFile_Picture_Success() {
        // given
        String filePath = "minutes_photo/file";

        MinutesService spyMinutesService = spy(minutesService);

        doReturn("{\"content\": \"테스트를 위한 회의 내용입니다.\"}")
                .when(spyMinutesService).processFileToText(anyString(), anyString());

        when(minutesRepository.save(any(Minutes.class))).thenAnswer(invocation -> {
            Minutes testMinutes = invocation.getArgument(0);
            testMinutes.setId(2L);
            return testMinutes;
        });

        // when
        MinutesFileUploadResponse response = spyMinutesService.createMinutes(filePath);

        // then
        assertNotNull(response);
        assertEquals(MinutesType.PICTURE, response.getFileType());
        verify(minutesRepository).save(any(Minutes.class));
    }


    @Test
    @DisplayName("파일 텍스트 변환 성공")
    void processFileToText_Success() {
        // given
        String filePath = "file/path";
        String flaskUrl = "http://localhost:5000/process_audio";
        String expectedResponse = "테스트를 위한 회의 내용입니다.";

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // flask url 설정을 해줘야함
        ReflectionTestUtils.setField(minutesService, "flaskServerUrl", flaskUrl);

        // when
        String result = minutesService.processFileToText(filePath ,flaskUrl);

        // then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(restTemplate).postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("파일 텍스트 변환 시 예외 발생")
    void processFileToText_Exception() {
        // given
        String filePath = "file/path";
        String flaskUrl = "http://localhost:5000/process_audio";

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.processFileToText(filePath, flaskUrl));
        assertEquals(MINUTES_TEXT_SUMMARY_API_CALL_FAILED, exception.getStatus());
    }

    @Test
    @DisplayName("음성 파일 업로드 중 예외 발생 시 처리")
    void uploadToS3AndProcessVoice_Exception() {
        // given
        String filePath = "minutes_voice/file";
        Minutes testMinutes = new Minutes();

        // when & then
        assertThrows(BaseException.class, () -> minutesService.processVoice(filePath, testMinutes));
    }

    @Test
    @DisplayName("사진 파일 업로드 중 예외 발생 시 처리")
    void uploadToS3AndProcessPicture_Exception() {
        // given
        String filePath = "minutes_photo/file";
        Minutes testMinutes = new Minutes();

        // when & then
        assertThrows(BaseException.class, () -> minutesService.processPicture(filePath, testMinutes));
    }

    @Test
    @DisplayName("텍스트 요약 및 스케줄 저장 성공")
    void summarizeText_Success() {
        // given
        Long minutesId = 1L;
        String extractedText = "테스트를 위한 회의 내용입니다.";
        UserProject mockUserProject = mock(UserProject.class);

        // ScheduleDto 생성
        ScheduleDto validSchedule = new ScheduleDto();
        validSchedule.setExtractedScheduleDate(LocalDateTime.now().toString());
        validSchedule.setExtractedScheduleContent("테스트를 위한 스케줄 일정입니다.");

        MinutesSummarizeResult summarizeResult = new MinutesSummarizeResult();
        summarizeResult.setSummarizedText("테스트를 했다고 합니다~");
        summarizeResult.setSchedules(Collections.singletonList(validSchedule));

        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(mockMinutes.getContent()).thenReturn(extractedText);
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mockUserProject));

        ReflectionTestUtils.setField(minutesService, "flaskServerUrl", "http://localhost:5000");

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class)))
                .thenReturn(new ResponseEntity<>(summarizeResult, HttpStatus.OK));

        // when
        MinutesSummarizeResult result = minutesService.summarizeText(minutesId);

        // then
        assertNotNull(result);
        assertEquals("테스트를 했다고 합니다~", result.getSummarizedText());
        verify(minutesRepository).save(mockMinutes);
        verify(scheduleRepository).save(any(Schedule.class));
    }


    @Test
    @DisplayName("인증되지 않은 사용자 예외 발생")
    void summarizeText_UnauthenticatedUser_ThrowsException() {
        // given
        Long minutesId = 1L;
        CurrentUserHolder.clear(); // 현재 인증된 유저 제거

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.summarizeText(minutesId));
        assertEquals(USER_NOT_AUTHENTICATED, exception.getStatus());
    }

    @Test
    @DisplayName("프로젝트에 속하지 않은 사용자 예외 발생")
    void summarizeText_UserNotInProject_ThrowsException() {
        // given
        Long minutesId = 1L;

        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.summarizeText(minutesId));
        assertEquals(USER_NOT_IN_PROJECT, exception.getStatus());
    }

    @Test
    @DisplayName("잘못된 날짜 형식 예외 발생")
    void summarizeText_InvalidDateFormat_ThrowsException() {
        // given
        Long minutesId = 1L;
        String extractedText = "테스트를 위한 회의 내용입니다.";
        UserProject mockUserProject = mock(UserProject.class);

        ScheduleDto invalidSchedule = new ScheduleDto();
        invalidSchedule.setExtractedScheduleDate("우악 날짜가 이상해");
        invalidSchedule.setExtractedScheduleContent("테스트를 위한 스케줄 일정입니다.");

        MinutesSummarizeResult summarizeResult = new MinutesSummarizeResult();
        summarizeResult.setSummarizedText("잘못된 날짜를 입력했다고 합니다.");
        summarizeResult.setSchedules(Collections.singletonList(invalidSchedule));

        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(mockMinutes.getContent()).thenReturn(extractedText);
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mockUserProject));

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class)))
                .thenReturn(new ResponseEntity<>(summarizeResult, HttpStatus.OK));

        ReflectionTestUtils.setField(minutesService, "flaskServerUrl", "http://localhost:5000");

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.summarizeText(minutesId));
        assertEquals(SCHEDULE_DATE_FORMAT_INVALID, exception.getStatus());
    }

    @Test
    @DisplayName("요약 API 응답에 'summary'가 없을 때 예외 발생")
    void summarizeText_MissingSummary_ThrowsException() {
        // given
        Long minutesId = 1L;
        String extractedText = "테스트를 위한 회의 내용입니다.";
        UserProject mockUserProject = mock(UserProject.class);

        MinutesSummarizeResult summarizeResult = new MinutesSummarizeResult();

        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(mockMinutes.getContent()).thenReturn(extractedText);
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mockUserProject));

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class)))
                .thenReturn(new ResponseEntity<>(summarizeResult, HttpStatus.OK));

        ReflectionTestUtils.setField(minutesService, "flaskServerUrl", "http://localhost:5000");

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.summarizeText(minutesId));
        assertEquals(MINUTES_TEXT_SUMMARY_MISSING, exception.getStatus());
    }

    @Test
    @DisplayName("요약 API 호출 실패 시 예외 발생")
    void summarizeText_ApiCallFailed_ThrowsException() {
        // given
        Long minutesId = 1L;
        String extractedText = "테스트를 위한 회의 내용입니다.";
        UserProject mockUserProject = mock(UserProject.class);

        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(mockMinutes.getContent()).thenReturn(extractedText);
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mockUserProject));

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));

        ReflectionTestUtils.setField(minutesService, "flaskServerUrl", "http://localhost:5000");

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.summarizeText(minutesId));
        assertEquals(MINUTES_TEXT_SUMMARY_API_CALL_FAILED, exception.getStatus());
    }

    @Test
    @DisplayName("회의록 요약중 알 수 없는 오류 발생 시 예외 처리")
    void summarizeText_UnknownError_ThrowsException() {
        // given
        Long minutesId = 1L;
        String extractedText = "테스트를 위한 회의 내용입니다.";
        UserProject mockUserProject = mock(UserProject.class);

        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(mockMinutes.getContent()).thenReturn(extractedText);
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mockUserProject));

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class)))
                .thenThrow(new RuntimeException());

        ReflectionTestUtils.setField(minutesService, "flaskServerUrl", "http://localhost:5000");

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.summarizeText(minutesId));
        assertEquals(MINUTES_TEXT_SUMMARY_ERROR, exception.getStatus());
    }

    @Test
    @DisplayName("회의록 정보 업데이트 성공")
    void updateMinutesInfo_Success() {
        // given
        Long minutesId = 1L;
        Long projectId = 1L;
        String minutesName = "Updated Minutes";
        UserProject mockUserProject = mock(UserProject.class);

        CurrentUserHolder.set(mockUser);
        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.of(mockUserProject));
        when(mockMinutes.getId()).thenReturn(minutesId);
        when(mockProject.getId()).thenReturn(projectId);

        // when
        MinutesCreateResponse response = minutesService.updateMinutesInfo(minutesId, minutesName, projectId);

        // then
        assertNotNull(response);
        assertEquals(minutesId, response.getMinutesId());
        assertEquals(projectId, response.getProjectId());

        verify(mockMinutes).setName(minutesName);
        verify(mockMinutes).setProject(mockProject);
        verify(mockMinutes).setStatus(Status.ACTIVE);
        verify(minutesRepository).save(mockMinutes);
    }

    @Test
    @DisplayName("인증되지 않은 사용자 예외 발생 (회의록 정보 업데이트)")
    void updateMinutesInfo_UnauthenticatedUser_ThrowsException() {
        // given
        Long minutesId = 1L;
        Long projectId = 1L;
        String minutesName = "업데이트된 회의록입니다.";
        CurrentUserHolder.clear();

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.updateMinutesInfo(minutesId, minutesName, projectId));
        assertEquals(USER_NOT_AUTHENTICATED, exception.getStatus());
    }

    @Test
    @DisplayName("회의록을 찾을 수 없을 때 예외 발생")
    void updateMinutesInfo_MinutesNotFound_ThrowsException() {
        // given
        Long minutesId = 1L;
        Long projectId = 1L;
        String minutesName = "업데이트된 회의록입니다.";

        CurrentUserHolder.set(mockUser);
        when(minutesRepository.findById(minutesId)).thenReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.updateMinutesInfo(minutesId, minutesName, projectId));
        assertEquals(MINUTES_NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("프로젝트를 찾을 수 없을 때 예외 발생")
    void updateMinutesInfo_ProjectNotFound_ThrowsException() {
        // given
        Long minutesId = 1L;
        Long projectId = 1L;
        String minutesName = "업데이트된 회의록입니다.";

        CurrentUserHolder.set(mockUser);
        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.updateMinutesInfo(minutesId, minutesName, projectId));
        assertEquals(PROJECT_NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("프로젝트에 속하지 않은 사용자 예외 발생 (회의록 정보 업데이트)")
    void updateMinutesInfo_UserNotInProject_ThrowsException() {
        // given
        Long minutesId = 1L;
        Long projectId = 1L;
        String minutesName = "업데이트된 회의록입니다.";

        CurrentUserHolder.set(mockUser);
        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.updateMinutesInfo(minutesId, minutesName, projectId));
        assertEquals(USER_NOT_IN_PROJECT, exception.getStatus());
    }

    @Test
    @DisplayName("수동으로 입력된 회의록 저장 성공")
    void saveAndUploadManualEntry_Success() {
        // given
        Long projectId = 1L;
        String textContent = "직접 입력한 회의록입니다.";
        String minutesName = "직접 입력한 회의록";
        UserProject mockUserProject = mock(UserProject.class);

        CurrentUserHolder.set(mockUser);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.of(mockUserProject));

        // when
        MinutesCreateResponse response = minutesService.saveAndUploadManualEntry(textContent, minutesName, projectId);

        // then
        assertNotNull(response);
        verify(minutesRepository).save(any(Minutes.class));
    }

    @Test
    @DisplayName("인증되지 않은 사용자 예외 발생 (수동 입력 회의록 저장)")
    void saveAndUploadManualEntry_UnauthenticatedUser_ThrowsException() {
        // given
        Long projectId = 1L;
        String textContent = "직접 입력한 회의록입니다.";
        String minutesName = "직접 입력한 회의록";
        CurrentUserHolder.clear();

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.saveAndUploadManualEntry(textContent, minutesName, projectId));
        assertEquals(USER_NOT_AUTHENTICATED, exception.getStatus());
    }

    @Test
    @DisplayName("프로젝트를 찾을 수 없을 때 예외 발생 (수동 입력 회의록 저장)")
    void saveAndUploadManualEntry_ProjectNotFound_ThrowsException() {
        // given
        Long projectId = 1L;
        String textContent = "직접 입력한 회의록입니다.";
        String minutesName = "직접 입력한 회의록";

        CurrentUserHolder.set(mockUser);
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.saveAndUploadManualEntry(textContent, minutesName, projectId));
        assertEquals(PROJECT_NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("프로젝트에 속하지 않은 사용자 예외 발생 (수동 입력 회의록 저장)")
    void saveAndUploadManualEntry_UserNotInProject_ThrowsException() {
        // given
        Long projectId = 1L;
        String textContent = "직접 입력한 회의록입니다.";
        String minutesName = "직접 입력한 회의록";

        CurrentUserHolder.set(mockUser);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.saveAndUploadManualEntry(textContent, minutesName, projectId));
        assertEquals(USER_NOT_IN_PROJECT, exception.getStatus());
    }

    @Test
    @DisplayName("회의록 정보 조회 성공 - VOICE 타입 (화자별 텍스트 추출)")
    void getMinutes_Success_VoiceTypeWithSpeakerSegments() {
        // given
        Long minutesId = 1L;
        String voiceContent = """
        A: 안녕하세요
        B: 반갑습니다
        A: 오늘 회의를 시작하겠습니다""";

        when(mockMinutes.getId()).thenReturn(minutesId);
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(mockMinutes.getClearContent()).thenReturn(voiceContent);
        when(mockMinutes.getType()).thenReturn(MinutesType.VOICE);
        when(mockMinutes.getName()).thenReturn("회의록 제목");
        when(mockMinutes.getFilePath()).thenReturn("file/path");
        when(mockMinutes.getSummary()).thenReturn("회의 요약");
        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mock(UserProject.class)));

        // when
        MinutesInfoResult result = minutesService.getMinutes(minutesId);

        // then
        assertNotNull(result);
        assertEquals(minutesId, result.getMinutesId());
        assertEquals("A: 안녕하세요\nB: 반갑습니다\nA: 오늘 회의를 시작하겠습니다", result.getContent());
    }

    @Test
    @DisplayName("회의록 정보 조회 성공 - PICTURE 타입")
    void getMinutes_Success_PictureType() {
        // given
        Long minutesId = 2L;
        String pictureContent = "사진 내용입니다.";

        when(mockMinutes.getId()).thenReturn(minutesId);
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(mockMinutes.getClearContent()).thenReturn(pictureContent);
        when(mockMinutes.getType()).thenReturn(MinutesType.PICTURE);
        when(mockMinutes.getName()).thenReturn("사진 회의록 제목");
        when(mockMinutes.getFilePath()).thenReturn("file/path");
        when(mockMinutes.getSummary()).thenReturn("사진 회의 요약");
        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mock(UserProject.class)));

        // when
        MinutesInfoResult result = minutesService.getMinutes(minutesId);

        // then
        assertNotNull(result);
        assertEquals(minutesId, result.getMinutesId());
        assertEquals("사진 내용입니다.", result.getContent());
    }


    @Test
    @DisplayName("MANUAL 타입의 회의록 정보 조회 성공")
    void getMinutes_ManualType_Success() {
        // given
        Long minutesId = 1L;
        String manualContent = "직접 작성한 회의록입니다.";

        when(mockMinutes.getId()).thenReturn(minutesId);
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(mockMinutes.getClearContent()).thenReturn(manualContent);
        when(mockMinutes.getType()).thenReturn(MinutesType.MANUAL);
        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mock(UserProject.class)));

        // when
        MinutesInfoResult result = minutesService.getMinutes(minutesId);

        // then
        assertNotNull(result);
        assertEquals(manualContent, result.getContent());
    }

    @Test
    @DisplayName("회의록 정보 조회 시 인증되지 않은 사용자 예외 발생")
    void getMinutes_UnauthenticatedUser_ThrowsException() {
        // given
        Long minutesId = 1L;
        CurrentUserHolder.clear();

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.getMinutes(minutesId));
        assertEquals(BaseExceptionResponseStatus.USER_NOT_AUTHENTICATED, exception.getStatus());
    }

    @Test
    @DisplayName("회의록 정보 조회 시 존재하지 않는 회의록 예외 발생")
    void getMinutes_NotFound_ThrowsException() {
        // given
        Long minutesId = 3L;
        when(minutesRepository.findById(minutesId)).thenReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.getMinutes(minutesId));
        assertEquals(BaseExceptionResponseStatus.MINUTES_NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("회의록 정보 조회 시 사용자가 프로젝트에 속하지 않은 경우 예외 발생")
    void getMinutes_UserNotInProject_ThrowsException() {
        // given
        Long minutesId = 1L;
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.getMinutes(minutesId));
        assertEquals(BaseExceptionResponseStatus.USER_NOT_IN_PROJECT, exception.getStatus());
    }


    @Test
    @DisplayName("회의록 리스트 조회 성공")
    void getMinutesList_Success() {
        // given
        UserProject mockUserProject = mock(UserProject.class);
        when(mockUserProject.getProject()).thenReturn(mockProject);
        when(mockUserProject.getColor()).thenReturn(ProjectColor.PROJECT_1);
        when(userProjectRepository.findAllByUser(mockUser)).thenReturn(Collections.singletonList(mockUserProject));
        when(minutesRepository.findAllByProjectId(anyLong())).thenReturn(Collections.singletonList(mockMinutes));

        // when
        List<MinutesListResult> result = minutesService.getMinutesList();

        // then
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("프로젝트별 회의록 조회 성공")
    void getProjectMinutes_Success() {
        // given
        Long projectId = 1L;
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mock(UserProject.class)));
        when(minutesRepository.findAllByProjectId(projectId)).thenReturn(Collections.singletonList(mockMinutes));

        // when
        List<MinutesListResult> result = minutesService.getProjectMinutes(projectId);

        // then
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("프로젝트에 회의록이 없을 때 빈 리스트 반환")
    void getProjectMinutes_EmptyList_ReturnsEmptyList() {
        // given
        Long projectId = 1L;
        UserProject mockUserProject = mock(UserProject.class);

        CurrentUserHolder.set(mockUser);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.of(mockUserProject));
        when(minutesRepository.findAllByProjectId(projectId)).thenReturn(Collections.emptyList());

        // when
        List<MinutesListResult> result = minutesService.getProjectMinutes(projectId);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(minutesRepository).findAllByProjectId(projectId);
        verify(projectRepository).findById(projectId);
        verify(userProjectRepository).findByUserAndProject(mockUser, mockProject);
    }

    @Test
    @DisplayName("회의록 삭제 성공")
    void deleteMinutes_Success() {
        // given
        Long minutesId = 1L;

        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(mockProject.getId()).thenReturn(1L);

        when(projectRepository.findById(mockProject.getId())).thenReturn(Optional.of(mockProject));

        UserProject leaderProject = mock(UserProject.class);
        when(leaderProject.getRole()).thenReturn(LEADER);
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(leaderProject));

        // when
        minutesService.deleteMinutes(minutesId);

        // then
        verify(minutesRepository).delete(mockMinutes);
    }

    @Test
    @DisplayName("인증되지 않은 사용자 예외 발생 (회의록 삭제)")
    void deleteMinutes_UnauthenticatedUser_ThrowsException() {
        // given
        Long minutesId = 1L;

        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(mockMinutes.getProject()).thenReturn(mockProject);

        CurrentUserHolder.clear();

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.deleteMinutes(minutesId));
        assertEquals(USER_NOT_AUTHENTICATED, exception.getStatus());
    }

    @Test
    @DisplayName("리더가 아닌 사용자 예외 발생 (회의록 삭제)")
    void deleteMinutes_NotLeaderUser_ThrowsException() {
        // given
        Long minutesId = 1L;
        UserProject nonLeaderProject = mock(UserProject.class);

        CurrentUserHolder.set(mockUser);
        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(mockProject.getId()).thenReturn(1L);
        when(projectRepository.findById(mockProject.getId())).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.of(nonLeaderProject));
        when(nonLeaderProject.getRole()).thenReturn(Role.MEMBER);
        when(nonLeaderProject.getUser()).thenReturn(mockUser);

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.deleteMinutes(minutesId));
        assertEquals(USER_NOT_LEADER, exception.getStatus());
        verify(userProjectRepository).findByUserAndProject(mockUser, mockProject);
        verify(nonLeaderProject).getRole();
    }

    @Test
    @DisplayName("인증되지 않은 사용자 예외 테스트")
    void unauthenticatedUserTest() {
        // given
        CurrentUserHolder.clear();

        // when & then
        assertThrows(BaseException.class, () -> minutesService.getMinutesList());
        assertThrows(BaseException.class, () -> minutesService.getMinutes(1L));
        assertThrows(BaseException.class, () -> minutesService.deleteMinutes(1L));
    }

    @Test
    @DisplayName("존재하지 않는 회의록 예외 테스트")
    void minutesNotFoundTest() {
        // given
        when(minutesRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThrows(BaseException.class, () -> minutesService.getMinutes(1L));
        assertThrows(BaseException.class, () -> minutesService.deleteMinutes(1L));
    }

    @Test
    @DisplayName("권한 없는 사용자 예외 테스트")
    void unauthorizedUserTest() {
        // given
        when(minutesRepository.findById(anyLong())).thenReturn(Optional.of(mockMinutes));
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.empty());

        // when & then
        assertThrows(BaseException.class, () -> minutesService.getMinutes(1L));
        assertThrows(BaseException.class, () -> minutesService.deleteMinutes(1L));
    }

}