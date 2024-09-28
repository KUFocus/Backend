package org.focus.logmeet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.minutes.*;
import org.focus.logmeet.service.MinutesService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/minutes")
public class MinutesController {

    private final MinutesService minutesService;

    @PutMapping("/update-info")
    public BaseResponse<MinutesCreateResponse> updateMinutesInfo(@RequestBody MinutesInfoCreateRequest request) {
        log.info("회의록 정보 설정: minutesName={}, projectId={}", request.getMinutesName(), request.getProjectId());
        MinutesCreateResponse response = minutesService.updateMinutesInfo(request.getMinutesId(), request.getMinutesName(), request.getProjectId());
        return new BaseResponse<>(response);
    }

    @PostMapping("/upload-file")
    public BaseResponse<MinutesFileUploadResponse> uploadFile(@RequestBody MinutesFileUploadRequest request) {
        log.info("파일 업로드 요청: fileName={}, fileType={}", request.getFileName(), request.getFileType());
        MinutesFileUploadResponse response = minutesService.uploadFile(request.getBase64FileData(), request.getFileName(), request.getFileType());
        return new BaseResponse<>(response);
    }

    //TODO: 텍스트 요약 요청 보완 필요
    @PostMapping("/summarize-text")
    public BaseResponse<MinutesSummarizeResponse> summarizeText(@RequestBody MinutesSummarizeRequest request) {
        log.info("텍스트 요약 요청: extractedText={}", request.getExtractedText());
        return null;
    }

    @PostMapping("/upload-content")
    public BaseResponse<MinutesCreateResponse> uploadManualEntry(@RequestBody MinutesManuallyCreateRequest request) {
        log.info("직접 작성한 회의록 업로드 요청: minutesName={}, projectId={}", request.getMinutesName(), request.getProjectId());
        MinutesCreateResponse response = minutesService.saveAndUploadManualEntry(request.getTextContent(), request.getMinutesName(), request.getProjectId());
        return new BaseResponse<>(response);
    }
}
