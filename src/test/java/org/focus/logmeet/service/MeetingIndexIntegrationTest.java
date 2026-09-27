package org.focus.logmeet.service;

import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.domain.*;
import org.focus.logmeet.domain.enums.*;
import org.focus.logmeet.repository.*;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:meeting_index;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
        "flask.server.url=http://unused.test"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local-eval")
@Import({MinutesService.class, MeetingIndexListener.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MeetingIndexIntegrationTest {
    @Autowired private MinutesService minutesService;
    @MockBean private MeetingIndexClient indexClient;
    @Autowired private org.springframework.context.ApplicationEventPublisher publisher;
    @Autowired private UserRepository users;
    @Autowired private ProjectRepository projects;
    @Autowired private UserProjectRepository memberships;
    @Autowired private MinutesRepository minutes;
    @Autowired private PlatformTransactionManager transactions;
    @MockBean private RestTemplate model;
    @MockBean private S3Service storage;

    private User user;
    private Project project;
    private Long minutesId;

    @BeforeEach
    void setUp() {
        user = users.save(User.builder().email(UUID.randomUUID() + "@example.test")
                .name("검증 사용자").password("unused").build());
        project = projects.save(Project.builder().name("일정 저장 검증").build());
        memberships.save(UserProject.builder().user(user).project(project).role(Role.LEADER)
                .color(ProjectColor.PROJECT_1).build());
        minutesId = minutes.save(Minutes.builder().project(project).name("회의록")
                .content("2026년 9월 28일 오후 3시에 디자인 리뷰를 진행합니다.")
                .type(MinutesType.MANUAL).build()).getId();
    }

    @AfterEach
    void clearUser() {
        CurrentUserHolder.clear();
    }

    @Test
    void 커밋후에만원문을색인한다() {
        CurrentUserHolder.set(user);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            minutesService.saveAndUploadManualEntry("공백도  유지하는 원문", "직접 입력", project.getId());
            verifyNoInteractions(indexClient);
        });
        verify(indexClient).index(eq(project.getId()), anyLong(), eq("공백도  유지하는 원문"));
    }

    @Test
    void 롤백되면색인하지않는다() {
        CurrentUserHolder.set(user);
        long count = minutes.count();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            minutesService.saveAndUploadManualEntry("취소할 원문", "직접 입력", project.getId());
            status.setRollbackOnly();
        });
        verifyNoInteractions(indexClient);
        assertThat(minutes.count()).isEqualTo(count);
    }

    @Test
    void 색인실패시에도원문저장은성공한다() {
        CurrentUserHolder.set(user);
        long count = minutes.count();
        doThrow(new BaseException(MINUTES_FLASK_SERVER_COMMUNICATION_ERROR))
                .when(indexClient).index(anyLong(), anyLong(), anyString());
        assertThatCode(() -> minutesService.saveAndUploadManualEntry("보존할 원문", "직접 입력", project.getId()))
                .doesNotThrowAnyException();
        verify(indexClient).index(eq(project.getId()), anyLong(), eq("보존할 원문"));
        assertThat(minutes.count()).isEqualTo(count + 1);
    }

    @Test
    void 권한이없으면저장과색인을하지않는다() {
        CurrentUserHolder.clear();
        long count = minutes.count();
        assertThatThrownBy(() -> minutesService.saveAndUploadManualEntry("원문", "직접 입력", project.getId()))
                .isInstanceOf(BaseException.class);
        verifyNoInteractions(indexClient);
        assertThat(minutes.count()).isEqualTo(count);
    }

    @Test
    void 트랜잭션없는이벤트는색인하지않는다() {
        publisher.publishEvent(new MeetingIndexRequested(project.getId(), minutesId, "원문"));
        verifyNoInteractions(indexClient);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = MinutesType.class, names = {"VOICE", "PICTURE"})
    void 파일회의록확정후추출텍스트를색인한다(MinutesType type) {
        Long id = temporaryFile(type, "화자 없는 문장과 줄바꿈\n표 내용도  유지합니다.");
        CurrentUserHolder.set(user);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            minutesService.updateMinutesInfo(id, "확정 회의록", project.getId());
            verifyNoInteractions(indexClient);
        });
        verify(indexClient).index(project.getId(), id, "화자 없는 문장과 줄바꿈\n표 내용도  유지합니다.");
        Minutes saved = minutes.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(saved.getProject().getId()).isEqualTo(project.getId());
    }

    @Test
    void 파일회의록확정이롤백되면색인하지않는다() {
        Long id = temporaryFile(MinutesType.VOICE, "추출된 텍스트");
        CurrentUserHolder.set(user);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            minutesService.updateMinutesInfo(id, "취소할 확정", project.getId());
            status.setRollbackOnly();
        });
        verifyNoInteractions(indexClient);
        assertThat(minutes.findById(id).orElseThrow().getStatus()).isEqualTo(Status.TEMP);
    }

    @Test
    void 추출텍스트가없으면응답JSON을대신색인하지않는다() {
        Long id = temporaryFile(MinutesType.PICTURE, "  ");
        CurrentUserHolder.set(user);
        minutesService.updateMinutesInfo(id, "빈 추출 결과", project.getId());
        verifyNoInteractions(indexClient);
        assertThat(minutes.findById(id).orElseThrow().getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void 파일회의록확정재요청은색인을중복호출하지않는다() {
        Long id = temporaryFile(MinutesType.VOICE, "추출된 텍스트");
        CurrentUserHolder.set(user);
        minutesService.updateMinutesInfo(id, "첫 확정", project.getId());
        minutesService.updateMinutesInfo(id, "이름 수정", project.getId());
        verify(indexClient, times(1)).index(project.getId(), id, "추출된 텍스트");
        verifyNoMoreInteractions(indexClient);
    }

    @Test
    void 파일회의록색인실패에도확정상태를유지한다() {
        Long id = temporaryFile(MinutesType.PICTURE, "추출된 텍스트");
        CurrentUserHolder.set(user);
        doThrow(new BaseException(MINUTES_FLASK_SERVER_COMMUNICATION_ERROR))
                .when(indexClient).index(anyLong(), anyLong(), anyString());
        assertThatCode(() -> minutesService.updateMinutesInfo(id, "확정 회의록", project.getId()))
                .doesNotThrowAnyException();
        verify(indexClient).index(project.getId(), id, "추출된 텍스트");
        assertThat(minutes.findById(id).orElseThrow().getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void 프로젝트접근권한이없으면파일회의록을확정하거나색인하지않는다() {
        Long id = temporaryFile(MinutesType.VOICE, "추출된 텍스트");
        Project other = projects.save(Project.builder().name("참여하지 않은 프로젝트").build());
        CurrentUserHolder.set(user);
        assertThatThrownBy(() -> minutesService.updateMinutesInfo(id, "확정 요청", other.getId()))
                .isInstanceOfSatisfying(BaseException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(USER_NOT_IN_PROJECT));
        verifyNoInteractions(indexClient);
        assertThat(minutes.findById(id).orElseThrow().getStatus()).isEqualTo(Status.TEMP);
    }

    private Long temporaryFile(MinutesType type, String clearText) {
        Long id = minutes.save(Minutes.builder().type(type).status(Status.TEMP)
                .content("{\"text\":\"변환 서버 응답 JSON\"}").clearContent(clearText).build()).getId();
        verifyNoInteractions(indexClient);
        return id;
    }

    @Test
    void 회의록삭제커밋후에만색인을삭제한다() {
        CurrentUserHolder.set(user);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            minutesService.deleteMinutes(minutesId);
            verifyNoInteractions(indexClient);
        });
        verify(indexClient).delete(project.getId(), minutesId);
        assertThat(minutes.existsById(minutesId)).isFalse();
    }

    @Test
    void 회의록삭제롤백시색인도유지한다() {
        CurrentUserHolder.set(user);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            minutesService.deleteMinutes(minutesId);
            status.setRollbackOnly();
        });
        verifyNoInteractions(indexClient);
        assertThat(minutes.existsById(minutesId)).isTrue();
    }

    @Test
    void 색인삭제실패가확정된DB삭제를실패로응답하지않는다() {
        CurrentUserHolder.set(user);
        doThrow(new BaseException(MINUTES_FLASK_SERVER_COMMUNICATION_ERROR))
                .when(indexClient).delete(project.getId(), minutesId);
        assertThatCode(() -> minutesService.deleteMinutes(minutesId)).doesNotThrowAnyException();
        verify(indexClient).delete(project.getId(), minutesId);
        assertThat(minutes.existsById(minutesId)).isFalse();
    }

    @Test
    void 리더가아니면회의록과색인삭제를거부한다() {
        User member = users.save(User.builder().email(UUID.randomUUID() + "@example.test")
                .name("일반 회원").password("unused").build());
        memberships.save(UserProject.builder().user(member).project(project).role(Role.MEMBER)
                .color(ProjectColor.PROJECT_1).build());
        CurrentUserHolder.set(member);
        assertThatThrownBy(() -> minutesService.deleteMinutes(minutesId))
                .isInstanceOfSatisfying(BaseException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(USER_NOT_LEADER));
        verifyNoInteractions(indexClient);
        assertThat(minutes.existsById(minutesId)).isTrue();
    }
}
