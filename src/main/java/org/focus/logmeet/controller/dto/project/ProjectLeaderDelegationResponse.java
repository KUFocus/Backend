package org.focus.logmeet.controller.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectLeaderDelegationResponse {
    private Long projectId;
    private Long newLeaderId;
}
