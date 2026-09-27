package org.focus.logmeet.service;

import org.focus.logmeet.common.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.MINUTES_FLASK_SERVER_COMMUNICATION_ERROR;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MeetingIndexClientTest {
    private MockRestServiceServer server;
    private MeetingIndexClient client;
    private final String text = "민수: 다음 주 화요일에 검토합니다.\n표의 공백도  유지합니다.";

    @BeforeEach
    void setUp() {
        client = new MeetingIndexClient(new RestTemplateBuilder().additionalCustomizers(
                template -> server = MockRestServiceServer.bindTo(template).build()), "http://ai.test/");
    }

    @Test
    void 원문과식별자를전송하고색인결과를받는다() throws Exception {
        server.expect(requestTo("http://ai.test/index_meeting"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.minutesId").value(23))
                .andExpect(jsonPath("$.text").value(text))
                .andRespond(withSuccess(response(true), MediaType.APPLICATION_JSON));
        assertThat(client.index(1L, 23L, text).changed()).isTrue();
        server.verify();
    }

    @Test
    void 중복요청의변경없음도성공으로처리한다() throws Exception {
        server.expect(requestTo("http://ai.test/index_meeting"))
                .andRespond(withSuccess(response(false), MediaType.APPLICATION_JSON));
        assertThat(client.index(1L, 23L, text).changed()).isFalse();
        server.verify();
    }

    @Test
    void 서버오류를기존예외로변환하고재시도하지않는다() {
        server.expect(requestTo("http://ai.test/index_meeting"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertFailure();
        server.verify();
    }

    @Test
    void 시간초과를기존예외로변환한다() {
        server.expect(requestTo("http://ai.test/index_meeting"))
                .andRespond(withException(new SocketTimeoutException("응답 시간 초과")));
        assertFailure();
        server.verify();
    }

    @Test
    void 다른회의록의응답은성공으로처리하지않는다() throws Exception {
        server.expect(requestTo("http://ai.test/index_meeting"))
                .andRespond(withSuccess(response(true).replace("\"minutesId\":23", "\"minutesId\":24"), MediaType.APPLICATION_JSON));
        assertFailure();
        server.verify();
    }

    @Test
    void 다른원문의응답은성공으로처리하지않는다() throws Exception {
        server.expect(requestTo("http://ai.test/index_meeting"))
                .andRespond(withSuccess(response(true).replace(hash(), "다른 원문의 해시"), MediaType.APPLICATION_JSON));
        assertFailure();
        server.verify();
    }

    @Test
    void 비어있는응답은실패로처리한다() {
        server.expect(requestTo("http://ai.test/index_meeting"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertFailure();
        server.verify();
    }

    @Test
    void 잘못된요청은전송하지않는다() {
        assertThatThrownBy(() -> client.index(0L, 23L, text)).isInstanceOf(BaseException.class);
        assertThatThrownBy(() -> client.index(1L, 23L, " ")).isInstanceOf(BaseException.class);
        server.verify();
    }

    private void assertFailure() {
        assertThatThrownBy(() -> client.index(1L, 23L, text))
                .isInstanceOfSatisfying(BaseException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(MINUTES_FLASK_SERVER_COMMUNICATION_ERROR));
    }

    private String hash() throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private String response(boolean changed) throws Exception {
        return "{\"projectId\":1,\"minutesId\":23,\"changed\":" + changed
                + ",\"chunkCount\":1,\"sourceHash\":\"" + hash() + "\"}";
    }
}
