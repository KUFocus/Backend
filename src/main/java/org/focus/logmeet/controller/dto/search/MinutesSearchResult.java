package org.focus.logmeet.controller.dto.search;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class MinutesSearchResult {
    private Long id;
    private String title;
    private String projectName;
    private String contentSnippet;
}
