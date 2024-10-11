package org.focus.logmeet.controller.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.focus.logmeet.domain.enums.ProjectColor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleInfoResult {
    private Long projectId;
    private String projectName;
    private String scheduleContent;
    private LocalDateTime scheduleDate;
    private ProjectColor color;
}