package org.focus.logmeet.controller.dto.minutes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.focus.logmeet.domain.enums.MinutesType;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MinutesInfoResult {
    private Long minutesId;
    private Long projectId;
    private String name;
    private String content;
    private String filePath;
    private MinutesType minutesType;
    private LocalDateTime createdAt;
}
