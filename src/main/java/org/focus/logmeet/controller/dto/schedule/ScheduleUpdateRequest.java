package org.focus.logmeet.controller.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleUpdateRequest {
    private String scheduleContent;
    private LocalDateTime scheduleDate;
}
