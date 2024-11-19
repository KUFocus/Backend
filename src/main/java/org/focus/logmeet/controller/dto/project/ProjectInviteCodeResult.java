package org.focus.logmeet.controller.dto.project;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ProjectInviteCodeResult {
    private Long projectId;
    private String inviteCode;
    private LocalDateTime expirationDate;
}
