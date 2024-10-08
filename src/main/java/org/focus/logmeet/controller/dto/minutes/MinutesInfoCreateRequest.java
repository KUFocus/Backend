package org.focus.logmeet.controller.dto.minutes;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinutesInfoCreateRequest {
    @Schema(description = "회의록의 ID", example = "116")
    private Long minutesId;

    @Schema(description = "업데이트할 회의록의 이름", example = "정기 대면회의 및 주간 목표 설정")
    private String minutesName;

    @Schema(description = "회의록이 속한 프로젝트의 ID", example = "116")
    private Long projectId;
}
