package org.focus.logmeet.controller.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectLeaderDelegationResponse {
    private Long projectId;
    private Long newLeaderId;
}
