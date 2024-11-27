package org.focus.logmeet.controller.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class MinutesSearchHistoryResult {
    private Long searchHistoryId;
    private LocalDateTime searchDate;
    private List<SearchHistoryResult> results;
}
