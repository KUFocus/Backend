package org.focus.logmeet.service;

import org.focus.logmeet.common.exception.BaseException;
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Base64;
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

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        mockProject = mock(Project.class);
        mockMinutes = mock(Minutes.class);
        CurrentUserHolder.set(mockUser);
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
    @DisplayName("음성 파일 업로드 후 임시 회의록 생성 성공")
    void uploadFile_Voice_Success() {
        // given
        String base64FileData = "base64Audio";
        String fileName = "audio.mp3";
        MinutesType fileType = MinutesType.VOICE;

        MinutesService spyMinutesService = spy(minutesService);

        // Base64 파일 디코딩 모킹
        File tempFile = mock(File.class);
        doReturn(tempFile).when(spyMinutesService).decodeBase64ToFile(anyString(), anyString());

        // S3 업로드 모킹
        doNothing().when(s3Service).uploadFile(anyString(), anyString(), any(File.class), anyString());

        // URL 생성 모킹
        doReturn("https://example.com/audio.mp3").when(spyMinutesService).generateFileUrl(anyString(), anyString());

        doReturn("테스트를 위한 회의 내용입니다.").when(spyMinutesService).processFileToText(any(File.class), anyString(), anyString());

        when(minutesRepository.save(any(Minutes.class))).thenAnswer(invocation -> {
            Minutes minutes = invocation.getArgument(0);
            minutes.setId(1L);
            return minutes;
        });

        // when
        MinutesFileUploadResponse response = spyMinutesService.uploadFile(base64FileData, fileName, fileType);

        // then
        assertNotNull(response);
        assertEquals(MinutesType.VOICE, response.getFileType());
        verify(minutesRepository).save(any(Minutes.class));
        verify(s3Service).uploadFile(eq("minutes_voice"), eq(fileName), any(File.class), eq("audio/mpeg"));
    }

    @Test
    @DisplayName("사진 파일 업로드 후 임시 회의록 생성 성공")
    void uploadFile_Picture_Success() {
        // given
        String base64FileData = "base64Image";
        String fileName = "image.jpg";
        MinutesType fileType = MinutesType.PICTURE;

        MinutesService spyMinutesService = spy(minutesService);

        // Base64 파일 디코딩 모킹
        File tempFile = mock(File.class);
        doReturn(tempFile).when(spyMinutesService).decodeBase64ToFile(anyString(), anyString());

        // S3 업로드 모킹
        doNothing().when(s3Service).uploadFile(anyString(), anyString(), any(File.class), anyString());

        // URL 생성 모킹
        doReturn("https://example.com/image.jpg").when(spyMinutesService).generateFileUrl(anyString(), anyString());

        doReturn("테스트를 위한 회의 내용입니다.").when(spyMinutesService).processFileToText(any(File.class), anyString(), anyString());

        when(minutesRepository.save(any(Minutes.class))).thenAnswer(invocation -> {
            Minutes minutes = invocation.getArgument(0);
            minutes.setId(2L);
            return minutes;
        });

        // when
        MinutesFileUploadResponse response = spyMinutesService.uploadFile(base64FileData, fileName, fileType);

        // then
        assertNotNull(response);
        assertEquals(MinutesType.PICTURE, response.getFileType());
        verify(minutesRepository).save(any(Minutes.class));
        verify(s3Service).uploadFile(eq("minutes_photo"), eq(fileName), any(File.class), eq("image/jpeg"));
    }


    @Test
    @DisplayName("잘못된 파일 타입 업로드 시 예외 발생")
    void uploadFile_InvalidFileType_ThrowsException() {
        // given
        String validBase64Data = Base64.getEncoder().encodeToString("valid".getBytes());
        String fileName = "valid.txt";
        MinutesType fileType = MinutesType.MANUAL; //MANUAL은 파일이 아니거덩요

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.uploadFile(validBase64Data, fileName, fileType));
        assertEquals(MINUTES_TYPE_NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("파일 텍스트 변환 성공")
    void processFileToText_Success() {
        // given
        File tempFile = mock(File.class);
        String fileName = "audio.mp3";
        String flaskUrl = "http://localhost:5000/process_audio";
        String expectedResponse = "테스트를 위한 회의 내용입니다.";

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        // flask url 설정을 해줘야함
        ReflectionTestUtils.setField(minutesService, "flaskServerUrl", flaskUrl);

        // when
        String result = minutesService.processFileToText(tempFile, fileName, flaskUrl);

        // then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(restTemplate).postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("파일 텍스트 변환 시 예외 발생")
    void processFileToText_Exception() {
        // given
        File tempFile = mock(File.class);
        String fileName = "audio.mp3";
        String flaskUrl = "http://localhost:5000/process_audio";

        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.processFileToText(tempFile, fileName, flaskUrl));
        assertEquals(MINUTES_TEXT_FILE_UPLOAD_ERROR, exception.getStatus());
    }

    @Test
    @DisplayName("음성 파일 업로드 중 예외 발생 시 처리")
    void uploadToS3AndProcessVoice_Exception() {
        // given
        File tempFile = mock(File.class);
        String fileName = "audio.mp3";
        Minutes minutes = new Minutes();

        // S3 업로드 중 예외 발생
        doThrow(new RuntimeException()).when(s3Service).uploadFile(anyString(), anyString(), any(File.class), anyString());

        // when & then
        assertThrows(BaseException.class, () -> minutesService.uploadToS3AndProcessVoice(tempFile, fileName, minutes));
    }

    @Test
    @DisplayName("사진 파일 업로드 중 예외 발생 시 처리")
    void uploadToS3AndProcessPicture_Exception() {
        // given
        File tempFile = mock(File.class);
        String fileName = "image.jpg";
        Minutes minutes = new Minutes();

        // S3 업로드 중 예외 발생
        doThrow(new RuntimeException()).when(s3Service).uploadFile(anyString(), anyString(), any(File.class), anyString());

        // when & then
        assertThrows(BaseException.class, () -> minutesService.uploadToS3AndProcessPicture(tempFile, fileName, minutes));
    }

    @Test
    @DisplayName("텍스트 요약 및 스케줄 저장 성공")
    void summarizeText_Success() {
        // given
        Long minutesId = 1L;
        String extractedText = "테스트를 위한 회의 내용입니다.";
        Minutes mockMinutes = mock(Minutes.class);
        Project mockProject = mock(Project.class);
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
        Minutes mockMinutes = mock(Minutes.class);
        Project mockProject = mock(Project.class);

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
        Minutes mockMinutes = mock(Minutes.class);
        Project mockProject = mock(Project.class);
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
        Minutes mockMinutes = mock(Minutes.class);
        Project mockProject = mock(Project.class);
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
        Minutes mockMinutes = mock(Minutes.class);
        Project mockProject = mock(Project.class);
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
        Minutes mockMinutes = mock(Minutes.class);
        Project mockProject = mock(Project.class);
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
        User mockUser = mock(User.class);
        Minutes mockMinutes = mock(Minutes.class);
        Project mockProject = mock(Project.class);
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
        User mockUser = mock(User.class);

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
        User mockUser = mock(User.class);
        Minutes mockMinutes = mock(Minutes.class);

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
        User mockUser = mock(User.class);
        Minutes mockMinutes = mock(Minutes.class);
        Project mockProject = mock(Project.class);

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
        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);
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
        User mockUser = mock(User.class);

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
        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);

        CurrentUserHolder.set(mockUser);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.saveAndUploadManualEntry(textContent, minutesName, projectId));
        assertEquals(USER_NOT_IN_PROJECT, exception.getStatus());
    }

    @Test
    @DisplayName("회의록 정보 조회 성공")
    void getMinutes_Success() {
        // given
        Long minutesId = 1L;
        Minutes mockMinutes = mock(Minutes.class);
        Project mockProject = mock(Project.class);

        when(mockMinutes.getId()).thenReturn(minutesId);
        when(minutesRepository.findById(minutesId)).thenReturn(Optional.of(mockMinutes));
        when(mockMinutes.getProject()).thenReturn(mockProject);
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mock(UserProject.class)));

        // when
        MinutesInfoResult result = minutesService.getMinutes(minutesId);

        // then
        assertNotNull(result);
        assertEquals(minutesId, result.getMinutesId());
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
        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);
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
        Project mockProject = mock(Project.class);
        Minutes mockMinutes = mock(Minutes.class);

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
        Minutes mockMinutes = mock(Minutes.class);
        Project mockProject = mock(Project.class);

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
        User mockUser = mock(User.class);
        Minutes mockMinutes = mock(Minutes.class);
        Project mockProject = mock(Project.class);
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
    @DisplayName("파일 URL 생성 테스트")
    void generateFileUrl_Success() {
        // given
        String directory = "minutes_voice";
        String fileName = "sample file.mp3";

        // Expected encoded URL
        String expectedUrl = "https://kr.object.ncloudstorage.com/logmeet/minutes_voice/sample+file.mp3";

        // when
        String resultUrl = minutesService.generateFileUrl(directory, fileName);

        // then
        assertNotNull(resultUrl);
        assertEquals(expectedUrl, resultUrl);
    }

    @Test
    @DisplayName("Base64 디코딩 실패 시 예외 발생")
    void decodeBase64ToFile_Base64DecodingError_ThrowsException() {
        // given
        String invalidBase64Data = "invalid base64";
        String fileName = "testfile.txt";

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.decodeBase64ToFile(invalidBase64Data, fileName));
        assertEquals(MINUTES_INVALID_BASE64_DATA, exception.getStatus());
    }

    @Test
    @DisplayName("파일 디코딩 중 IOException 발생 시 예외 발생")
    void decodeBase64ToFile_FileDecodingError_ThrowsException() {
        // given
        String validBase64Data = Base64.getEncoder().encodeToString("valid data".getBytes());
        String fileName = "invalid/testfile.txt";

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> minutesService.decodeBase64ToFile(validBase64Data, fileName));
        assertEquals(S3_FILE_DECODING_ERROR, exception.getStatus());
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