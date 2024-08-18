package org.focus.logmeet.controller.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProjectUpdateResponse {
    private Long projectId;
    private String name;
    private String content;
    private List<UserProjectDto> userProjects;
}
