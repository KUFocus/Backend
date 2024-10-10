package org.focus.logmeet.controller.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleCreateRequest {
    @Schema(description = "생성할 스케줄 정보", example = "졸프 6주차 활동지 제출")
    private String scheduleContent;

    @Schema(description = "스케줄 날짜 정보", example = "2024-11-06T14:30:00")
    private LocalDateTime scheduleDate;

    @Schema(description = "스케줄이 속한 프로젝트의 ID", example = "116")
    private Long projectId;
}