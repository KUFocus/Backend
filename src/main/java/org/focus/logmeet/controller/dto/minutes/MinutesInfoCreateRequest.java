package org.focus.logmeet.controller.dto.minutes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinutesWithFileCreateRequest {
    private String base64FileData;
    private String minutesName;
    private String fileName;
    private Long projectId;
}
