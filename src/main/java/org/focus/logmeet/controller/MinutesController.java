package org.focus.logmeet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.service.MinutesService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/minutes")
public class MinutesController {

    private final MinutesService minutesService;

    @PostMapping("/upload/voice")
    public Mono<BaseResponse<String>> uploadVoice(@RequestParam("file") String base64FileData, @RequestParam("fileName") String fileName) {
        log.info("음성 파일 업로드 요청: fileName={}", fileName);
        return minutesService.processAndUploadVoice(base64FileData, fileName)
                .map(BaseResponse::new);
    }

    @PostMapping("/upload/photo")
    public BaseResponse<String> uploadPhoto(@RequestParam("file") String base64FileData, @RequestParam("fileName") String fileName) {
        log.info("사진 파일 업로드 요청: fileName={}", fileName);
        minutesService.uploadPhoto(base64FileData, fileName);
        return new BaseResponse<>("Photo uploaded successfully.");
    }
}
