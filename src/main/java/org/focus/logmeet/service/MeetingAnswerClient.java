package org.focus.logmeet.service;

import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.meeting.MeetingAnswer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.MINUTES_FLASK_SERVER_COMMUNICATION_ERROR;

@Slf4j
@Component
public class MeetingAnswerClient {
    private final RestTemplate restTemplate;
    private final String url;

    public MeetingAnswerClient(RestTemplateBuilder builder, @Value("${flask.server.url}") String serverUrl) {
        restTemplate = builder.setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(60)).build();
        url = serverUrl.replaceAll("/+$", "") + "/answer_meeting";
    }

    public MeetingAnswer answer(Long projectId, Long minutesId, String question) {
        try {
            MeetingAnswer result = restTemplate.postForObject(url, new Question(projectId, minutesId, question), MeetingAnswer.class);
            if (result == null || result.answer() == null || result.answer().isBlank() || result.claims() == null
                    || !("answered".equals(result.status()) && !result.claims().isEmpty()
                    || "insufficient_evidence".equals(result.status()) && result.claims().isEmpty())) {
                throw new IllegalStateException("회의록 답변 상태와 본문이 올바르지 않습니다.");
            }
            for (MeetingAnswer.Claim claim : result.claims()) {
                if (claim == null || claim.text() == null || claim.text().isBlank()
                        || claim.citations() == null || claim.citations().isEmpty()) {
                    throw new IllegalStateException("답변 문장에 인용 근거가 없습니다.");
                }
                for (MeetingAnswer.Citation citation : claim.citations()) {
                    if (citation == null || !projectId.equals(citation.projectId())
                            || citation.minutesId() == null || citation.minutesId() <= 0
                            || minutesId != null && !minutesId.equals(citation.minutesId())
                            || citation.quote() == null || citation.quote().isBlank()
                            || citation.sourceHash() == null || !citation.sourceHash().matches("[0-9a-f]{64}")
                            || citation.start() == null || citation.end() == null
                            || citation.start() < 0 || citation.end() <= citation.start()) {
                        throw new IllegalStateException("회의록 인용 근거의 범위나 형식이 올바르지 않습니다.");
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("회의록 질문 요청에 실패했습니다. 프로젝트 ID={}, 회의록 ID={}", projectId, minutesId, e);
            throw new BaseException(MINUTES_FLASK_SERVER_COMMUNICATION_ERROR);
        }
    }

    private record Question(Long projectId, Long minutesId, String question) {}
}
