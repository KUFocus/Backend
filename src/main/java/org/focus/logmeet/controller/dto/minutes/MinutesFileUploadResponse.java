package org.focus.logmeet.controller.dto.minutes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.focus.logmeet.domain.enums.MinutesType;

@Getter
@AllArgsConstructor
public class MinutesFileUploadResponse {
    private Long minutesId;
    private String filePath;
    private MinutesType fileType;
}
