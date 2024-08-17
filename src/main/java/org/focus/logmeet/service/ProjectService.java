package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.controller.dto.Project.ProjectCreateRequest;
import org.focus.logmeet.controller.dto.Project.ProjectCreateResponse;
import org.focus.logmeet.repository.ProjectRepository;
import org.focus.logmeet.repository.UserProjectRepository;
import org.focus.logmeet.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserProjectRepository userProjectRepository;

//    @Transactional
//    public ProjectCreateResponse createProject(ProjectCreateRequest request) {
//        log.info("프로젝트 생성 시도: projectName={}, projectContent={}", request.getName(), request.getContent());
//
//
//    }
}
