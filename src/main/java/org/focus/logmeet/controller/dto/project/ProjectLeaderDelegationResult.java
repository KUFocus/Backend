package org.focus.logmeet.controller.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectLeaderDelegationResult {
    private Long projectId;
    private Long newLeaderId;
}
