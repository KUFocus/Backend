package org.focus.logmeet.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.project.ProjectCreateRequest;
import org.focus.logmeet.controller.dto.project.ProjectCreateResponse;
import org.focus.logmeet.controller.dto.project.ProjectUpdateRequest;
import org.focus.logmeet.controller.dto.project.ProjectUpdateResponse;
import org.focus.logmeet.service.ProjectService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{projectId}")
    public BaseResponse<ProjectUpdateResponse> getProject(@PathVariable Long projectId) {
        log.info("프로젝트 정보 요청: projectId={}", projectId);
        ProjectUpdateResponse response = projectService.getProject(projectId);
        return new BaseResponse<>(response);
    }

    @PutMapping("/{projectId}")
    public BaseResponse<Void> updateProject(@PathVariable Long projectId, @RequestBody ProjectUpdateRequest request) {
        log.info("프로젝트 수정 요청 (PUT): projectId={}", projectId);
        projectService.updateProject(projectId, request.getName(), request.getContent(), request.getColor());
        return new BaseResponse<>(BaseExceptionResponseStatus.SUCCESS);
    }

    @DeleteMapping("/expel") //TODO: 자기 자신 추방 못하게 수정해야함
    public BaseResponse<Void> expelMember(@RequestParam Long projectId, @RequestParam Long userId) {
        log.info("참가자 추방 요청: projectId={}, userId={}", projectId, userId);
        projectService.expelMember(projectId, userId);
        return new BaseResponse<>(BaseExceptionResponseStatus.SUCCESS);
    }


    @PostMapping("/test")
    public Boolean testProject(String message) {
        log.info("테스트 요청: {}", message);
        return true;
    }
}
