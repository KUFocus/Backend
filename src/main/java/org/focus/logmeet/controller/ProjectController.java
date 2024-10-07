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

    /**
     * 새 프로젝트를 생성한다.
     * @param request 프로젝트 생성에 필요한 정보 (이름, 설명, 색상 등)
     * @param bindingResult 유효성 검사 결과
     * @return 생성된 프로젝트의 정보 (ProjectCreateResponse)
     */
    @PostMapping("/new")
    public BaseResponse<ProjectCreateResponse> createProject(@Validated @RequestBody ProjectCreateRequest request, BindingResult bindingResult) {
        log.info("프로젝트 생성 요청: {}", request.getName());
        validateBindingResult(bindingResult);

        ProjectCreateResponse response = projectService.createProject(request);
        return new BaseResponse<>(response);
    }

    /**
     * 특정 프로젝트의 상세 정보를 조회한다.
     * @param projectId 조회할 프로젝트의 ID
     * @return 프로젝트의 세부 정보 (ProjectInfoResult)
     */
    @GetMapping("/{projectId}")
    public BaseResponse<ProjectInfoResult> getProject(@PathVariable Long projectId) {
        log.info("프로젝트 정보 요청: projectId={}", projectId);
        ProjectInfoResult result = projectService.getProject(projectId);
        return new BaseResponse<>(result);
    }

    /**
     * 모든 프로젝트의 목록을 조회한다.
     * @return 프로젝트 목록 (List<ProjectListResult>)
     */
    @GetMapping("/project-list")
    public BaseResponse<List<ProjectListResult>> getProjectList() {
        log.info("프로젝트 리스트 요청");
        List<ProjectListResult> projectList = projectService.getProjectList();
        return new BaseResponse<>(projectList);
    }

    /**
     * 즐겨찾기된 프로젝트의 목록을 조회한다.
     * @return 즐겨찾기된 프로젝트 목록 (List<ProjectListResult>)
     */
    @GetMapping("/bookmark-list")
    public BaseResponse<List<ProjectListResult>> getProjectBookmarkList() {
        log.info("프로젝트 북마크 리스트 요청");
        List<ProjectListResult> projectBookmarkList = projectService.getProjectBookmarkList();
        return new BaseResponse<>(projectBookmarkList);
    }

    /**
     * 특정 프로젝트의 정보를 수정한다.
     * @param projectId 수정할 프로젝트의 ID
     * @param request 새로운 이름, 설명, 색상 정보를 포함
     * @return 성공 여부
     */
    @PutMapping("/{projectId}")
    public BaseResponse<Void> updateProject(@PathVariable Long projectId, @RequestBody ProjectUpdateRequest request) {
        log.info("프로젝트 수정 요청 (PUT): projectId={}", projectId);
        projectService.updateProject(projectId, request.getName(), request.getContent(), request.getColor());
        return new BaseResponse<>(SUCCESS);
    }

    /**
     * 특정 프로젝트의 즐겨찾기 추가/해제를 토글한다.
     * @param projectId 즐겨찾기 추가/해제할 프로젝트의 ID
     * @return 즐겨찾기 토글 결과 (ProjectBookmarkResult)
     */
    @PutMapping("/{projectId}/bookmark")
    public BaseResponse<ProjectBookmarkResult> bookmarkProjectToggle(@PathVariable Long projectId) {
        log.info("프로젝트 즐겨찾기 추가/해제 요청: projectId={}", projectId);
        ProjectBookmarkResult result = projectService.bookmarkProjectToggle(projectId);
        return new BaseResponse<>(result);
    }

    /**
     * 특정 프로젝트에서 참가자를 추방한다.
     * @param projectId 추방할 프로젝트의 ID
     * @param userId 추방할 사용자 ID
     * @return 성공 여부
     */
    @DeleteMapping("/expel")
    public BaseResponse<Void> expelMember(@RequestParam Long projectId, @RequestParam Long userId) {
        log.info("참가자 추방 요청: projectId={}, userId={}", projectId, userId);
        projectService.expelMember(projectId, userId);
        return new BaseResponse<>(SUCCESS);
    }

    /**
     * 특정 프로젝트를 삭제한다.
     * @param projectId 삭제할 프로젝트의 ID
     * @return 성공 여부
     */
    @DeleteMapping("/{projectId}")
    public BaseResponse<Void> deleteProject(@PathVariable Long projectId) {
        log.info("프로젝트 삭제 요청: projectId={}", projectId);
        projectService.deleteProject(projectId);
        return new BaseResponse<>(SUCCESS);
    }

    /**
     * 특정 프로젝트의 리더를 다른 사용자로 임명한다.
     * @param projectId 리더를 임명할 프로젝트의 ID
     * @param request 새로운 리더의 사용자 ID를 포함
     * @return 리더 임명 결과 (ProjectLeaderDelegationResponse)
     */
    @PutMapping("/{projectId}/leader")
    public BaseResponse<ProjectLeaderDelegationResponse> delegateLeader(@PathVariable Long projectId, @RequestBody ProjectLeaderDelegationRequest request) {
        log.info("프로젝트 리더 임명 요청: projectId={}", projectId);
        ProjectLeaderDelegationResponse result = projectService.delegateLeader(projectId, request.getNewLeaderId());
        return new BaseResponse<>(result);
    }

    /**
     * 현재 프로젝트에서 나간다.
     * @param projectId 나갈 프로젝트의 ID
     * @return 성공 여부
     */
    @DeleteMapping("/{projectId}/leave")
    public BaseResponse<Void> leaveProject(@PathVariable Long projectId) {
        log.info("프로젝트 나가기 요청: projectId={}", projectId);
        projectService.leaveProject(projectId);
        return new BaseResponse<>(SUCCESS);
    }
}
