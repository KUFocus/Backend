package org.focus.logmeet.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.Project.ProjectCreateRequest;
import org.focus.logmeet.controller.dto.Project.ProjectCreateResponse;
import org.focus.logmeet.service.ProjectService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.focus.logmeet.common.utils.ValidationUtils.validateBindingResult;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/project")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/new")
    public BaseResponse<ProjectCreateResponse> createProject(@Validated @RequestBody ProjectCreateRequest request, BindingResult bindingResult) {
        log.info("프로젝트 생성 요청: {}", request.getName());
        validateBindingResult(bindingResult);

        ProjectCreateResponse response = projectService.createProject(request);
        return new BaseResponse<>(response);
    }

    @PostMapping("/test")
    public Boolean testProject(String message) {
        log.info("테스트 요청: {}", message);
        return true;
    }
}
