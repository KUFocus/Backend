package org.focus.logmeet.controller.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.focus.logmeet.domain.enums.ProjectColor;

@Getter
@AllArgsConstructor
public class ProjectCreateRequest  {
    @Schema(description = "프로젝트의 이름", example = "로그밋 프로젝트")
    private String name;

    @Schema(description = "프로젝트의 설명", example = "2024 졸업프로젝트로 진행하는 팀플")
    private String content;

    @Schema(description = "프로젝트의 색상(PROJECT_1 ~ PROJECT_12)", example = "PROJECT_11")
    private ProjectColor color;
}
