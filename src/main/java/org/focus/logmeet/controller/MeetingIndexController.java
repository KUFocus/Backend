package org.focus.logmeet.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.service.MeetingIndexClient;
import org.focus.logmeet.service.MeetingIndexService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/minutes")
public class MeetingIndexController {
    private final MeetingIndexService service;

    @Operation(summary = "회의록 색인 요청", description = "저장된 원문으로 색인을 생성하거나 실패한 색인을 다시 요청합니다.")
    @PostMapping("/{minutesId}/index")
    public BaseResponse<MeetingIndexClient.IndexResult> index(@PathVariable Long minutesId) {
        return new BaseResponse<>(service.index(minutesId));
    }
}
