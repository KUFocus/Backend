package org.focus.logmeet.controller.dto.minutes;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.focus.logmeet.domain.enums.MinutesType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinutesFileUploadRequest {
    @Schema(description = "Base64로 인코딩된 파일 데이터", example = "iVBORw0KGgoAAAANSUhEUgAAB...")
    private String base64FileData;

    @Schema(description = "업로드할 파일의 이름", example = "meeting_audio.mp3")
    private String fileName;

    @Schema(description = "파일 타입 (예: VOICE, PICTURE)", example = "PICTURE")
    private MinutesType fileType;
}
