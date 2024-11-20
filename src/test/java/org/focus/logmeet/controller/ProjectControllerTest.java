package org.focus.logmeet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.focus.logmeet.controller.dto.project.*;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.domain.enums.Role;
import org.focus.logmeet.service.ProjectService;
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

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private MockMvc mockMvc;
    private static ObjectMapper objectMapper;

    @InjectMocks
    private ProjectController projectController;

    @Mock
    private ProjectService projectService;

    @BeforeAll
    static void setupOnce() {
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectController).build();
    }

    @Test
    @DisplayName("새 프로젝트 생성 요청이 성공적으로 처리됨")
    void createProject() throws Exception {
        // given
        ProjectCreateRequest request = new ProjectCreateRequest("New Project", "Description", ProjectColor.PROJECT_6);
        ProjectCreateResponse response = new ProjectCreateResponse(1L);
        when(projectService.createProject(any(ProjectCreateRequest.class))).thenReturn(response);

        // when
        MvcResult result = mockMvc.perform(post("/projects/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(content);
        long projectId = jsonNode.path("result").path("projectId").asLong();

        assertThat(status).isEqualTo(200);
        assertThat(projectId).isEqualTo(1L);
    }

    @Test
    @DisplayName("특정 프로젝트 상세 정보 조회가 성공적으로 처리됨")
    void getProject() throws Exception {
        // given
        UserProjectDto userProjectDto = new UserProjectDto(1L, 100L, "John Doe", Role.MEMBER, true, ProjectColor.PROJECT_6);
        ProjectInfoResult response = new ProjectInfoResult(1L, "Project Name", "Description", LocalDateTime.now(), Collections.singletonList(userProjectDto));
        when(projectService.getProject(1L)).thenReturn(response);

        // when
        MvcResult result = mockMvc.perform(get("/projects/1"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(content);
        long projectId = jsonNode.path("result").path("projectId").asLong();

        assertThat(status).isEqualTo(200);
        assertThat(projectId).isEqualTo(1L);
    }

    @Test
    @DisplayName("사용자의 프로젝트 목록 조회가 성공적으로 처리됨")
    void getProjectList() throws Exception {
        // given
        ProjectListResult projectListResult = new ProjectListResult(1L, "Project Name", Role.LEADER, true, ProjectColor.PROJECT_3, 5, LocalDateTime.now());
        when(projectService.getProjectList()).thenReturn(Collections.singletonList(projectListResult));

        // when
        MvcResult result = mockMvc.perform(get("/projects/project-list"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(content);
        long projectId = jsonNode.path("result").get(0).path("projectId").asLong();

        assertThat(status).isEqualTo(200);
        assertThat(projectId).isEqualTo(1L);
    }

    @Test
    @DisplayName("프로젝트 정보 수정 요청이 성공적으로 처리됨")
    void updateProject() throws Exception {
        // given
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("Updated Name");
        request.setContent("Updated Description");
        request.setColor(ProjectColor.PROJECT_4);

        // when
        MvcResult result = mockMvc.perform(put("/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())  // 상태 코드 200 확인
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("\"success\":true");  // 성공 여부가 true인지 확인
    }


    @Test
    @DisplayName("프로젝트 즐겨찾기 토글 요청이 성공적으로 처리됨")
    void bookmarkProjectToggle() throws Exception {
        // given
        ProjectBookmarkResult response = new ProjectBookmarkResult(true);
        when(projectService.bookmarkProjectToggle(1L)).thenReturn(response);

        // when
        MvcResult result = mockMvc.perform(put("/projects/1/bookmark"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(content);
        boolean bookmark = jsonNode.path("result").path("bookmark").asBoolean();

        assertThat(status).isEqualTo(200);
        assertThat(bookmark).isTrue();
    }

    @Test
    @DisplayName("프로젝트 삭제 요청이 성공적으로 처리됨")
    void deleteProject() throws Exception {
        // when
        MvcResult result = mockMvc.perform(delete("/projects/1"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);
        String content = result.getResponse().getContentAsString();

        assertThat(content).contains("\"success\":true");  // 성공 여부만 검증
    }


    @Test
    @DisplayName("프로젝트 리더 임명 요청이 성공적으로 처리됨")
    void delegateLeader() throws Exception {
        // given
        ProjectLeaderDelegationRequest request = new ProjectLeaderDelegationRequest(116L);
        ProjectLeaderDelegationResponse response = new ProjectLeaderDelegationResponse(1L, 116L);
        when(projectService.delegateLeader(1L, 116L)).thenReturn(response);

        // when
        MvcResult result = mockMvc.perform(put("/projects/1/leader")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(content);
        long newLeaderId = jsonNode.path("result").path("newLeaderId").asLong();

        assertThat(status).isEqualTo(200);
        assertThat(newLeaderId).isEqualTo(116L);
    }

    @Test
    @DisplayName("프로젝트 북마크 리스트 조회가 성공적으로 처리됨")
    void getProjectBookmarkList() throws Exception {
        // given
        ProjectListResult bookmarkProject = new ProjectListResult(1L, "Project Name", Role.LEADER, true, ProjectColor.PROJECT_3, 5, LocalDateTime.now());
        when(projectService.getProjectBookmarkList()).thenReturn(Collections.singletonList(bookmarkProject));

        // when
        MvcResult result = mockMvc.perform(get("/projects/bookmark-list"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(content);
        long projectId = jsonNode.path("result").get(0).path("projectId").asLong();

        assertThat(status).isEqualTo(200);
        assertThat(projectId).isEqualTo(1L);
    }

    @Test
    @DisplayName("참가자 추방 요청이 성공적으로 처리됨")
    void expelMember() throws Exception {
        // when
        MvcResult result = mockMvc.perform(delete("/projects/expel")
                        .param("projectId", "1")
                        .param("userId", "100"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);
    }

    @Test
    @DisplayName("프로젝트 나가기 요청이 성공적으로 처리됨")
    void leaveProject() throws Exception {
        // when
        MvcResult result = mockMvc.perform(delete("/projects/1/leave"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);
    }

    @Test
    @DisplayName("프로젝트 초대 코드 생성 요청이 성공적으로 처리됨")
    void getInviteCode() throws Exception {
        // given
        ProjectInviteCodeResult projectInviteCodeResult = new ProjectInviteCodeResult(1L, "12345678", LocalDateTime.now());
        when(projectService.getInviteCode(1L)).thenReturn(projectInviteCodeResult);

        // when
        MvcResult result = mockMvc.perform(get("/projects/1/invite-code"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        String content = result.getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(content);
        String code = jsonNode.path("result").path("inviteCode").asText();

        assertThat(status).isEqualTo(200);
        assertThat(code).isEqualTo("12345678");
    }

    @Test
    @DisplayName("프로젝트 초대 코드로 참여 요청이 성공적으로 처리됨")
    void joinProject() throws Exception {
        // given
        ProjectJoinRequest request = new ProjectJoinRequest("ABCDEFGH");

        // when
        MvcResult result = mockMvc.perform(post("/projects/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // then
        int status = result.getResponse().getStatus();
        assertThat(status).isEqualTo(200);
    }
}
