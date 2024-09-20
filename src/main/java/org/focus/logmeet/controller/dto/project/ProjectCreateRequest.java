package org.focus.logmeet.controller.dto.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.focus.logmeet.domain.enums.ProjectColor;

@Getter
@AllArgsConstructor
public class ProjectCreateRequest  {
    private String name;
    private String content;
    private ProjectColor color;
}
