package org.focus.logmeet.controller.dto.minutes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinutesSummarizeResult {
    private String summarizedText;
    private String extractedScheduleDate;
    private String extractedScheduleContent;
}
