package org.focus.logmeet.service;

import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.common.exception.GlobalExceptionHandler;
import org.focus.logmeet.controller.MeetingIndexController;
import org.focus.logmeet.domain.*;
import org.focus.logmeet.domain.enums.*;
import org.focus.logmeet.repository.*;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;

class MeetingIndexServiceTest {
    private MinutesRepository repository;
    private UserProjectRepository memberships;
    private MeetingIndexClient client;
    private MockMvc mvc;
    private User user;
    private Project project;
    private Minutes minutes;

    @BeforeEach
    void setUp() {
        repository = mock(MinutesRepository.class);
        memberships = mock(UserProjectRepository.class);
        client = mock(MeetingIndexClient.class);
        mvc = MockMvcBuilders.standaloneSetup(new MeetingIndexController(
                new MeetingIndexService(repository, memberships, client)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        user = User.builder().id(1L).build();
        project = Project.builder().id(2L).build();
        minutes = Minutes.builder().id(3L).project(project).type(MinutesType.MANUAL)
                .content("원문은  그대로 보냅니다.").build();
        CurrentUserHolder.set(user);
        when(repository.findById(3L)).thenReturn(Optional.of(minutes));
        when(memberships.findByUserAndProject(user, project)).thenReturn(Optional.of(UserProject.builder().user(user).project(project).build()));
    }

    @AfterEach
    void clearUser() {
        CurrentUserHolder.clear();
    }

    @Test
    void 저장된원문으로재요청하고변경없음도성공응답한다() throws Exception {
        when(client.index(2L, 3L, minutes.getContent()))
                .thenReturn(new MeetingIndexClient.IndexResult(2L, 3L, false, 1, "해시"));
        mvc.perform(post("/minutes/3/index"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.changed").value(false));
        verify(client).index(2L, 3L, minutes.getContent());
    }

    @ParameterizedTest
    @EnumSource(value = MinutesType.class, names = {"VOICE", "PICTURE"})
    void 파일은응답JSON이아닌추출텍스트를보낸다(MinutesType type) throws Exception {
        minutes.setType(type);
        minutes.setContent("{\"text\":\"응답 JSON\"}");
        minutes.setClearContent("추출된  텍스트");
        when(client.index(2L, 3L, "추출된  텍스트"))
                .thenReturn(new MeetingIndexClient.IndexResult(2L, 3L, true, 1, "해시"));
        mvc.perform(post("/minutes/3/index")).andExpect(status().isOk());
        verify(client).index(2L, 3L, "추출된  텍스트");
    }

    @Test
    void 미인증요청은거부한다() throws Exception {
        CurrentUserHolder.clear();
        mvc.perform(post("/minutes/3/index")).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository, client);
    }

    @Test
    void 다른프로젝트회원은거부한다() throws Exception {
        when(memberships.findByUserAndProject(user, project)).thenReturn(Optional.empty());
        mvc.perform(post("/minutes/3/index")).andExpect(status().isForbidden());
        verifyNoInteractions(client);
    }

    @Test
    void 임시회의록은거부한다() throws Exception {
        minutes.setStatus(Status.TEMP);
        minutes.setProject(null);
        mvc.perform(post("/minutes/3/index")).andExpect(status().isBadRequest());
        verifyNoInteractions(client);
    }

    @Test
    void 없는회의록은찾을수없음으로응답한다() throws Exception {
        when(repository.findById(3L)).thenReturn(Optional.empty());
        mvc.perform(post("/minutes/3/index")).andExpect(status().isNotFound());
        verifyNoInteractions(client);
    }

    @Test
    void 추출텍스트가없으면호출하지않는다() throws Exception {
        minutes.setType(MinutesType.PICTURE);
        mvc.perform(post("/minutes/3/index")).andExpect(status().isBadRequest());
        verifyNoInteractions(client);
    }

    @Test
    void 색인실패는클라이언트에게기존오류형식으로전달한다() throws Exception {
        when(client.index(2L, 3L, minutes.getContent()))
                .thenThrow(new BaseException(MINUTES_FLASK_SERVER_COMMUNICATION_ERROR));
        mvc.perform(post("/minutes/3/index"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(6001));
    }
}
