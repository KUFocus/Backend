package org.focus.logmeet.controller.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleDto {
    private String extractedScheduleDate;
    private String extractedScheduleContent;
}
