package org.focus.logmeet.controller.dto.project;

import lombok.Getter;
import lombok.Setter;
import org.focus.logmeet.domain.enums.ProjectColor;

@Getter
@Setter
public class ProjectUpdateRequest {
    private String name;
    private String content;
    private ProjectColor color;
}
