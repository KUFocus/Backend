package org.focus.logmeet.service;

import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.schedule.*;
import org.focus.logmeet.domain.*;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.domain.enums.Role;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.domain.enums.ProjectColor.*;
import static org.focus.logmeet.domain.enums.Role.LEADER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserProjectRepository userProjectRepository;
    @InjectMocks
    private ScheduleService scheduleService;

    private User mockUser;
    private Project mockProject;
    private UserProject mockUserProject;
    private Schedule mockSchedule;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        mockProject = mock(Project.class);
        mockUserProject = mock(UserProject.class);
        mockSchedule = mock(Schedule.class);
        CurrentUserHolder.set(mockUser);
    }

    @Test
    @DisplayName("스케줄 생성 성공")
    void createSchedule_Success() {
        // given
        ScheduleCreateRequest request = new ScheduleCreateRequest("Meeting", LocalDateTime.now(), 1L);
        when(projectRepository.findById(request.getProjectId())).thenReturn(Optional.of(mockProject));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(mockSchedule);

        // when
        ScheduleCreateResponse response = scheduleService.createSchedule(request);

        // then
        assertNotNull(response);
        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    @DisplayName("스케줄 수정 성공")
    void updateSchedule_Success() {
        // given
        Long scheduleId = 1L;
        Long projectId = 1L;
        ScheduleUpdateRequest request = new ScheduleUpdateRequest("Updated Meeting", LocalDateTime.now());

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(mockSchedule.getProject()).thenReturn(mockProject);
        when(mockProject.getId()).thenReturn(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mockUserProject));
        when(mockUserProject.getRole()).thenReturn(LEADER);

        // when
        scheduleService.updateSchedule(scheduleId, request);

        // then
        verify(scheduleRepository).save(mockSchedule);
        verify(mockSchedule).setContent(request.getScheduleContent());
        verify(mockSchedule).setScheduleDate(request.getScheduleDate());
    }

    @Test
    @DisplayName("프로젝트의 월별 스케줄 조회 성공")
    void getScheduleOfProject_Success() {
        // given
        Long projectId = 1L;
        LocalDate yearMonth = LocalDate.of(2024, 10, 1);
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject)); // 프로젝트 모킹 추가
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mockUserProject));
        when(scheduleRepository.findSchedulesByProjectIdAndMonth(projectId, year, month)).thenReturn(List.of(mockSchedule));
        when(mockSchedule.getScheduleDate()).thenReturn(LocalDateTime.of(2024, 10, 5, 10, 0));
        when(mockUserProject.getColor()).thenReturn(PROJECT_1);

        // when
        List<ScheduleMonthlyListResult> result = scheduleService.getScheduleOfProject(projectId, yearMonth);

        // then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getDate());
        assertTrue(result.get(0).getColors().contains(PROJECT_1));
    }

    @Test
    @DisplayName("프로젝트의 월별 스케줄 조회 시 프로젝트에 속하지 않은 경우 예외 발생")
    void getScheduleOfProject_UserNotInProject_ThrowsException() {
        // given
        Long projectId = 1L;
        LocalDate yearMonth = LocalDate.of(2024, 10, 1);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.empty()); // 유저가 프로젝트에 속하지 않음

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> scheduleService.getScheduleOfProject(projectId, yearMonth));
        assertEquals(USER_NOT_IN_PROJECT, exception.getStatus());
    }

    @Test
    @DisplayName("프로젝트 유효성 검증 시 인증되지 않은 사용자 예외 발생")
    void validateUserAndProject_UnauthenticatedUser_ThrowsException() {
        // given
        Long projectId = 1L;
        CurrentUserHolder.clear(); // 현재 유저를 제거하여 인증되지 않은 상태로 만듦

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> scheduleService.getScheduleOfProject(projectId, LocalDate.now()));
        assertEquals(USER_NOT_AUTHENTICATED, exception.getStatus());
    }


    @Test
    @DisplayName("스케줄 조회 성공")
    void getSchedule_Success() {
        // given
        Long scheduleId = 1L;
        Long projectId = 1L;
        LocalDateTime scheduleDate = LocalDateTime.now();

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(mockSchedule.getProject()).thenReturn(mockProject);
        when(mockProject.getId()).thenReturn(projectId);
        when(mockProject.getName()).thenReturn("Project Name");
        when(mockSchedule.getContent()).thenReturn("Meeting");
        when(mockSchedule.getScheduleDate()).thenReturn(scheduleDate);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mockUserProject));
        when(mockUserProject.getColor()).thenReturn(ProjectColor.PROJECT_1);

        // when
        ScheduleInfoResult result = scheduleService.getSchedule(scheduleId);

        // then
        assertNotNull(result);
        assertEquals("Meeting", result.getScheduleContent());
        assertEquals("Project Name", result.getProjectName());
        assertEquals(scheduleDate, result.getScheduleDate());
    }

    @Test
    @DisplayName("유저의 월별 스케줄 조회 성공")
    void getScheduleOfUser_Success() {
        // given
        User mockUser = mock(User.class);
        LocalDate yearMonth = LocalDate.of(2024, 10, 1);
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();

        CurrentUserHolder.set(mockUser); // 현재 유저 설정
        when(mockUser.getId()).thenReturn(1L);
        when(scheduleRepository.findSchedulesByUserIdAndMonth(mockUser.getId(), year, month)).thenReturn(List.of(mockSchedule));
        when(mockSchedule.getScheduleDate()).thenReturn(LocalDateTime.of(2024, 10, 5, 10, 0));
        when(mockSchedule.getProject()).thenReturn(mockProject);
        when(mockProject.getUserProjects()).thenReturn(List.of(mockUserProject));
        when(mockUserProject.getUser()).thenReturn(mockUser);
        when(mockUserProject.getColor()).thenReturn(PROJECT_1);

        // when
        List<ScheduleMonthlyListResult> result = scheduleService.getScheduleOfUser(yearMonth);

        // then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getDate());
        assertTrue(result.get(0).getColors().contains(PROJECT_1));
    }

    @Test
    @DisplayName("유저의 월별 스케줄 조회 시 인증되지 않은 사용자 예외 발생")
    void getScheduleOfUser_UnauthenticatedUser_ThrowsException() {
        // given
        LocalDate yearMonth = LocalDate.of(2024, 10, 1);
        CurrentUserHolder.clear(); // 현재 유저를 제거하여 인증되지 않은 상태로 만듦

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> scheduleService.getScheduleOfUser(yearMonth));
        assertEquals(USER_NOT_AUTHENTICATED, exception.getStatus());
    }

    @Test
    @DisplayName("스케줄 삭제 성공")
    void deleteSchedule_Success() {
        // given
        Long scheduleId = 1L;
        Long projectId = 1L;
        User mockUser = mock(User.class);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(mockSchedule.getProject()).thenReturn(mockProject);
        when(mockProject.getId()).thenReturn(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mockUserProject));
        when(mockUserProject.getRole()).thenReturn(LEADER);

        // when
        scheduleService.deleteSchedule(scheduleId);

        // then
        verify(scheduleRepository).delete(mockSchedule);
    }

    @Test
    @DisplayName("스케줄 생성 시 프로젝트를 찾을 수 없을 때 예외 발생")
    void createSchedule_ProjectNotFound_ThrowsException() {
        // given
        ScheduleCreateRequest request = new ScheduleCreateRequest("Meeting", LocalDateTime.now(), 1L);
        when(projectRepository.findById(request.getProjectId())).thenReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> scheduleService.createSchedule(request));
        assertEquals(PROJECT_NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("스케줄 수정 시 유저가 리더가 아닌 경우 예외 발생")
    void updateSchedule_UserNotLeader_ThrowsException() {
        // given
        Long scheduleId = 1L;
        Long projectId = 1L;
        ScheduleUpdateRequest request = new ScheduleUpdateRequest("Updated Meeting", LocalDateTime.now());
        User mockUser = mock(User.class);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(mockSchedule.getProject()).thenReturn(mockProject);
        when(mockProject.getId()).thenReturn(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mockUserProject));
        when(mockUserProject.getRole()).thenReturn(Role.MEMBER);
        when(mockUserProject.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn(1L);

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> scheduleService.updateSchedule(scheduleId, request));
        assertEquals(USER_NOT_LEADER, exception.getStatus());
    }

    @Test
    @DisplayName("스케줄 조회 시 프로젝트에 속하지 않은 경우 예외 발생")
    void getSchedule_UserNotInProject_ThrowsException() {
        // given
        Long scheduleId = 1L;
        Long projectId = 1L;
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(mockSchedule.getProject()).thenReturn(mockProject);
        when(mockProject.getId()).thenReturn(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> scheduleService.getSchedule(scheduleId));
        assertEquals(USER_NOT_IN_PROJECT, exception.getStatus());
    }

    @Test
    @DisplayName("스케줄 삭제 시 스케줄을 찾을 수 없을 때 예외 발생")
    void deleteSchedule_ScheduleNotFound_ThrowsException() {
        // given
        Long scheduleId = 1L;
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> scheduleService.deleteSchedule(scheduleId));
        assertEquals(SCHEDULE_NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("스케줄 삭제 시 유저가 리더가 아닌 경우 예외 발생")
    void deleteSchedule_UserNotLeader_ThrowsException() {
        // given
        Long scheduleId = 1L;
        Long projectId = 1L;
        User mockUser = mock(User.class);

        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(mockSchedule));
        when(mockSchedule.getProject()).thenReturn(mockProject);
        when(mockProject.getId()).thenReturn(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mockUserProject));
        when(mockUserProject.getRole()).thenReturn(Role.MEMBER); // 리더가 아님
        when(mockUserProject.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn(1L);

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> scheduleService.deleteSchedule(scheduleId));
        assertEquals(USER_NOT_LEADER, exception.getStatus());
    }

    @Test
    @DisplayName("프로젝트의 특정 날짜 스케줄 조회 성공")
    void getScheduleOfProjectAt_Success() {
        // given
        Long projectId = 1L;
        LocalDate date = LocalDate.now();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(any(), any())).thenReturn(Optional.of(mockUserProject));
        when(scheduleRepository.findByProjectIdAndDate(projectId, date)).thenReturn(List.of(mockSchedule));
        when(mockSchedule.getId()).thenReturn(1L);
        when(mockSchedule.getContent()).thenReturn("Meeting");
        when(mockSchedule.getScheduleDate()).thenReturn(LocalDateTime.now());
        when(mockSchedule.getProject()).thenReturn(mockProject);
        when(mockProject.getName()).thenReturn("Project Name");
        when(mockUserProject.getColor()).thenReturn(PROJECT_1);

        // when
        List<ScheduleListResult> result = scheduleService.getScheduleOfProjectAt(projectId, date);

        // then
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Meeting", result.get(0).getScheduleContent());
        assertEquals("Project Name", result.get(0).getProjectName());
    }

    @Test
    @DisplayName("유저의 특정 날짜 스케줄 조회 성공")
    void getScheduleOfUserAt_Success() {
        // given
        LocalDate date = LocalDate.now();
        when(scheduleRepository.findByUserIdAndDate(anyLong(), any(LocalDate.class))).thenReturn(List.of(mockSchedule));
        when(mockSchedule.getProject()).thenReturn(mockProject);
        when(mockProject.getUserProjects()).thenReturn(List.of(mockUserProject));
        when(mockUserProject.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn(1L);

        // when
        List<ScheduleListResult> result = scheduleService.getScheduleOfUserAt(date);

        // then
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("유저의 특정 날짜 스케줄 조회 시 인증되지 않은 사용자 예외 발생")
    void getScheduleOfUserAt_UnauthenticatedUser_ThrowsException() {
        // given
        LocalDate date = LocalDate.now();
        CurrentUserHolder.clear(); // 현재 유저를 제거하여 인증되지 않은 상태로 만듦

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> scheduleService.getScheduleOfUserAt(date));
        assertEquals(USER_NOT_AUTHENTICATED, exception.getStatus());
    }

}
