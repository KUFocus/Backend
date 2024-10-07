package org.focus.logmeet.controller.dto.minutes;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.focus.logmeet.controller.dto.project.UserProjectDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class MinutesInfoResult {
    private Long minutesId;
    private Long projectId;
    private String name;
    private String content;
    private String filePath;
    private LocalDateTime createdAt;
}
