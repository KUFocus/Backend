package org.focus.logmeet.service;

import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.MINUTES_FLASK_SERVER_COMMUNICATION_ERROR;

@Slf4j
@Component
public class MeetingIndexClient {
    private final RestTemplate restTemplate;
    private final String indexUrl;

    public MeetingIndexClient(RestTemplateBuilder builder, @Value("${flask.server.url}") String serverUrl) {
        this.restTemplate = builder.setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(60)).build();
        this.indexUrl = serverUrl.replaceAll("/+$", "") + "/index_meeting";
    }

    // 호출자는 프로젝트 접근 권한과 회의록 저장 완료 여부를 먼저 확인해야 한다.
    public IndexResult index(Long projectId, Long minutesId, String text) {
        try {
            if (projectId == null || projectId <= 0 || minutesId == null || minutesId <= 0
                    || text == null || text.isBlank() || text.codePointCount(0, text.length()) > 100000) {
                throw new IllegalArgumentException("회의록 색인 요청 값이 올바르지 않습니다.");
            }
            IndexResult result = restTemplate.postForObject(indexUrl,
                    new IndexRequest(projectId, minutesId, text), IndexResult.class);
            String sourceHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
            if (result == null || !projectId.equals(result.projectId()) || !minutesId.equals(result.minutesId())
                    || result.changed() == null || result.chunkCount() == null || result.chunkCount() <= 0
                    || !sourceHash.equals(result.sourceHash())) {
                throw new IllegalStateException("회의록 색인 응답이 요청 원문과 일치하지 않습니다.");
            }
            return result;
        } catch (Exception e) {
            log.error("회의록 색인 요청에 실패했습니다. 프로젝트 ID={}, 회의록 ID={}", projectId, minutesId, e);
            throw new BaseException(MINUTES_FLASK_SERVER_COMMUNICATION_ERROR);
        }
    }

    private record IndexRequest(Long projectId, Long minutesId, String text) {}

    public record IndexResult(Long projectId, Long minutesId, Boolean changed, Integer chunkCount,
                              String sourceHash) {}
}
