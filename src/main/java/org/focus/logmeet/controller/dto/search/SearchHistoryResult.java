package org.focus.logmeet.controller.dto.search;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistoryResult {
    private String title;
    private String projectName;
}
