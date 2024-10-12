package org.focus.logmeet.controller.dto.minutes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.focus.logmeet.controller.dto.schedule.ScheduleDto;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinutesSummarizeResult {
    private String summarizedText;
    private List<ScheduleDto> schedules;
}
