package org.focus.logmeet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.schedule.ScheduleCreateRequest;
import org.focus.logmeet.controller.dto.schedule.ScheduleCreateResponse;
import org.focus.logmeet.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/schedule")
public class ScheduleController {
    private final ScheduleService scheduleService;
    @PostMapping("/new")
    public BaseResponse<ScheduleCreateResponse> createSchedule(@RequestBody ScheduleCreateRequest request) {
        log.info("스케줄 생성 요청: {}", request.getScheduleContent());
        ScheduleCreateResponse response = scheduleService.createSchedule(request);
        return new BaseResponse<>(response);
    }

    @DeleteMapping("/{scheduleId}")
    public BaseResponse<Void> deleteSchedule(@PathVariable Long scheduleId) {
        log.info("스케줄 삭제 요청: scheduleId={}",scheduleId);
        scheduleService.deleteSchedule(scheduleId);
        return new BaseResponse<>(SUCCESS);
    }
}
