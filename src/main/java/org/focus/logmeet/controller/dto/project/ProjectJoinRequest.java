package org.focus.logmeet.controller.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectJoinRequest {
    @Schema(description = "프로젝트 초대 코드", example = "ABCDEFGH")
    private String inviteCode;
}
