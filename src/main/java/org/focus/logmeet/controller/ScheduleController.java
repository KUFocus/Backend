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
import org.focus.logmeet.controller.dto.schedule.*;
import org.focus.logmeet.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.SUCCESS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "새 스케줄 생성", description = "새로운 스케줄을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스케줄이 성공적으로 생성됨",
                    content = @Content(schema = @Schema(implementation = ScheduleCreateResponse.class)))
    })
    @PostMapping("/new")
    public BaseResponse<ScheduleCreateResponse> createSchedule(@RequestBody ScheduleCreateRequest request) {
        log.info("스케줄 생성 요청: {}", request.getScheduleContent());
        ScheduleCreateResponse response = scheduleService.createSchedule(request);
        return new BaseResponse<>(response);
    }

    @Operation(summary = "스케줄 수정", description = "특정 스케줄의 내용을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스케줄 수정 성공")
    })
    @PutMapping("/{scheduleId}")
    public BaseResponse<Void> updateSchedule(
            @Parameter(name = "scheduleId", description = "수정할 스케줄의 ID", required = true)
            @PathVariable Long scheduleId,
            @RequestBody ScheduleUpdateRequest request) {
        log.info("스케줄 수정 요청: scheduleId={}", scheduleId);
        scheduleService.updateSchedule(scheduleId, request);
        return new BaseResponse<>(SUCCESS);
    }

    @Operation(summary = "스케줄 정보 조회", description = "특정 스케줄의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스케줄 상세 정보 반환",
                    content = @Content(schema = @Schema(implementation = ScheduleInfoResult.class)))
    })
    @GetMapping("/{scheduleId}")
    public BaseResponse<ScheduleInfoResult> getSchedule(
            @Parameter(name = "scheduleId", description = "조회할 스케줄의 ID", required = true)
            @PathVariable Long scheduleId) {
        log.info("스케줄 정보 요청: scheduleId={}", scheduleId);
        ScheduleInfoResult result = scheduleService.getSchedule(scheduleId);
        return new BaseResponse<>(result);
    }

    @Operation(summary = "프로젝트의 월별 스케줄 조회", description = "특정 프로젝트의 월별 스케줄 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "월별 스케줄 목록 반환",
                    content = @Content(schema = @Schema(implementation = ScheduleListResult.class)))
    })
    @GetMapping("/{projectId}/schedule-list")
    public BaseResponse<List<ScheduleMonthlyListResult>> getScheduleOfProject(
            @Parameter(name = "projectId", description = "조회할 프로젝트의 ID", required = true)
            @PathVariable Long projectId,
            @Parameter(name = "yearMonth", description = "조회할 연도 및 월 (yyyy-MM-xx 형식)", required = true)
            @RequestParam("yearMonth") LocalDate yearMonth) {
        log.info("프로젝트의 월별 스케줄 리스트 요청: projectId={}, yearMonth={}", projectId, yearMonth);
        List<ScheduleMonthlyListResult> results = scheduleService.getScheduleOfProject(projectId, yearMonth);
        return new BaseResponse<>(results);
    }

    @Operation(summary = "프로젝트의 특정 날짜의 스케줄 조회", description = "특정 프로젝트의 특정 날짜의 스케줄 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "특정 날짜의 스케줄 목록 반환",
                    content = @Content(schema = @Schema(implementation = ScheduleListResult.class)))
    })
    @GetMapping("/{projectId}/schedules")
    public BaseResponse<List<ScheduleListResult>> getScheduleOfProjectAt(
            @Parameter(name = "projectId", description = "조회할 프로젝트의 ID", required = true)
            @PathVariable Long projectId,
            @Parameter(name = "date", description = "조회할 날짜 (yyyy-MM-dd 형식)", required = true)
            @RequestParam("date") LocalDate date) {
        log.info("프로젝트의 특정 날짜의 스케줄 리스트 요청: projectId={}, date={}", projectId, date);
        List<ScheduleListResult> results = scheduleService.getScheduleOfProjectAt(projectId, date);
        return new BaseResponse<>(results);
    }

    @Operation(summary = "사용자의 월별 스케줄 조회", description = "사용자의 월별 스케줄 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자의 월별 스케줄 목록 반환",
                    content = @Content(schema = @Schema(implementation = ScheduleListResult.class)))
    })
    @GetMapping("/users/schedule-list")
    public BaseResponse<List<ScheduleMonthlyListResult>> getScheduleOfUser(
            @Parameter(name = "yearMonth", description = "조회할 연도 및 월 (yyyy-MM-xx 형식)", required = true)
            @RequestParam("yearMonth") LocalDate yearMonth) {
        log.info("유저의 월별 스케줄 리스트 요청: yearMonth={}", yearMonth);
        List<ScheduleMonthlyListResult> results = scheduleService.getScheduleOfUser(yearMonth);
        return new BaseResponse<>(results);
    }

    @Operation(summary = "사용자의 특정 날짜의 스케줄 조회", description = "사용자의 특정 날짜에 해당하는 스케줄 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자의 특정 날짜의 스케줄 목록 반환",
                    content = @Content(schema = @Schema(implementation = ScheduleListResult.class)))
    })
    @GetMapping("/users/schedules")
    public BaseResponse<List<ScheduleListResult>> getScheduleOfUserAt(
            @Parameter(name = "date", description = "조회할 날짜 (yyyy-MM-dd 형식)", required = true)
            @RequestParam("date") LocalDate date) {
        log.info("유저의 특정 날짜의 스케줄 리스트 요청: date={}", date);
        List<ScheduleListResult> results = scheduleService.getScheduleOfUserAt(date);
        return new BaseResponse<>(results);
    }

    @Operation(summary = "스케줄 삭제", description = "특정 스케줄을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스케줄 삭제 성공")
    })
    @DeleteMapping("/{scheduleId}")
    public BaseResponse<Void> deleteSchedule(
            @Parameter(name = "scheduleId", description = "삭제할 스케줄의 ID", required = true)
            @PathVariable Long scheduleId) {
        log.info("스케줄 삭제 요청: scheduleId={}", scheduleId);
        scheduleService.deleteSchedule(scheduleId);
        return new BaseResponse<>(SUCCESS);
    }
}
