package org.focus.logmeet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.minutes.MinutesManuallyCreateRequest;
import org.focus.logmeet.controller.dto.minutes.MinutesWithFileCreateRequest;
import org.focus.logmeet.controller.dto.minutes.MinutesCreateResponse;
import org.focus.logmeet.service.MinutesService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/minutes")
public class MinutesController {

    private final MinutesService minutesService;

    @PostMapping("/upload/voice")
    public BaseResponse<MinutesCreateResponse> uploadVoice(@RequestBody MinutesWithFileCreateRequest request) {
        log.info("음성 파일 업로드 요청: minutesName={}, fileName={}, projectId={}", request.getMinutesName(), request.getFileName(), request.getProjectId());
        MinutesCreateResponse response = minutesService.processAndUploadVoice(request.getBase64FileData(), request.getMinutesName(), request.getFileName(), request.getProjectId());
        return new BaseResponse<>(response);
    }

    @PostMapping("/upload/photo")
    public BaseResponse<MinutesCreateResponse> uploadPhoto(@RequestBody MinutesWithFileCreateRequest request) {
        log.info("사진 파일 업로드 요청: minutesName={}, fileName={}, projectId={}", request.getMinutesName(), request.getFileName(), request.getProjectId());
        MinutesCreateResponse response = minutesService.uploadPhoto(request.getBase64FileData(), request.getMinutesName(), request.getFileName(), request.getProjectId());
        return new BaseResponse<>(response);
    }

    @PostMapping("/upload/manualEntry")
    public BaseResponse<MinutesCreateResponse> uploadManualEntry(@RequestBody MinutesManuallyCreateRequest request) {
        log.info("직접 작성한 회의록 업로드 요청: minutesName={}, projectId={}", request.getMinutesName(), request.getProjectId());
        MinutesCreateResponse response = minutesService.saveAndUploadManualEntry(request.getTextContent(), request.getMinutesName(), request.getProjectId());
        return new BaseResponse<>(response);
    }
}
