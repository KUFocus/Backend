package org.focus.logmeet.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.focus.logmeet.common.response.BaseResponse;
import org.focus.logmeet.controller.dto.schedule.*;
import org.focus.logmeet.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PutMapping("/{scheduleId}")
    public BaseResponse<Void> updateSchedule(
            @PathVariable Long scheduleId,
            @RequestBody ScheduleUpdateRequest request) {
        log.info("스케줄 수정 요청: scheduleId={}", scheduleId);
        scheduleService.updateSchedule(scheduleId, request);
        return new BaseResponse<>(SUCCESS);
    }

    @GetMapping("/{scheduleId}")
    public BaseResponse<ScheduleInfoResult> getSchedule(@PathVariable Long scheduleId) {
        log.info("스케줄 정보 요청: scheduleId={}", scheduleId);
        ScheduleInfoResult result = scheduleService.getSchedule(scheduleId);
        return new BaseResponse<>(result);
    }

    @GetMapping("/{projectId}/schedule-list")
    public BaseResponse<List<ScheduleListOfProjectResult>> getScheduleOfProject(@PathVariable Long projectId) {
        log.info("프로젝트의 스케줄 리스트 요청: projectId={}", projectId);
        List<ScheduleListOfProjectResult> results = scheduleService.getScheduleOfProject(projectId);
        return new BaseResponse<>(results);
    }

    @DeleteMapping("/{scheduleId}")
    public BaseResponse<Void> deleteSchedule(@PathVariable Long scheduleId) {
        log.info("스케줄 삭제 요청: scheduleId={}",scheduleId);
        scheduleService.deleteSchedule(scheduleId);
        return new BaseResponse<>(SUCCESS);
    }
}
