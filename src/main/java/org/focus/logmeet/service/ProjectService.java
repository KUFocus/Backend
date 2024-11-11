package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.project.*;
import org.focus.logmeet.domain.Project;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.domain.UserProject;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.repository.ProjectRepository;
import org.focus.logmeet.repository.UserProjectRepository;
import org.focus.logmeet.security.annotation.CurrentUser;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.domain.enums.Role.LEADER;
import static org.focus.logmeet.domain.enums.Role.MEMBER;
import static org.focus.logmeet.domain.enums.Status.ACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserProjectRepository userProjectRepository;

    @Transactional
    @CurrentUser
    public ProjectCreateResponse createProject(ProjectCreateRequest request) {
        log.info("프로젝트 생성 시도: projectName={}, projectContent={}", request.getName(), request.getContent());
        User currentUser = CurrentUserHolder.get();

        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        Project project = Project.builder()
                .name(request.getName())
                .content(request.getContent())
                .status(ACTIVE)
                .build();

        projectRepository.save(project);

        UserProject userProject = UserProject.builder()
                .user(currentUser)
                .project(project)
                .role(LEADER)
                .color(ProjectColor.valueOf(request.getColor().name()))
                .build();
        userProjectRepository.save(userProject);

        log.info("프로젝트 생성 성공: projectId={}", project.getId());

        return new ProjectCreateResponse(project.getId());
    }

    @Transactional
    @CurrentUser
    public ProjectInfoResult getProject(Long projectId) {
        log.info("프로젝트 정보 조회: projectId={}", projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        List<UserProjectDto> userProjectDtos = project.getUserProjects().stream()
                .map(up -> new UserProjectDto(
                        up.getId(),
                        up.getUser().getId(),
                        up.getUser().getName(),
                        up.getRole(),
                        up.getBookmark(),
                        up.getColor()))
                .collect(Collectors.toList());

        return new ProjectInfoResult(project.getId(), project.getName(), project.getContent(), project.getCreatedAt(), userProjectDtos);
    }

    @Transactional
    @CurrentUser
    public List<ProjectListResult> getProjectList() {
        User currentUser = CurrentUserHolder.get();

        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        log.info("프로젝트 리스트 조회: userId={}", currentUser.getId());

        List<UserProject> userProject = userProjectRepository.findAllByUser(currentUser);

        return userProject.stream().map(up -> {
            Project project = up.getProject();
            return new ProjectListResult(
                    project.getId(),
                    project.getName(),
                    up.getRole(),
                    up.getBookmark(),
                    up.getColor(),
                    project.getUserProjects().size(),
                    project.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }

    @Transactional
    @CurrentUser
    public List<ProjectListResult> getProjectBookmarkList() {
        User currentUser = CurrentUserHolder.get();

        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        log.info("프로젝트 북마크 리스트 조회: userId={}", currentUser.getId());

        List<UserProject> userProject = userProjectRepository.findAllByUser(currentUser);

        // 북마크가 true인 경우에만 필터링하여 반환
        return userProject.stream()
                .filter(UserProject::getBookmark) // 북마크가 true인 것만 필터링
                .map(up -> {
                    Project project = up.getProject();
                    return new ProjectListResult(
                            project.getId(),
                            project.getName(),
                            up.getRole(),
                            true,
                            up.getColor(),
                            project.getUserProjects().size(),
                            project.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @CurrentUser
    public void updateProject(Long projectId, String name, String content, ProjectColor color) {
        log.info("프로젝트 수정 시도: projectId={}, projectName={}", projectId, name);
        UserProject userProject = validateUserAndProject(projectId);

        if (!userProject.getRole().equals(LEADER)) {
            throw new BaseException(USER_NOT_LEADER);
        }

        Project project = userProject.getProject();
        project.setName(name);
        project.setContent(content);
        userProject.setColor(color);

        projectRepository.save(project);
        userProjectRepository.save(userProject);
        log.info("프로젝트 수정 성공: projectId={}", projectId);
    }

    @Transactional
    @CurrentUser
    public ProjectBookmarkResult bookmarkProjectToggle(Long projectId) {
        log.info("프로젝트 즐겨찾기 추가/해제 시도: projectId={}", projectId);
        UserProject userProject = validateUserAndProject(projectId);

        if (userProject.getBookmark() == Boolean.TRUE) {
            userProject.setBookmark(Boolean.FALSE);
        } else {
            userProject.setBookmark(Boolean.TRUE);
        }

        userProjectRepository.save(userProject);
        log.info("프로젝트 즐겨찾기 추가/해제 성공: projectId={}, userId={}, bookmark={}", projectId, userProject.getUser().getId(), userProject.getBookmark());
        return new ProjectBookmarkResult(userProject.getBookmark());
    }

    @Transactional
    @CurrentUser
    public void expelMember(Long projectId, Long userId) {
        log.info("참가자 추방 시도: projectId={}, userId={}", projectId, userId);
        UserProject leaderProject = validateUserAndProject(projectId);

        if (!leaderProject.getRole().equals(LEADER)) {
            throw new BaseException(USER_NOT_LEADER);
        }

        User currentUser = CurrentUserHolder.get();
        if (currentUser.getId().equals(userId)) {
            log.error("자기 자신을 추방하려고 함: userId={}", userId);
            throw new BaseException(CANNOT_EXPEL_SELF);
        }

        UserProject memberToExpel = userProjectRepository.findByUserIdAndProject(userId, leaderProject.getProject())
                .orElseThrow(() -> new BaseException(USER_NOT_IN_PROJECT));

        userProjectRepository.delete(memberToExpel);

        log.info("참가자 추방 성공: projectId={}, userId={}", projectId, userId);
    }


    @Transactional
    @CurrentUser
    public void deleteProject(Long projectId) {
        log.info("프로젝트 삭제 시도: projectId={}", projectId);
        UserProject leaderProject = validateUserAndProject(projectId);

        if (!leaderProject.getRole().equals(LEADER)) {
            log.info("권한이 없는 삭제 시도: projectId={}, userId={}", projectId, leaderProject.getUser().getId());
            throw new BaseException(USER_NOT_LEADER);
        }

        projectRepository.delete(leaderProject.getProject());
        log.info("프로젝트 삭제 성공: projectId={}", projectId);
    }

    @Transactional
    @CurrentUser
    public ProjectLeaderDelegationResponse delegateLeader(Long projectId, Long newLeaderId) {
        log.info("프로젝트 리더 위임 시도: projectId={}", projectId);
        UserProject leaderProject = validateUserAndProject(projectId);
        UserProject newLeaderProject = userProjectRepository.findByUserIdAndProject(newLeaderId, leaderProject.getProject())
                .orElseThrow(() -> new BaseException(USER_NOT_IN_PROJECT));

        if (!leaderProject.getRole().equals(LEADER)) {
            log.info("권한이 없는 리더 위임 시도: projectId={}, userId={}", projectId, leaderProject.getUser().getId());
            throw new BaseException(USER_NOT_LEADER);
        }

        if (Objects.equals(leaderProject.getUser().getId(), newLeaderId)) {
            log.info("자기 자신에게 리더 위임 시도: projectId={}, newLeaderId={}", projectId, newLeaderId);
            throw new BaseException(CANNOT_DELEGATE_SELF);
        }

        leaderProject.setRole(MEMBER);
        newLeaderProject.setRole(LEADER);
        userProjectRepository.save(leaderProject);
        userProjectRepository.save(newLeaderProject);
        log.info("프로젝트 리더 위임 성공: projectId={}, newLeaderId={}", projectId, newLeaderId);

        return new ProjectLeaderDelegationResponse(projectId, newLeaderId);
    }

    @Transactional
    @CurrentUser
    public void leaveProject(Long projectId) {
        log.info("프로젝트 나가기 시도: projectId={}", projectId);
        UserProject userProject = validateUserAndProject(projectId);

        if (userProject.getRole().equals(LEADER)) {
            log.info("프로젝트 리더의 나가기 시도: projectId={}, userId={}", projectId, userProject.getUser().getId());
            throw new BaseException(USER_IS_LEADER);
        }

        userProjectRepository.delete(userProject);
        log.info("프로젝트 나가기 성공: projectId={}, userId={}", projectId, userProject.getUser().getId());
    }

    private UserProject validateUserAndProject(Long projectId) {
        User currentUser = CurrentUserHolder.get();

        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        return userProjectRepository.findByUserAndProject(currentUser, project)
                .orElseThrow(() -> new BaseException(USER_NOT_IN_PROJECT));
    }
}
