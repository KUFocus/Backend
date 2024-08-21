package org.focus.logmeet.controller.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ProjectInfoResult {
    private Long projectId;
    private String name;
    private String content;
    private LocalDateTime createdAt;
    private List<UserProjectDto> userProjects;
}
