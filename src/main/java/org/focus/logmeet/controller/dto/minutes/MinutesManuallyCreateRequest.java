package org.focus.logmeet.controller.dto.minutes;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinutesManuallyCreateRequest {
    @Schema(description = "수동으로 생성할 회의록의 이름", example = "정기 대면회의 및 주간 목표 설정")
    private String minutesName;

    @Schema(description = "회의록에 포함될 텍스트 내용", example = "2주에 한번씩 대면회의 (그때그때 유동적으로 고고), 매주 1회 회의")
    private String textContent;

    @Schema(description = "회의록이 속한 프로젝트의 ID", example = "116")
    private Long projectId;
}
