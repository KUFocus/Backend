package org.focus.logmeet.controller.dto.minutes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.focus.logmeet.domain.enums.MinutesType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinutesFileUploadRequest {
    private String base64FileData;
    private String fileName;
    private MinutesType fileType;
}
