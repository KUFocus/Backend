package org.focus.logmeet.service;

import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.project.*;
import org.focus.logmeet.domain.*;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.domain.enums.Role;
import org.focus.logmeet.domain.enums.Status;
import org.focus.logmeet.repository.InviteCodeRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.domain.enums.Role.LEADER;
import static org.focus.logmeet.domain.enums.Role.MEMBER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserProjectRepository userProjectRepository;

    @Mock
    private InviteCodeRepository inviteCodeRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project project;
    @BeforeEach
    void setUp() {
        project = Project.builder()
                .name("테스트 프로젝트")
                .content("테스트 프로젝트입니다.")
                .build();
    }

    @Test
    @DisplayName("inviteCodes 필드 초기화 테스트")
    void inviteCodesInitializationTest() {
        // given
        InviteCode inviteCode = new InviteCode();
        project.setInviteCodes(List.of(inviteCode));

        // when
        List<InviteCode> inviteCodes = project.getInviteCodes();

        // then
        assertNotNull(inviteCodes);
        assertEquals(1, inviteCodes.size());
        assertEquals(inviteCode, inviteCodes.get(0));
    }
    @Test
    @DisplayName("InviteCode의 id 필드 값 설정 및 검증")
    void inviteCodeIdFieldTest() {
        // given
        InviteCode inviteCode = new InviteCode();
        inviteCode.setId(1L);

        // when
        Long id = inviteCode.getId();

        // then
        assertNotNull(id);
        assertEquals(1L, id);
    }

    @Test
    @DisplayName("InviteCode의 project 필드 설정 및 검증")
    void inviteCodeProjectFieldTest() {
        // given
        InviteCode inviteCode = new InviteCode();
        inviteCode.setProject(project);

        // when
        Project associatedProject = inviteCode.getProject();

        // then
        assertNotNull(associatedProject);
        assertEquals(project, associatedProject);
    }

    @Test
    @DisplayName("userProjects 필드 초기화 테스트")
    void userProjectsInitializationTest() {
        // given
        UserProject userProject = UserProject.builder()
                .role(Role.MEMBER)
                .project(project)
                .build();
        project.setUserProjects(List.of(userProject));

        // when
        List<UserProject> userProjects = project.getUserProjects();

        // then
        assertNotNull(userProjects);
        assertEquals(1, userProjects.size());
        assertEquals(userProject, userProjects.get(0));
    }

    @Test
    @DisplayName("schedules 필드 초기화 테스트")
    void schedulesInitializationTest() {
        // given
        Schedule schedule = new Schedule();
        project.setSchedules(List.of(schedule));

        // when
        List<Schedule> schedules = project.getSchedules();

        // then
        assertNotNull(schedules);
        assertEquals(1, schedules.size());
        assertEquals(schedule, schedules.get(0));
    }

    @Test
    @DisplayName("minutes 필드 초기화 테스트")
    void minutesInitializationTest() {
        // given
        Minutes minutes = new Minutes();
        project.setMinutes(List.of(minutes));

        // when
        List<Minutes> minutesList = project.getMinutes();

        // then
        assertNotNull(minutesList);
        assertEquals(1, minutesList.size());
        assertEquals(minutes, minutesList.get(0));
    }

    @Test
    @DisplayName("name 필드 값 설정 및 검증")
    void nameFieldTest() {
        // when
        String name = project.getName();

        // then
        assertNotNull(name);
        assertEquals("테스트 프로젝트", name);
    }

    @Test
    @DisplayName("content 필드 값 설정 및 검증")
    void contentFieldTest() {
        // when
        String content = project.getContent();

        // then
        assertNotNull(content);
        assertEquals("테스트 프로젝트입니다.", content);
    }

    @Test
    @DisplayName("status 필드 기본값 검증")
    void statusFieldDefaultTest() {
        // when
        Status status = project.getStatus();

        // then
        assertNotNull(status);
        assertEquals(Status.ACTIVE, status);
    }

    @Test
    @DisplayName("프로젝트 생성 성공")
    void createProject_Success() {
        // given
        ProjectCreateRequest request = new ProjectCreateRequest("테스트 프로젝트 제목", "테스트 프로젝트 내용", ProjectColor.PROJECT_1);
        User mockUser = mock(User.class);
        CurrentUserHolder.set(mockUser);

        //JPA라 직접 ID 생성해줘야함
        doAnswer(invocation -> {
            Project testProject = invocation.getArgument(0);
            testProject.setId(1L);
            return testProject;
        }).when(projectRepository).save(any(Project.class));

        when(userProjectRepository.save(any(UserProject.class))).thenReturn(mock(UserProject.class));

        // when
        ProjectCreateResponse response = projectService.createProject(request);

        // then
        assertNotNull(response);
        assertEquals(1L, response.getProjectId());
        verify(projectRepository, times(1)).save(any(Project.class));
        verify(userProjectRepository, times(1)).save(any(UserProject.class));
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 프로젝트 생성 시 예외 발생")
    void createProject_UserNotAuthenticated_ThrowsException() {
        //given
        ProjectCreateRequest request = new ProjectCreateRequest("프로젝트 이름", "내용", ProjectColor.PROJECT_1);
        CurrentUserHolder.clear();

        //when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.createProject(request));
        assertEquals(USER_NOT_AUTHENTICATED, exception.getStatus());
    }

    @Test
    @DisplayName("프로젝트 정보 조회 성공")
    void getProject_Success() {
        //given
        Long projectId = 1L;
        Project mockProject = mock(Project.class);
        UserProject mockUserProject = mock(UserProject.class);
        User mockUser = mock(User.class);

        //UnnecessaryStubbingException 발생 조심....
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(mockProject.getUserProjects()).thenReturn(List.of(mockUserProject));

        when(mockProject.getId()).thenReturn(projectId);
        when(mockProject.getName()).thenReturn("테스트 프로젝트 제목");
        when(mockProject.getContent()).thenReturn("테스트 프로젝트 내용");

        when(mockUserProject.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getName()).thenReturn("테스트 사용자");

        //when
        ProjectInfoResult result = projectService.getProject(projectId);

        //then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        verify(projectRepository, times(1)).findById(projectId);
    }

    @Test
    @DisplayName("존재하지 않는 프로젝트 조회 시 예외 발생")
    void getProject_ProjectNotFound_ThrowsException() {
        //given
        Long projectId = 1L;

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        //when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.getProject(projectId));
        assertEquals(PROJECT_NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("프로젝트 리스트 조회 성공")
    void getProjectList_Success() {
        //given
        User mockUser = mock(User.class);
        CurrentUserHolder.set(mockUser);

        UserProject mockUserProject = mock(UserProject.class);
        Project mockProject = mock(Project.class);

        when(mockUserProject.getProject()).thenReturn(mockProject);
        when(mockProject.getId()).thenReturn(1L);
        when(userProjectRepository.findAllByUser(mockUser)).thenReturn(List.of(mockUserProject));

        //when
        List<ProjectListResult> result = projectService.getProjectList();

        //then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userProjectRepository, times(1)).findAllByUser(mockUser);
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 프로젝트 리스트 조회 시 예외 발생")
    void getProjectList_UserNotAuthenticated_ThrowsException() {
        //given
        CurrentUserHolder.clear();

        //when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.getProjectList());
        assertEquals(USER_NOT_AUTHENTICATED, exception.getStatus());
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 북마크 리스트 조회 시 예외 발생")
    void getProjectBookmarkList_UserNotAuthenticated_ThrowsException() {
        //given
        CurrentUserHolder.clear();

        //when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.getProjectBookmarkList());
        assertEquals(USER_NOT_AUTHENTICATED, exception.getStatus());
    }
    @Test
    @DisplayName("북마크가 설정된 프로젝트 리스트 조회 성공")
    void getProjectBookmarkList_Success() {
        // given
        User mockUser = mock(User.class);
        Project mockProject1 = mock(Project.class);
        Project mockProject2 = mock(Project.class);

        UserProject userProject1 = UserProject.builder()
                .user(mockUser)
                .project(mockProject1)
                .bookmark(true)
                .build();

        UserProject userProject2 = UserProject.builder()
                .user(mockUser)
                .project(mockProject2)
                .bookmark(false)
                .build();

        List<UserProject> userProjects = List.of(userProject1, userProject2);

        when(mockUser.getId()).thenReturn(1L);
        when(userProjectRepository.findAllByUser(mockUser)).thenReturn(userProjects);

        CurrentUserHolder.set(mockUser);

        // when
        List<ProjectListResult> result = projectService.getProjectBookmarkList();

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userProjectRepository, times(1)).findAllByUser(mockUser);
    }


    @Test
    @DisplayName("프로젝트 수정 성공")
    void updateProject_Success() {
        //given
        Long projectId = 1L;
        String newName = "테스트 프로젝트 업데이트 제목";
        String newContent = "테스트 프로젝트 업데이트 내용";
        ProjectColor newColor = ProjectColor.PROJECT_1;

        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);
        UserProject mockUserProject = mock(UserProject.class);

        when(mockUserProject.getRole()).thenReturn(LEADER);
        when(mockUserProject.getProject()).thenReturn(mockProject);
        when(userProjectRepository.findByUserAndProject(any(User.class), any(Project.class)))
                .thenReturn(Optional.of(mockUserProject));
        when(projectRepository.findById(anyLong()))
                .thenReturn(Optional.ofNullable(mockProject));

        CurrentUserHolder.set(mockUser);

        //when
        projectService.updateProject(projectId, newName, newContent, newColor);

        //then
        verify(mockProject, times(1)).setName(newName);
        verify(mockProject, times(1)).setContent(newContent);
        verify(userProjectRepository, times(1)).save(mockUserProject);
    }
    @Test
    @DisplayName("리더가 아닌 사용자가 프로젝트 수정 시 예외 발생")
    void updateProject_UserNotLeader_ThrowsException() {
        //given
        Long projectId = 1L;
        String newName = "수정된 프로젝트 제목";
        String newContent = "수정된 프로젝트 내용";
        ProjectColor newColor = ProjectColor.PROJECT_1;

        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);
        UserProject mockUserProject = mock(UserProject.class);

        when(mockUserProject.getRole()).thenReturn(MEMBER);
        when(userProjectRepository.findByUserAndProject(any(User.class), any(Project.class)))
                .thenReturn(Optional.of(mockUserProject));
        when(projectRepository.findById(anyLong())).thenReturn(Optional.of(mockProject));

        CurrentUserHolder.set(mockUser);

        //when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.updateProject(projectId, newName, newContent, newColor));
        assertEquals(USER_NOT_LEADER, exception.getStatus());
    }

    @Test
    @DisplayName("프로젝트 즐겨찾기 토글 성공")
    void bookmarkProjectToggle_Success() {
        //given
        Long projectId = 1L;
        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);

        UserProject userProject = UserProject.builder()
                .user(mockUser)
                .project(mockProject)
                .bookmark(false)
                .build();

        UserProject spyUserProject = spy(userProject);

        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.of(spyUserProject));
        when(projectRepository.findById(anyLong())).thenReturn(Optional.of(mockProject));

        CurrentUserHolder.set(mockUser);

        ProjectBookmarkResult result = projectService.bookmarkProjectToggle(projectId);

        //then
        assertTrue(result.getBookmark());

        result = projectService.bookmarkProjectToggle(projectId);

        //then
        assertFalse(result.getBookmark());
    }
    @Test
    @DisplayName("리더가 아닌 사용자가 참가자 추방 시 예외 발생")
    void expelMember_UserNotLeader_ThrowsException() {
        //given
        Long projectId = 1L;
        Long userId = 2L;

        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);
        UserProject mockUserProject = mock(UserProject.class);

        when(mockUserProject.getRole()).thenReturn(MEMBER);
        when(projectRepository.findById(projectId)).thenReturn(Optional.ofNullable(mockProject));
        when(userProjectRepository.findByUserAndProject(any(User.class), any(Project.class)))
                .thenReturn(Optional.of(mockUserProject));

        CurrentUserHolder.set(mockUser);

        //when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.expelMember(projectId, userId));
        assertEquals(USER_NOT_LEADER, exception.getStatus());
    }

    @Test
    @DisplayName("자기 자신을 추방하려고 할 때 예외 발생")
    void expelMember_ExpelSelf_ThrowsException() {
        //given
        Long projectId = 1L;
        Long userId = 1L;

        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);
        UserProject mockUserProject = mock(UserProject.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockUserProject.getRole()).thenReturn(LEADER);
        when(projectRepository.findById(projectId)).thenReturn(Optional.ofNullable(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.of(mockUserProject));

        CurrentUserHolder.set(mockUser);

        //when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.expelMember(projectId, userId));
        assertEquals(CANNOT_EXPEL_SELF, exception.getStatus());
    }

    @Test
    @DisplayName("참가자 추방 성공")
    void expelMember_Success() {
        //given
        Long projectId = 1L;
        Long userIdToExpel = 2L;

        User mockLeaderUser = mock(User.class);
        User memberUser = mock(User.class);
        Project mockProject = mock(Project.class);

        UserProject leaderProject = UserProject.builder()
                .user(mockLeaderUser)
                .project(mockProject)
                .role(LEADER)
                .build();

        UserProject memberProject = UserProject.builder()
                .user(memberUser)
                .project(mockProject)
                .role(MEMBER)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockLeaderUser, mockProject))
                .thenReturn(Optional.of(leaderProject));
        when(userProjectRepository.findByUserIdAndProject(userIdToExpel, mockProject))
                .thenReturn(Optional.of(memberProject));

        CurrentUserHolder.set(mockLeaderUser);

        //when
        projectService.expelMember(projectId, userIdToExpel);

        //then
        verify(userProjectRepository, times(1)).delete(memberProject);

        verify(userProjectRepository, times(1)).findByUserIdAndProject(userIdToExpel, mockProject);
        verify(userProjectRepository, times(1)).delete(memberProject);
    }

    @Test
    @DisplayName("프로젝트 삭제 성공")
    void deleteProject_Success() {
        //given
        Long projectId = 1L;

        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);

        UserProject leaderProject = UserProject.builder()
                .user(mockUser)
                .project(mockProject)
                .role(LEADER)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject))
                .thenReturn(Optional.of(leaderProject));

        CurrentUserHolder.set(mockUser);

        //when
        projectService.deleteProject(projectId);

        //then
        verify(projectRepository, times(1)).delete(mockProject);
    }


    @Test
    @DisplayName("리더가 아닌 사용자가 프로젝트 삭제 시 예외 발생")
    void deleteProject_UserNotLeader_ThrowsException() {
        // given
        Long projectId = 1L;

        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);
        UserProject mockUserProject = mock(UserProject.class);

        when(mockUserProject.getRole()).thenReturn(MEMBER);
        when(mockUserProject.getUser()).thenReturn(mockUser);
        when(projectRepository.findById(projectId)).thenReturn(Optional.ofNullable(mockProject));
        when(userProjectRepository.findByUserAndProject(any(User.class), any(Project.class)))
                .thenReturn(Optional.of(mockUserProject));

        CurrentUserHolder.set(mockUser);

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.deleteProject(projectId));
        assertEquals(USER_NOT_LEADER, exception.getStatus());
    }

    @Test
    @DisplayName("리더가 아닌 사용자가 리더 위임 시 예외 발생")
    void delegateLeader_UserNotLeader_ThrowsException() {
        //given
        Long projectId = 1L;
        Long newLeaderId = 2L;

        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);

        UserProject mockUserProject = UserProject.builder()
                .user(mockUser)
                .project(mockProject)
                .role(MEMBER)
                .build();

        UserProject newLeaderProject = UserProject.builder()
                .user(mockUser)
                .project(mockProject)
                .role(MEMBER)
                .build();

        when(mockUser.getId()).thenReturn(1L);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject))
                .thenReturn(Optional.of(mockUserProject));
        when(userProjectRepository.findByUserIdAndProject(newLeaderId, mockProject))
                .thenReturn(Optional.of(newLeaderProject));

        // CurrentUser 설정
        CurrentUserHolder.set(mockUser);

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.delegateLeader(projectId, newLeaderId));
        assertEquals(USER_NOT_LEADER, exception.getStatus());
    }
    @Test
    @DisplayName("자기 자신에게 리더 위임 시 예외 발생")
    void delegateLeader_DelegateToSelf_ThrowsException() {
        //given
        Long projectId = 1L;
        Long newLeaderId = 1L;

        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);

        UserProject leaderProject = UserProject.builder()
                .user(mockUser)
                .project(mockProject)
                .role(LEADER)
                .build();

        UserProject newLeaderProject = UserProject.builder()
                .user(mockUser)
                .project(mockProject)
                .role(LEADER)
                .build();

        when(mockUser.getId()).thenReturn(newLeaderId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject))
                .thenReturn(Optional.of(leaderProject));

        when(userProjectRepository.findByUserIdAndProject(newLeaderId, mockProject))
                .thenReturn(Optional.of(newLeaderProject));

        CurrentUserHolder.set(mockUser);

        //when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.delegateLeader(projectId, newLeaderId));
        assertEquals(CANNOT_DELEGATE_SELF, exception.getStatus());
    }

    @Test
    @DisplayName("리더 위임 성공")
    void delegateLeader_Success() {
        // given
        Long projectId = 1L;
        Long newLeaderId = 2L;

        User mockUser = mock(User.class);
        User newLeader = mock(User.class);
        Project mockProject = mock(Project.class);

        UserProject leaderProject = UserProject.builder()
                .user(mockUser)
                .project(mockProject)
                .role(LEADER)
                .build();

        UserProject newLeaderProject = UserProject.builder()
                .user(newLeader)
                .project(mockProject)
                .role(MEMBER)
                .build();

        when(mockUser.getId()).thenReturn(1L);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject))
                .thenReturn(Optional.of(leaderProject));
        when(userProjectRepository.findByUserIdAndProject(newLeaderId, mockProject))
                .thenReturn(Optional.of(newLeaderProject));

        CurrentUserHolder.set(mockUser);

        // when
        ProjectLeaderDelegationResponse response = projectService.delegateLeader(projectId, newLeaderId);

        // then
        assertNotNull(response);
        assertEquals(newLeaderId, response.getNewLeaderId());
    }

    @Test
    @DisplayName("프로젝트 나가기 성공")
    void leaveProject_Success() {
        //given
        Long projectId = 1L;

        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);
        UserProject mockUserProject = mock(UserProject.class);

        when(mockUserProject.getRole()).thenReturn(MEMBER);
        when(mockUserProject.getUser()).thenReturn(mockUser);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject))
                .thenReturn(Optional.of(mockUserProject));

        CurrentUserHolder.set(mockUser);

        //when
        projectService.leaveProject(projectId);

        //then
        verify(userProjectRepository, times(1)).delete(mockUserProject);
    }


    @Test
    @DisplayName("리더가 프로젝트 나가기를 시도할 때 예외 발생")
    void leaveProject_LeaderLeaves_ThrowsException() {
        //given
        Long projectId = 1L;

        User mockUser = mock(User.class);
        Project mockProject = mock(Project.class);
        UserProject mockUserProject = mock(UserProject.class);

        when(mockUserProject.getRole()).thenReturn(LEADER);
        when(mockUserProject.getUser()).thenReturn(mockUser);
        when(projectRepository.findById(projectId)).thenReturn(Optional.ofNullable(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject))
                .thenReturn(Optional.of(mockUserProject));

        CurrentUserHolder.set(mockUser);

        //when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.leaveProject(projectId));
        assertEquals(USER_IS_LEADER, exception.getStatus());
    }
    @Test
    @DisplayName("인증되지 않은 사용자가 프로젝트 접근 시 예외 발생")
    void validateUserAndProject_UserNotAuthenticated_ThrowsException() {
        //given
        Long projectId = 1L;
        CurrentUserHolder.clear();

        //when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.deleteProject(projectId));
        assertEquals(USER_NOT_AUTHENTICATED, exception.getStatus());
    }
    @Test
    @DisplayName("사용자가 리더이고 기존 초대 코드가 없을 때 새로운 초대 코드 생성")
    void getInviteCode_Success_NewCodeGenerated() {
        // given
        Long projectId = 1L;
        User mockUser = mock(User.class);
        CurrentUserHolder.set(mockUser);


        Project mockProject = mock(Project.class);
        UserProject mockUserProject = mock(UserProject.class);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.of(mockUserProject));
        when(mockUserProject.getRole()).thenReturn(LEADER);

        when(inviteCodeRepository.findValidCodeByProjectId(eq(projectId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        when(inviteCodeRepository.existsByCode(anyString())).thenReturn(false);

        // when
        ProjectInviteCodeResult result = projectService.getInviteCode(projectId);

        // then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertNotNull(result.getInviteCode());
        assertNotNull(result.getExpirationDate());

        verify(inviteCodeRepository, times(1)).save(any(InviteCode.class));
    }

    @Test
    @DisplayName("사용자가 리더이고 기존 초대 코드가 있을 때 기존 코드 반환")
    void getInviteCode_Success_ExistingCodeReturned() {
        // given
        Long projectId = 1L;
        User mockUser = mock(User.class);
        CurrentUserHolder.set(mockUser);

        Project mockProject = mock(Project.class);
        UserProject mockUserProject = mock(UserProject.class);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.of(mockUserProject));
        when(mockUserProject.getRole()).thenReturn(LEADER);

        InviteCode existingInviteCode = InviteCode.builder()
                .code("EXISTINGCODE")
                .project(mockProject)
                .expirationDate(LocalDateTime.now().plusDays(7))
                .build();

        when(inviteCodeRepository.findValidCodeByProjectId(eq(projectId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(existingInviteCode));

        // when
        ProjectInviteCodeResult result = projectService.getInviteCode(projectId);

        // then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertEquals("EXISTINGCODE", result.getInviteCode());
        assertEquals(existingInviteCode.getExpirationDate(), result.getExpirationDate());

        verify(inviteCodeRepository, never()).save(any(InviteCode.class));
    }

    @Test
    @DisplayName("사용자가 리더가 아닐 때 예외 발생")
    void getInviteCode_UserNotLeader_ThrowsException() {
        // given
        Long projectId = 1L;
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        CurrentUserHolder.set(mockUser);

        Project mockProject = mock(Project.class);
        UserProject mockUserProject = mock(UserProject.class);

        // 모킹 설정 추가
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.of(mockUserProject));

        when(mockUserProject.getUser()).thenReturn(mockUser);
        when(mockUserProject.getRole()).thenReturn(MEMBER);

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.getInviteCode(projectId));
        assertEquals(USER_NOT_LEADER, exception.getStatus());

        verify(inviteCodeRepository, never()).findValidCodeByProjectId(anyLong(), any(LocalDateTime.class));
        verify(inviteCodeRepository, never()).save(any(InviteCode.class));
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 초대 코드 요청 시 예외 발생")
    void getInviteCode_UserNotAuthenticated_ThrowsException() {
        // given
        Long projectId = 1L;
        CurrentUserHolder.clear();

        // when & then
        BaseException exception = assertThrows(BaseException.class, () -> projectService.getInviteCode(projectId));
        assertEquals(USER_NOT_AUTHENTICATED, exception.getStatus());

        verify(inviteCodeRepository, never()).findValidCodeByProjectId(anyLong(), any(LocalDateTime.class));
        verify(inviteCodeRepository, never()).save(any(InviteCode.class));
    }

    @Test
    @DisplayName("초대 코드 생성 시 중복 코드가 발생하면 재시도하여 고유 코드 생성")
    void getInviteCode_CodeGenerationRetriesOnDuplicate() {
        // given
        Long projectId = 1L;
        User mockUser = mock(User.class);
        CurrentUserHolder.set(mockUser);

        Project mockProject = mock(Project.class);
        UserProject mockUserProject = mock(UserProject.class);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(mockProject));
        when(userProjectRepository.findByUserAndProject(mockUser, mockProject)).thenReturn(Optional.of(mockUserProject));
        when(mockUserProject.getRole()).thenReturn(LEADER);

        when(inviteCodeRepository.findValidCodeByProjectId(eq(projectId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        when(inviteCodeRepository.existsByCode(anyString()))
                .thenReturn(true)   // 첫 번째 시도에서 중복 코드 발생
                .thenReturn(false);  // 두 번째 시도에서 고유 코드 생성

        // when
        ProjectInviteCodeResult result = projectService.getInviteCode(projectId);

        // then
        assertNotNull(result);
        assertEquals(projectId, result.getProjectId());
        assertNotNull(result.getInviteCode());
        assertNotNull(result.getExpirationDate());

        verify(inviteCodeRepository, times(2)).existsByCode(anyString());
        verify(inviteCodeRepository, times(1)).save(any(InviteCode.class));
    }
}
