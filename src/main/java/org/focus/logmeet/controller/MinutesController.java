package org.focus.logmeet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.minutes.MinutesCreateRequest;
import org.focus.logmeet.controller.dto.minutes.MinutesCreateResponse;
import org.focus.logmeet.service.MinutesService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/minutes")
public class MinutesController {

    private final MinutesService minutesService;

    @PostMapping("/upload/voice")
    public Mono<BaseResponse<MinutesCreateResponse>> uploadVoice(@RequestBody MinutesCreateRequest request) {
        log.info("음성 파일 업로드 요청: minutesName={}, fileName={}, projectId={}", request.getMinutesName(), request.getFileName(), request.getProjectId());
        return minutesService.processAndUploadVoice(request.getBase64FileData(), request.getMinutesName(), request.getFileName(), request.getProjectId())
                .map(result -> new BaseResponse<>(new MinutesCreateResponse(result.getId(), result.getProject().getId())));
    }

    @PostMapping("/upload/photo")
    public BaseResponse<MinutesCreateResponse> uploadPhoto(@RequestBody MinutesCreateRequest request) {
        log.info("사진 파일 업로드 요청: minutesName={}, fileName={}, projectId={}", request.getMinutesName(), request.getFileName(), request.getProjectId());
        Long minutesId = minutesService.uploadPhoto(request.getBase64FileData(), request.getMinutesName(), request.getFileName(), request.getProjectId());
        return new BaseResponse<>(new MinutesCreateResponse(minutesId, request.getProjectId()));
    }

    @PostMapping("/upload/manualEntry")
    public BaseResponse<MinutesCreateResponse> uploadManualEntry(@RequestBody MinutesCreateRequest request) {
        log.info("직접 작성한 회의록 업로드 요청: fileName={}, projectId={}", request.getMinutesName(), request.getProjectId());
        Long minutesId = minutesService.saveAndUploadManualEntry(request.getTextContent(), request.getMinutesName(), request.getProjectId());
        return new BaseResponse<>(new MinutesCreateResponse(minutesId, request.getProjectId()));
    }
}
