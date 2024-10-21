package org.focus.logmeet.controller.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.focus.logmeet.domain.enums.ProjectColor;

import java.util.Set;

@Getter
@AllArgsConstructor
public class ScheduleMonthlyListResult {
    private int date;
    private Set<ProjectColor> colors;
}
