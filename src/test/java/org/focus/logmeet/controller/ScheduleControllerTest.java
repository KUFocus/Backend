package org.focus.logmeet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.focus.logmeet.controller.dto.schedule.*;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.service.ScheduleService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    private MockMvc mockMvc;
    private static ObjectMapper objectMapper;

    @InjectMocks
    private ScheduleController scheduleController;

    @Mock
    private ScheduleService scheduleService;

    @BeforeAll
    static void setupOnce() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(scheduleController).build();
    }

    @Test
    @DisplayName("새 스케줄 생성 요청이 성공적으로 처리됨")
    void createSchedule() throws Exception {
        // given
        ScheduleCreateRequest request = new ScheduleCreateRequest("졸프 6주차 활동지 제출", LocalDateTime.now(), 116L);
        ScheduleCreateResponse response = new ScheduleCreateResponse(1L);
        when(scheduleService.createSchedule(any(ScheduleCreateRequest.class))).thenReturn(response);

        // when
        MvcResult result = mockMvc.perform(post("/schedule/new")
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
    @DisplayName("스케줄 수정 요청이 성공적으로 처리됨")
    void updateSchedule() throws Exception {
        // given
        ScheduleUpdateRequest request = new ScheduleUpdateRequest("졸프 6주차 활동지 수정", LocalDateTime.now());

        // when
        MvcResult result = mockMvc.perform(put("/schedule/1")
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
    @DisplayName("스케줄 정보 조회가 성공적으로 처리됨")
    void getSchedule() throws Exception {
        // given
        ScheduleInfoResult response = new ScheduleInfoResult(116L, "Project Name", "졸프 6주차 활동지 제출", LocalDateTime.now(), ProjectColor.PROJECT_3);
        when(scheduleService.getSchedule(1L)).thenReturn(response);

        // when
        MvcResult result = mockMvc.perform(get("/schedule/1"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }

    @Test
    @DisplayName("프로젝트의 월별 스케줄 조회가 성공적으로 처리됨")
    void getScheduleOfProject() throws Exception {
        // given
        ScheduleMonthlyListResult scheduleMonthlyListResult = new ScheduleMonthlyListResult(LocalDateTime.now().getDayOfMonth(), Collections.singleton(ProjectColor.PROJECT_3));
        when(scheduleService.getScheduleOfProject(any(Long.class), any(LocalDate.class))).thenReturn(Collections.singletonList(scheduleMonthlyListResult));

        // when
        MvcResult result = mockMvc.perform(get("/schedule/1/schedule-list")
                        .param("yearMonth", "2024-11-06"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }

    @Test
    @DisplayName("프로젝트의 특정 날짜의 스케줄 조회가 성공적으로 처리됨")
    void getScheduleOfProjectAt() throws Exception {
        // given
        ScheduleListResult scheduleListResult = new ScheduleListResult(1L, "Project Name", "졸프 6주차 활동지 제출", LocalDateTime.now(), ProjectColor.PROJECT_3);
        when(scheduleService.getScheduleOfProjectAt(any(Long.class), any(LocalDate.class))).thenReturn(Collections.singletonList(scheduleListResult));

        // when
        MvcResult result = mockMvc.perform(get("/schedule/1/schedules")
                        .param("date", "2024-11-06"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }

    @Test
    @DisplayName("사용자의 월별 스케줄 조회가 성공적으로 처리됨")
    void getScheduleOfUser() throws Exception {
        // given
        ScheduleMonthlyListResult scheduleMonthlyListResult = new ScheduleMonthlyListResult(LocalDateTime.now().getDayOfMonth(), Collections.singleton(ProjectColor.PROJECT_3));
        when(scheduleService.getScheduleOfUser(any(LocalDate.class))).thenReturn(Collections.singletonList(scheduleMonthlyListResult));

        // when
        MvcResult result = mockMvc.perform(get("/schedule/users/schedule-list")
                        .param("yearMonth", "2024-11-06"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }

    @Test
    @DisplayName("사용자의 특정 날짜의 스케줄 조회가 성공적으로 처리됨")
    void getScheduleOfUserAt() throws Exception {
        // given
        ScheduleListResult scheduleListResult = new ScheduleListResult(1L, "Project Name", "졸프 6주차 활동지 제출", LocalDateTime.now(), ProjectColor.PROJECT_3);
        when(scheduleService.getScheduleOfUserAt(any(LocalDate.class))).thenReturn(Collections.singletonList(scheduleListResult));

        // when
        MvcResult result = mockMvc.perform(get("/schedule/users/schedules")
                        .param("date", "2024-11-06"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }

    @Test
    @DisplayName("스케줄 삭제 요청이 성공적으로 처리됨")
    void deleteSchedule() throws Exception {
        // when
        MvcResult result = mockMvc.perform(delete("/schedule/1"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");
    }

    @Test
    @DisplayName("ScheduleDto 생성자 및 게터 테스트")
    void testScheduleDto() {
        // given
        ScheduleDto scheduleDto = new ScheduleDto("2024-11-06", "졸프 6주차 활동지 제출");

        // then
        assertThat(scheduleDto.getExtractedScheduleDate()).isEqualTo("2024-11-06");
        assertThat(scheduleDto.getExtractedScheduleContent()).isEqualTo("졸프 6주차 활동지 제출");
    }

}
