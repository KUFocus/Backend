package org.focus.logmeet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.focus.logmeet.controller.dto.minutes.*;
import org.focus.logmeet.domain.enums.MinutesType;
import org.focus.logmeet.domain.enums.Status;
import org.focus.logmeet.service.MinutesService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.focus.logmeet.domain.enums.ProjectColor.PROJECT_6;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MinutesControllerTest {

    private MockMvc mockMvc;
    private static ObjectMapper objectMapper;

    @InjectMocks
    private MinutesController minutesController;

    @Mock
    private MinutesService minutesService;

    @BeforeAll
    static void setupOnce() {
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(minutesController).build();
    }

    @Test
    @DisplayName("업데이트된 회의록 정보가 성공적으로 반환됨")
    void updateMinutesInfo() throws Exception {
        // given
        MinutesInfoCreateRequest request = new MinutesInfoCreateRequest(1L, "Updated Minutes", 1L);
        MinutesCreateResponse response = new MinutesCreateResponse(1L, 1L);
        when(minutesService.updateMinutesInfo(any(Long.class), any(String.class), any(Long.class)))
                .thenReturn(response);

        // when
        MvcResult result = mockMvc.perform(put("/minutes/update-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }

    @Test
    @DisplayName("파일 업로드 요청이 성공적으로 처리됨")
    void uploadFile() throws Exception {
        // given
        MinutesFileUploadRequest request = new MinutesFileUploadRequest("base64data", "testfile.mp3", MinutesType.VOICE);
        MinutesFileUploadResponse response = new MinutesFileUploadResponse("file/path", MinutesType.VOICE);
        when(minutesService.uploadFile(any(String.class), any(String.class), any(MinutesType.class)))
                .thenReturn(response);

        // when
        MvcResult result = mockMvc.perform(post("/minutes/upload-file")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }

    @Test
    @DisplayName("텍스트 요약 요청이 성공적으로 처리됨")
    void summarizeText() throws Exception {
        // given
        MinutesSummarizeResult response = new MinutesSummarizeResult("Summarized text", Collections.emptyList());
        when(minutesService.summarizeText(any(Long.class))).thenReturn(response);

        // when
        MvcResult result = mockMvc.perform(post("/minutes/1/summarize-text"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }

    @Test
    @DisplayName("수동으로 작성된 회의록 업로드 요청이 성공적으로 처리됨")
    void uploadManualEntry() throws Exception {
        // given
        MinutesManuallyCreateRequest request = new MinutesManuallyCreateRequest("Manual Minutes", "Text content", 1L);
        MinutesCreateResponse response = new MinutesCreateResponse(1L, 1L);
        when(minutesService.saveAndUploadManualEntry(any(String.class), any(String.class), any(Long.class)))
                .thenReturn(response);

        // when
        MvcResult result = mockMvc.perform(post("/minutes/upload-content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }

    @Test
    @DisplayName("회의록 정보 조회가 성공적으로 처리됨")
    void getMinutes() throws Exception {
        // given
        MinutesInfoResult response = new MinutesInfoResult(1L, 1L, "Minutes Name", "Content", "file/path", null);
        when(minutesService.getMinutes(any(Long.class))).thenReturn(response);

        // when
        MvcResult result = mockMvc.perform(get("/minutes/1"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }

    @Test
    @DisplayName("현재 유저의 회의록 리스트 조회가 성공적으로 처리됨")
    void getMinutesList() throws Exception {
        // given
        MinutesListResult minutesListResult = new MinutesListResult(1L, 1L, "Minutes Name", PROJECT_6, MinutesType.VOICE, Status.ACTIVE, LocalDateTime.now());
        when(minutesService.getMinutesList()).thenReturn(Collections.singletonList(minutesListResult));

        // when
        MvcResult result = mockMvc.perform(get("/minutes/minutes-list"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }

    @Test
    @DisplayName("특정 프로젝트의 회의록 리스트 조회가 성공적으로 처리됨")
    void getProjectMinutes() throws Exception {
        // given
        MinutesListResult minutesListResult = new MinutesListResult(1L, 1L, "Minutes Name", PROJECT_6, MinutesType.VOICE, Status.ACTIVE, LocalDateTime.now());
        when(minutesService.getProjectMinutes(any(Long.class))).thenReturn(Collections.singletonList(minutesListResult));

        // when
        MvcResult result = mockMvc.perform(get("/minutes/1/minutes-list"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }


    @Test
    @DisplayName("회의록 삭제 요청이 성공적으로 처리됨")
    void deleteMinutes() throws Exception {
        // when
        MvcResult result = mockMvc.perform(delete("/minutes/1"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }
}
