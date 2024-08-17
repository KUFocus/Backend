package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.controller.dto.Project.ProjectCreateRequest;
import org.focus.logmeet.controller.dto.Project.ProjectCreateResponse;
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

import static org.focus.logmeet.domain.enums.Role.LEADER;
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

        return new ProjectCreateResponse(project.getId());
    }
}
