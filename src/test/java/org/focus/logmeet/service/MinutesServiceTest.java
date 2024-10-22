package org.focus.logmeet.service;

import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.minutes.*;
import org.focus.logmeet.domain.*;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.repository.MinutesRepository;
import org.focus.logmeet.repository.ProjectRepository;
import org.focus.logmeet.repository.UserProjectRepository;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.focus.logmeet.domain.enums.Role.LEADER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinutesServiceTest {

    @Mock
    private MinutesRepository minutesRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserProjectRepository userProjectRepository;

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