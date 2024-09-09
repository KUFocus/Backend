package org.focus.logmeet.controller.dto.minutes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinutesManuallyCreateRequest {
    private String minutesName;
    private String textContent;
    private Long projectId;
}
