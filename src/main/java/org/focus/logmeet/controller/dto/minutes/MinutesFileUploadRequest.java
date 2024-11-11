package org.focus.logmeet.controller.dto.minutes;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinutesFileUploadRequest {
    @Schema(description = "업로드할 파일의 path", example = "https://kr.object.ncloudstorage.com/logmeet/minutes_voice/example.mp3")
    private String path;
}
