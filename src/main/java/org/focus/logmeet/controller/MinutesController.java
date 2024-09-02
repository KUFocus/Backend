package org.focus.logmeet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.service.MinutesService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/minutes")
public class MinutesController {

    private final MinutesService minutesService;

    @PostMapping("/upload/voice")
    public Mono<BaseResponse<String>> uploadVoice(
            @RequestParam("file") String base64FileData,
            @RequestParam("minutesName") String minutesName,
            @RequestParam("fileName") String fileName,
            @RequestParam("projectId") Long projectId) {
        log.info("음성 파일 업로드 요청: minutesName={}, fileName={}, projectId={}", minutesName, fileName, projectId);
        return minutesService.processAndUploadVoice(base64FileData, minutesName, fileName, projectId)
                .map(BaseResponse::new);
    }

    @PostMapping("/upload/photo")
    public BaseResponse<String> uploadPhoto(
            @RequestParam("file") String base64FileData,
            @RequestParam("minutesName") String minutesName,
            @RequestParam("fileName") String fileName,
            @RequestParam("projectId") Long projectId) {
        log.info("사진 파일 업로드 요청: fileName={}, projectId={}", fileName, projectId);
        minutesService.uploadPhoto(base64FileData, minutesName, fileName, projectId);
        return new BaseResponse<>("Photo uploaded successfully.");
    }

    @PostMapping("/upload/manualEntry")
    public BaseResponse<String> uploadManualEntry(
            @RequestParam("text") String textContent,
            @RequestParam("minutesName") String minutesName,
            @RequestParam("projectId") Long projectId) {
        log.info("직접 작성한 회의록 업로드 요청: fileName={}, projectId={}", minutesName, projectId);
        minutesService.saveAndUploadManualEntry(textContent, minutesName, projectId);
        return new BaseResponse<>("Manual entry uploaded successfully.");
    }
}
