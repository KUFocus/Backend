package org.focus.logmeet.controller.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.domain.enums.Role;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProjectListResult {
    private Long projectId;
    private String projectName;
    private Role role;
    private Boolean bookmark;
    private ProjectColor projectColor;
    private Integer numOfMember;
    private LocalDateTime createdAt;
}
