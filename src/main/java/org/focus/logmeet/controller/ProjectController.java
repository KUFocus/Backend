package org.focus.logmeet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Operation(summary = "새 프로젝트 생성", description = "새로운 프로젝트를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트가 성공적으로 생성됨",
                    content = @Content(schema = @Schema(implementation = ProjectCreateResponse.class)))
    })
    @PostMapping("/new")
    public BaseResponse<ProjectCreateResponse> createProject(
            @Validated @RequestBody ProjectCreateRequest request,
            BindingResult bindingResult) {
        log.info("프로젝트 생성 요청: {}", request.getName());
        validateBindingResult(bindingResult);

        ProjectCreateResponse response = projectService.createProject(request);
        return new BaseResponse<>(response);
    }

    @Operation(summary = "특정 프로젝트의 상세 정보 조회", description = "프로젝트 ID로 프로젝트의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트 세부 정보 반환",
                    content = @Content(schema = @Schema(implementation = ProjectInfoResult.class)))
    })
    @GetMapping("/{projectId}")
    public BaseResponse<ProjectInfoResult> getProject(@PathVariable Long projectId) {
        log.info("프로젝트 정보 요청: projectId={}", projectId);
        ProjectInfoResult result = projectService.getProject(projectId);
        return new BaseResponse<>(result);
    }

    @Operation(summary = "모든 프로젝트 목록 조회", description = "현재 사용자의 모든 프로젝트 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트 목록 반환",
                    content = @Content(schema = @Schema(implementation = ProjectListResult.class)))
    })
    @GetMapping("/project-list")
    public BaseResponse<List<ProjectListResult>> getProjectList() {
        log.info("프로젝트 리스트 요청");
        List<ProjectListResult> projectList = projectService.getProjectList();
        return new BaseResponse<>(projectList);
    }

    @Operation(summary = "즐겨찾기된 프로젝트 목록 조회", description = "즐겨찾기된 프로젝트 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "즐겨찾기된 프로젝트 목록 반환",
                    content = @Content(schema = @Schema(implementation = ProjectListResult.class)))
    })
    @GetMapping("/bookmark-list")
    public BaseResponse<List<ProjectListResult>> getProjectBookmarkList() {
        log.info("프로젝트 북마크 리스트 요청");
        List<ProjectListResult> projectBookmarkList = projectService.getProjectBookmarkList();
        return new BaseResponse<>(projectBookmarkList);
    }

    @Operation(summary = "프로젝트 정보 수정", description = "프로젝트의 이름, 설명, 색상을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트 수정 성공")
    })
    @PutMapping("/{projectId}")
    public BaseResponse<Void> updateProject(
            @PathVariable Long projectId,
            @RequestBody ProjectUpdateRequest request) {
        log.info("프로젝트 수정 요청: projectId={}", projectId);
        projectService.updateProject(projectId, request.getName(), request.getContent(), request.getColor());
        return new BaseResponse<>(SUCCESS);
    }

    @Operation(summary = "프로젝트 즐겨찾기 토글", description = "프로젝트의 즐겨찾기를 추가하거나 해제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트 즐겨찾기 토글 성공",
                    content = @Content(schema = @Schema(implementation = ProjectBookmarkResult.class)))
    })
    @PutMapping("/{projectId}/bookmark")
    public BaseResponse<ProjectBookmarkResult> bookmarkProjectToggle(@PathVariable Long projectId) {
        log.info("프로젝트 즐겨찾기 토글 요청: projectId={}", projectId);
        ProjectBookmarkResult result = projectService.bookmarkProjectToggle(projectId);
        return new BaseResponse<>(result);
    }

    @Operation(summary = "참가자 추방", description = "특정 프로젝트에서 참가자를 추방합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참가자 추방 성공")
    })
    @DeleteMapping("/expel")
    public BaseResponse<Void> expelMember(@RequestParam Long projectId, @RequestParam Long userId) {
        log.info("참가자 추방 요청: projectId={}, userId={}", projectId, userId);
        projectService.expelMember(projectId, userId);
        return new BaseResponse<>(SUCCESS);
    }

    @Operation(summary = "프로젝트 삭제", description = "특정 프로젝트를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트 삭제 성공")
    })
    @DeleteMapping("/{projectId}")
    public BaseResponse<Void> deleteProject(@PathVariable Long projectId) {
        log.info("프로젝트 삭제 요청: projectId={}", projectId);
        projectService.deleteProject(projectId);
        return new BaseResponse<>(SUCCESS);
    }

    @Operation(summary = "프로젝트 리더 임명", description = "프로젝트 리더를 다른 사용자로 임명합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트 리더 임명 성공",
                    content = @Content(schema = @Schema(implementation = ProjectLeaderDelegationResponse.class)))
    })
    @PutMapping("/{projectId}/leader")
    public BaseResponse<ProjectLeaderDelegationResponse> delegateLeader(@PathVariable Long projectId, @RequestBody ProjectLeaderDelegationRequest request) {
        log.info("프로젝트 리더 임명 요청: projectId={}", projectId);
        ProjectLeaderDelegationResponse result = projectService.delegateLeader(projectId, request.getNewLeaderId());
        return new BaseResponse<>(result);
    }

    @Operation(summary = "프로젝트 나가기", description = "현재 프로젝트에서 나갑니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트 나가기 성공")
    })
    @DeleteMapping("/{projectId}/leave")
    public BaseResponse<Void> leaveProject(@PathVariable Long projectId) {
        log.info("프로젝트 나가기 요청: projectId={}", projectId);
        projectService.leaveProject(projectId);
        return new BaseResponse<>(SUCCESS);
    }
}
