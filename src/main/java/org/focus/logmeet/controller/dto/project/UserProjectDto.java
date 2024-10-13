package org.focus.logmeet.controller.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.domain.enums.Role;

@Getter
@AllArgsConstructor
public class UserProjectDto {
    private Long id;
    private Long userId;
    private String userName;
    private Role role;
    private Boolean bookmark;
    private ProjectColor color;
}
