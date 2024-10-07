package org.focus.logmeet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.minutes.*;
import org.focus.logmeet.service.MinutesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/minutes")
public class MinutesController {

    private final MinutesService minutesService;

    /**
     * 파일로 생성된 회의록의 이름과 프로젝트 정보를 업데이트한다.
     * @param request 업데이트할 회의록의 ID, 이름, 프로젝트 ID를 포함
     * @return 업데이트된 회의록 정보 (MinutesCreateResponse)
     */
    @PutMapping("/update-info")
    public BaseResponse<MinutesCreateResponse> updateMinutesInfo(@RequestBody MinutesInfoCreateRequest request) {
        log.info("회의록 정보 설정: minutesName={}, projectId={}", request.getMinutesName(), request.getProjectId());
        MinutesCreateResponse response = minutesService.updateMinutesInfo(request.getMinutesId(), request.getMinutesName(), request.getProjectId());
        return new BaseResponse<>(response);
    }

    /**
     * 음성 또는 사진 파일을 업로드하여 회의록을 생성한다.
     * @param request base64 인코딩된 파일 데이터, 파일 이름, 파일 타입을 포함
     * @return 생성된 회의록의 파일 경로와 타입 정보 (MinutesFileUploadResponse)
     */
    @PostMapping("/upload-file") //TODO: 동일 파일명 업로드 시 덮어쓰는 문제 처리 필요
    public BaseResponse<MinutesFileUploadResponse> uploadFile(@RequestBody MinutesFileUploadRequest request) {
        log.info("파일 업로드 요청: fileName={}, fileType={}", request.getFileName(), request.getFileType());
        MinutesFileUploadResponse response = minutesService.uploadFile(request.getBase64FileData(), request.getFileName(), request.getFileType());
        return new BaseResponse<>(response);
    }

    /**
     * 회의록의 텍스트 내용을 요약한다.
     * @param request 요약할 텍스트를 포함한 요청
     * @return 요약된 텍스트 정보 (MinutesSummarizeResponse)
     */
    @PostMapping("/summarize-text") //TODO: 텍스트 요약 API 요청 보완 필요
    public BaseResponse<MinutesSummarizeResponse> summarizeText(@RequestBody MinutesSummarizeRequest request) {
        log.info("텍스트 요약 요청: extractedText={}", request.getExtractedText());
        return null;
    }

    /**
     * 텍스트를 직접 입력하여 수동으로 회의록을 생성한다.
     * @param request 회의록 이름, 프로젝트 ID, 입력된 텍스트 내용을 포함
     * @return 생성된 회의록 정보 (MinutesCreateResponse)
     */
    @PostMapping("/upload-content")
    public BaseResponse<MinutesCreateResponse> uploadManualEntry(@RequestBody MinutesManuallyCreateRequest request) {
        log.info("직접 작성한 회의록 업로드 요청: minutesName={}, projectId={}", request.getMinutesName(), request.getProjectId());
        MinutesCreateResponse response = minutesService.saveAndUploadManualEntry(request.getTextContent(), request.getMinutesName(), request.getProjectId());
        return new BaseResponse<>(response);
    }

    /**
     * 단일 회의록의 상세 정보를 조회한다.
     * @param minutesId 조회할 회의록의 ID
     * @return 조회된 회의록의 세부 정보 (MinutesInfoResult)
     */
    @GetMapping("/{minutesId}")
    public BaseResponse<MinutesInfoResult> getMinutes(@PathVariable Long minutesId) {
        log.info("회의록 정보 요청: minutesId={}", minutesId);
        MinutesInfoResult result = minutesService.getMinutes(minutesId);
        return new BaseResponse<>(result);
    }

    /**
     * 현재 유저의 회의록 리스트를 조회한다.
     * @return 조회된 회의록 리스트 정보 (MinutesListResult)
     */
    @GetMapping("/minutes-list")
    public BaseResponse<List<MinutesListResult>> getMinutesList() {
        log.info("회의록 리스트 요청");
        List<MinutesListResult> results = minutesService.getMinutesList();
        return new BaseResponse<>(results);
    }

    /**
     * 특정 프로젝트의 회의록 리스트를 조회한다.
     * @param projectId 조회할 회의록들이 속한 프로젝트의 ID
     * @return 조회된 프로젝트의 회의록 리스트 정보 (MinutesListResult)
     */
    @GetMapping("/{projectId}/minutes-list")
    public BaseResponse<List<MinutesListResult>> getProjectMinutes(@PathVariable Long projectId) {
        log.info("특정 프로젝트에 속한 회의록 리스트 요청: projectId={}", projectId);
        List<MinutesListResult> results = minutesService.getProjectMinutes(projectId);
        return new BaseResponse<>(results);
    }
}
