package org.focus.logmeet.controller.dto.project;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectLeaderDelegationRequest {
    private Long newLeaderId;
}
