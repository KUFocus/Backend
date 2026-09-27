package org.focus.logmeet.service;

import org.focus.logmeet.common.exception.GlobalExceptionHandler;
import org.focus.logmeet.controller.MeetingAnswerController;
import org.focus.logmeet.domain.*;
import org.focus.logmeet.domain.enums.Status;
import org.focus.logmeet.repository.*;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.junit.jupiter.api.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class MeetingAnswerServiceTest {
    private MockRestServiceServer server;
    private MockMvc mvc;
    private ProjectRepository projects;
    private UserProjectRepository memberships;
    private MinutesRepository minutes;
    private User user;
    private Project project;
    private Minutes meeting;
    private final String request = "{\"question\":\"검토 회의는 언제인가요?\",\"minutesId\":3}";

    @BeforeEach
    void setUp() {
        MeetingAnswerClient client = new MeetingAnswerClient(new RestTemplateBuilder().additionalCustomizers(
                template -> server = MockRestServiceServer.bindTo(template).build()), "http://ai.test/");
        projects = mock(ProjectRepository.class);
        memberships = mock(UserProjectRepository.class);
        minutes = mock(MinutesRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new MeetingAnswerController(
                new MeetingAnswerService(projects, memberships, minutes, client)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        user = User.builder().id(1L).build();
        project = Project.builder().id(2L).build();
        meeting = Minutes.builder().id(3L).project(project).status(Status.ACTIVE).build();
        CurrentUserHolder.set(user);
        when(projects.findById(2L)).thenReturn(Optional.of(project));
        when(memberships.findByUserAndProject(user, project)).thenReturn(Optional.of(UserProject.builder().build()));
        when(minutes.findById(3L)).thenReturn(Optional.of(meeting));
    }

    @AfterEach
    void cleanUp() {
        CurrentUserHolder.clear();
        server.verify();
    }

    private String answer() {
        return "{\"status\":\"answered\",\"answer\":\"금요일입니다.\",\"claims\":[{\"text\":\"금요일입니다.\",\"citations\":["
                + "{\"project_id\":2,\"minutes_id\":3,\"source_hash\":\"" + "a".repeat(64)
                + "\",\"quote\":\"금요일입니다.\",\"start\":0,\"end\":7}]}]}";
    }

    @Test
    void 권한검사후질문을전송하고근거를반환한다() throws Exception {
        server.expect(requestTo("http://ai.test/answer_meeting"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"projectId\":2,\"minutesId\":3,\"question\":\"검토 회의는 언제인가요?\"}", true))
                .andRespond(withSuccess(answer(), MediaType.APPLICATION_JSON));
        mvc.perform(post("/projects/2/questions").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("answered"))
                .andExpect(jsonPath("$.result.claims[0].citations[0].minutes_id").value(3));
    }

    @Test
    void 프로젝트전체검색과근거부족응답을지원한다() throws Exception {
        server.expect(requestTo("http://ai.test/answer_meeting"))
                .andExpect(content().json("{\"projectId\":2,\"minutesId\":null,\"question\":\"예산은?\"}", true))
                .andRespond(withSuccess("{\"status\":\"insufficient_evidence\",\"answer\":\"근거가 없습니다.\",\"claims\":[]}", MediaType.APPLICATION_JSON));
        mvc.perform(post("/projects/2/questions").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"예산은?\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("insufficient_evidence"));
    }

    @Test
    void 미인증사용자는모델을호출하지않는다() throws Exception {
        CurrentUserHolder.clear();
        mvc.perform(post("/projects/2/questions").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 프로젝트회원이아니면모델을호출하지않는다() throws Exception {
        when(memberships.findByUserAndProject(user, project)).thenReturn(Optional.empty());
        mvc.perform(post("/projects/2/questions").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void 다른프로젝트회의록은요청할수없다() throws Exception {
        meeting.setProject(Project.builder().id(9L).build());
        mvc.perform(post("/projects/2/questions").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNotFound());
    }

    @Test
    void 임시회의록은요청할수없다() throws Exception {
        meeting.setStatus(Status.TEMP);
        mvc.perform(post("/projects/2/questions").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNotFound());
    }

    @Test
    void 잘못된질문은모델을호출하지않는다() throws Exception {
        for (String question : new String[]{" ", "가".repeat(2001)}) {
            mvc.perform(post("/projects/2/questions").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"question\":\"" + question + "\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void 다른프로젝트인용을반환하면오류로처리한다() throws Exception {
        server.expect(requestTo("http://ai.test/answer_meeting"))
                .andRespond(withSuccess(answer().replace("\"project_id\":2", "\"project_id\":9"), MediaType.APPLICATION_JSON));
        mvc.perform(post("/projects/2/questions").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(6001));
    }

    @Test
    void 삭제된회의록이인용되면답변을전달하지않는다() throws Exception {
        when(minutes.findById(3L)).thenReturn(Optional.empty());
        server.expect(requestTo("http://ai.test/answer_meeting"))
                .andRespond(withSuccess(answer(), MediaType.APPLICATION_JSON));
        mvc.perform(post("/projects/2/questions").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"언제인가요?\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 서버실패를근거부족으로바꾸지않는다() throws Exception {
        server.expect(requestTo("http://ai.test/answer_meeting"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        mvc.perform(post("/projects/2/questions").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(6001));
    }
}
