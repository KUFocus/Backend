package org.focus.logmeet.controller.dto.meeting;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MeetingAnswer(String status, String answer, List<Claim> claims) {
    public record Claim(String text, List<Citation> citations) {}
    public record Citation(@JsonProperty("project_id") Long projectId,
                           @JsonProperty("minutes_id") Long minutesId,
                           @JsonProperty("source_hash") String sourceHash,
                           String quote, Integer start, Integer end) {}
}
