package org.focus.logmeet.controller.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.focus.logmeet.domain.enums.ProjectColor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ScheduleInfoResult {
    private Long projectId;
    private String projectName;
    private String scheduleContent;
    private LocalDateTime scheduleDate;
    private ProjectColor color;
}
