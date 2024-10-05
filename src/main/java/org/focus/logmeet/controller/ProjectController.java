package org.focus.logmeet.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.project.*;
import org.focus.logmeet.service.ProjectService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.common.utils.ValidationUtils.validateBindingResult;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
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
    public BaseResponse<ProjectInfoResult> getProject(@PathVariable Long projectId) {
        log.info("프로젝트 정보 요청: projectId={}", projectId);
        ProjectInfoResult result = projectService.getProject(projectId);
        return new BaseResponse<>(result);
    }

    @GetMapping("/project-list")
    public BaseResponse<List<ProjectListResult>> getProjectList() {
        log.info("프로젝트 리스트 요청");
        List<ProjectListResult> projectList = projectService.getProjectList();
        return new BaseResponse<>(projectList);
    }

    @GetMapping("/bookmark-list")
    public BaseResponse<List<ProjectListResult>> getProjectBookmarkList() {
        log.info("프로젝트 북마크 리스트 요청");
        List<ProjectListResult> projectBookmarkList = projectService.getProjectBookmarkList();
        return new BaseResponse<>(projectBookmarkList);
    }


    @PutMapping("/{projectId}")
    public BaseResponse<Void> updateProject(@PathVariable Long projectId, @RequestBody ProjectUpdateRequest request) {
        log.info("프로젝트 수정 요청 (PUT): projectId={}", projectId);
        projectService.updateProject(projectId, request.getName(), request.getContent(), request.getColor());
        return new BaseResponse<>(SUCCESS);
    }

    @PutMapping("/{projectId}/bookmark")
    public BaseResponse<ProjectBookmarkResult> bookmarkProjectToggle(@PathVariable Long projectId) {
        log.info("프로젝트 즐겨찾기 추가/해제 요청: projectId={}", projectId);
        ProjectBookmarkResult result = projectService.bookmarkProjectToggle(projectId);
        return new BaseResponse<>(result);
    }

    @DeleteMapping("/expel")
    public BaseResponse<Void> expelMember(@RequestParam Long projectId, @RequestParam Long userId) {
        log.info("참가자 추방 요청: projectId={}, userId={}", projectId, userId);
        projectService.expelMember(projectId, userId);
        return new BaseResponse<>(SUCCESS);
    }

    @DeleteMapping("/{projectId}")
    public BaseResponse<Void> deleteProject(@PathVariable Long projectId) {
        log.info("프로젝트 삭제 요청: projectId={}", projectId);
        projectService.deleteProject(projectId);
        return new BaseResponse<>(SUCCESS);
    }

    @DeleteMapping("/{projectId}/leave")
    public BaseResponse<Void> leaveProject(@PathVariable Long projectId) {
        log.info("프로젝트 나가기 요청: projectId={}", projectId);
        projectService.leaveProject(projectId);
        return new BaseResponse<>(SUCCESS);
    }
}
