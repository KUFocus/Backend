package org.focus.logmeet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.minutes.*;
import org.focus.logmeet.service.MinutesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/minutes")
public class MinutesController {

    private final MinutesService minutesService;

    @Operation(summary = "업데이트된 회의록 정보 반환", description = "파일로 생성된 회의록의 이름과 프로젝트 정보를 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업데이트된 회의록 정보 반환")
    })
    @PutMapping("/update-info")
    public BaseResponse<MinutesCreateResponse> updateMinutesInfo(
            @RequestBody MinutesInfoCreateRequest request) {
        return new BaseResponse<>(minutesService.updateMinutesInfo(request.getMinutesId(), request.getMinutesName(), request.getProjectId()));
    }

    @Operation(summary = "음성 또는 사진 파일을 업로드하여 회의록을 생성", description = "base64 인코딩된 파일 데이터를 업로드하여 회의록을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성된 회의록 정보 반환")
    })
    @PostMapping("/upload-file")
    public BaseResponse<MinutesFileUploadResponse> uploadFile(
            @RequestBody MinutesFileUploadRequest request) {
        log.info("파일 업로드 요청: fileName={}, fileType={}", request.getFileName(), request.getFileType());
        MinutesFileUploadResponse response = minutesService.uploadFile(request.getBase64FileData(), request.getFileName(), request.getFileType());
        return new BaseResponse<>(response);
    }

    @Operation(summary = "회의록의 텍스트 요약", description = "회의록의 텍스트를 요약하여 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "요약된 텍스트 반환")
    })
    @PostMapping("/{minutesId}/summarize-text")
    public BaseResponse<MinutesSummarizeResult> summarizeText(
            @Parameter(name = "minutesId", description = "요약할 회의록의 고유 ID", required = true)
            @PathVariable Long minutesId) {
        log.info("텍스트 요약 요청: minutesId={}", minutesId);
        MinutesSummarizeResult summarizedText = minutesService.summarizeText(minutesId);
        return new BaseResponse<>(summarizedText);
    }

    @Operation(summary = "텍스트로 회의록을 생성", description = "사용자가 직접 텍스트를 입력하여 회의록을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "생성된 회의록 정보 반환")
    })
    @PostMapping("/upload-content")
    public BaseResponse<MinutesCreateResponse> uploadManualEntry(
            @RequestBody MinutesManuallyCreateRequest request) {
        log.info("직접 작성한 회의록 업로드 요청: minutesName={}, projectId={}", request.getMinutesName(), request.getProjectId());
        MinutesCreateResponse response = minutesService.saveAndUploadManualEntry(request.getTextContent(), request.getMinutesName(), request.getProjectId());
        return new BaseResponse<>(response);
    }

    @Operation(summary = "단일 회의록 조회", description = "ID로 회의록의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회된 회의록 정보 반환")
    })
    @GetMapping("/{minutesId}")
    public BaseResponse<MinutesInfoResult> getMinutes(
            @Parameter(name = "minutesId", description = "조회할 회의록의 고유 ID", required = true)
            @PathVariable Long minutesId) {
        log.info("회의록 정보 요청: minutesId={}", minutesId);
        MinutesInfoResult result = minutesService.getMinutes(minutesId);
        return new BaseResponse<>(result);
    }

    @Operation(summary = "현재 유저의 회의록 리스트 조회", description = "현재 유저가 생성한 모든 회의록 리스트를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회된 회의록 리스트 반환")
    })
    @GetMapping("/minutes-list")
    public BaseResponse<List<MinutesListResult>> getMinutesList() {
        log.info("회의록 리스트 요청");
        List<MinutesListResult> results = minutesService.getMinutesList();
        return new BaseResponse<>(results);
    }

    @Operation(summary = "프로젝트별 회의록 조회", description = "특정 프로젝트의 회의록 리스트를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회된 프로젝트 회의록 리스트 반환")
    })
    @GetMapping("/{projectId}/minutes-list")
    public BaseResponse<List<MinutesListResult>> getProjectMinutes(
            @Parameter(name = "projectId", description = "조회할 프로젝트의 고유 ID", required = true)
            @PathVariable Long projectId) {
        log.info("특정 프로젝트에 속한 회의록 리스트 요청: projectId={}", projectId);
        List<MinutesListResult> results = minutesService.getProjectMinutes(projectId);
        return new BaseResponse<>(results);
    }

    @Operation(summary = "회의록 삭제", description = "ID로 회의록을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회의록 삭제 성공")
    })
    @DeleteMapping("/{minutesId}")
    public BaseResponse<Void> deleteMinutes(
            @Parameter(name = "minutesId", description = "삭제할 회의록의 고유 ID", required = true)
            @PathVariable Long minutesId) {
        log.info("회의록 삭제 요청: minutesId={}", minutesId);
        minutesService.deleteMinutes(minutesId);
        return new BaseResponse<>(SUCCESS);
    }
}
