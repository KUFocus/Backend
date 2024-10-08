package org.focus.logmeet.controller.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectLeaderDelegationRequest {
    @Schema(description = "새로 임명할 리더의 사용자 ID", example = "116")
    private Long newLeaderId;
}
