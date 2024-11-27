package org.focus.logmeet.controller.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class MinutesSearchHistoryResult {
    private LocalDateTime searchDate;
    private Long minutesId;
    private String title;
    private String projectName;
}
