package org.focus.logmeet.controller.dto.minutes;

import lombok.*;
import org.focus.logmeet.controller.dto.schedule.ScheduleDto;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MinutesSummarizeResult {
    private String summarizedText;
    private List<ScheduleDto> schedules;
}
