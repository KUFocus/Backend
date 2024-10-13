package org.focus.logmeet.controller.dto.minutes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MinutesCreateResponse {
    private Long minutesId;
    private Long projectId;
}
