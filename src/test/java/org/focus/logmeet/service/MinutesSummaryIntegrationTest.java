package org.focus.logmeet.service;

import org.focus.logmeet.controller.dto.minutes.MinutesSummarizeResult;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.schedule.ScheduleDto;
import org.focus.logmeet.domain.*;
import org.focus.logmeet.domain.enums.MinutesType;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.domain.enums.Role;
import org.focus.logmeet.repository.*;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:summary;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
        "flask.server.url=http://unused.test"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("local-eval")
@Import({MinutesService.class, ScheduleService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MinutesSummaryIntegrationTest {
    @Autowired private MinutesService minutesService;
    @Autowired private ScheduleService scheduleService;
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
        when(model.postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class)))
                .thenReturn(ResponseEntity.ok(response()));
    }

    private MinutesSummarizeResult response() {
        return new MinutesSummarizeResult("디자인 검토 방안을 논의했습니다.",
                List.of(new ScheduleDto("2026-09-28T15:00:00", "디자인 리뷰")));
    }

    private MinutesSummarizeResult summarize() {
        CurrentUserHolder.set(user);
        try {
            return minutesService.summarizeText(minutesId);
        } finally {
            CurrentUserHolder.clear();
        }
    }

    private MinutesSummarizeResult summarize(String requestKey) {
        CurrentUserHolder.set(user);
        try {
            return minutesService.summarizeText(minutesId, requestKey);
        } finally {
            CurrentUserHolder.clear();
        }
    }

    private int scheduleCount() {
        CurrentUserHolder.set(user);
        try {
            return new TransactionTemplate(transactions).execute(status ->
                    scheduleService.getScheduleOfProjectAt(project.getId(), LocalDate.of(2026, 9, 28)).size());
        } finally {
            CurrentUserHolder.clear();
        }
    }

    @Test
    @DisplayName("같은 회의록의 요약을 반복 요청해도 일정과 모델 호출은 한 번만 발생한다")
    void repeatedSummarySavesOnce() {
        MinutesSummarizeResult first = summarize();
        MinutesSummarizeResult second = summarize();

        assertThat(scheduleCount()).isEqualTo(1);
        assertThat(second).usingRecursiveComparison().isEqualTo(first);
        verify(model, times(1)).postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class));
    }

    @Test
    @DisplayName("같은 요청 키에 회의록 내용이 달라지면 기존 결과를 잘못 재사용하거나 추가 저장하지 않는다")
    void changedInputCannotReuseRequestKey() {
        summarize("request-1");
        Minutes edited = minutes.findById(minutesId).orElseThrow();
        edited.setContent("일정을 변경했습니다.");
        minutes.save(edited);

        assertThatThrownBy(() -> summarize("request-1")).isInstanceOfSatisfying(BaseException.class,
                error -> assertThat(error.getStatus()).isEqualTo(MINUTES_SUMMARY_REQUEST_CONFLICT));
        assertThat(scheduleCount()).isEqualTo(1);
        verify(model, times(1)).postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class));
    }

    @Test
    @DisplayName("같은 요청 키를 재전송하면 최초 응답을 반환하고 일정을 추가하지 않는다")
    void explicitRequestKeyReplaysResult() {
        MinutesSummarizeResult first = summarize("request-1");
        assertThat(summarize("request-1")).usingRecursiveComparison().isEqualTo(first);
        assertThat(scheduleCount()).isEqualTo(1);
        verify(model, times(1)).postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class));
    }

    @Test
    @DisplayName("같은 요청을 동시에 여덟 번 보내도 모델 호출과 일정 저장은 한 번만 발생한다")
    void concurrentRequestsSaveOnce() throws Exception {
        when(model.postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(250);
                    return ResponseEntity.ok(response());
                });
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MinutesSummarizeResult>> results = new ArrayList<>();
        try {
            for (int i = 0; i < 8; i++) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("동시 요청 시작 시간 초과");
                    }
                    return summarize("parallel-request");
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<MinutesSummarizeResult> result : results) {
                assertThat(result.get(10, TimeUnit.SECONDS)).usingRecursiveComparison().isEqualTo(response());
            }
        } finally {
            start.countDown();
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
        assertThat(scheduleCount()).isEqualTo(1);
        verify(model, times(1)).postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class));
    }

    @Test
    @DisplayName("일부 일정을 저장하다 실패하면 전체를 롤백하고 같은 키로 재시도할 수 있다")
    void partialFailureRollsBackAndCanRetry() {
        MinutesSummarizeResult invalid = new MinutesSummarizeResult("실패할 요약", List.of(
                new ScheduleDto("2026-09-28T15:00:00", "디자인 리뷰"),
                new ScheduleDto("잘못된 날짜", "두 번째 일정")));
        when(model.postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class)))
                .thenReturn(ResponseEntity.ok(invalid), ResponseEntity.ok(response()));

        assertThatThrownBy(() -> summarize("retry-request")).isInstanceOfSatisfying(BaseException.class,
                error -> assertThat(error.getStatus()).isEqualTo(SCHEDULE_DATE_FORMAT_INVALID));
        assertThat(scheduleCount()).isZero();
        assertThat(minutes.findById(minutesId).orElseThrow().getSummary()).isNull();

        summarize("retry-request");
        summarize("retry-request");
        assertThat(scheduleCount()).isEqualTo(1);
        verify(model, times(2)).postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class));
    }

    @Test
    @DisplayName("모델 통신 실패 후에도 같은 요청 키로 다시 처리할 수 있다")
    void modelFailureCanRetry() {
        when(model.postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class)))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("검증용 연결 실패"))
                .thenReturn(ResponseEntity.ok(response()));
        assertThatThrownBy(() -> summarize("retry-request")).isInstanceOf(BaseException.class);
        assertThat(scheduleCount()).isZero();
        summarize("retry-request");
        assertThat(scheduleCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 회의록은 같은 요청 키와 일정 내용이어도 별도 작업으로 처리한다")
    void anotherMinutesDoesNotReuseResult() {
        summarize("shared-key");
        minutesId = minutes.save(Minutes.builder().project(project).name("다른 회의록")
                .content("2026년 9월 28일 오후 3시에 디자인 리뷰를 진행합니다.")
                .type(MinutesType.MANUAL).build()).getId();
        summarize("shared-key");
        assertThat(scheduleCount()).isEqualTo(2);
        verify(model, times(2)).postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class));
    }

    @Test
    @DisplayName("원문이 변경되면 자동 생성한 요청 키로 새 작업을 처리한다")
    void changedContentCreatesNewAutomaticRequest() {
        summarize();
        Minutes edited = minutes.findById(minutesId).orElseThrow();
        edited.setContent("디자인 리뷰의 진행 방식을 변경했습니다.");
        minutes.save(edited);
        summarize();
        assertThat(scheduleCount()).isEqualTo(2);
        verify(model, times(2)).postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class));
    }

    @Test
    @DisplayName("최초 처리 이후 권한이 사라진 사용자는 저장된 결과도 조회할 수 없다")
    void replayChecksCurrentPermission() {
        summarize("private-request");
        memberships.delete(memberships.findByUserAndProject(user, project).orElseThrow());
        assertThatThrownBy(() -> summarize("private-request")).isInstanceOfSatisfying(BaseException.class,
                error -> assertThat(error.getStatus()).isEqualTo(USER_NOT_IN_PROJECT));
        verify(model, times(1)).postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class));
    }

    @Test
    @DisplayName("잘못된 요청 키는 모델을 호출하기 전에 거부한다")
    void invalidRequestKeysAreRejected() {
        for (String key : List.of("", " ", "a".repeat(129), "한글키")) {
            assertThatThrownBy(() -> summarize(key)).isInstanceOfSatisfying(BaseException.class,
                    error -> assertThat(error.getStatus()).isEqualTo(INVALID_INPUT_VALUE));
        }
        verifyNoInteractions(model);
    }

    @Test
    @DisplayName("일정이 없는 정상 응답도 재사용해 불필요한 모델 호출을 막는다")
    void emptySchedulesAreACompletedResult() {
        when(model.postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class)))
                .thenReturn(ResponseEntity.ok(new MinutesSummarizeResult("일정 없음", List.of())));
        summarize();
        assertThat(summarize().getSchedules()).isEmpty();
        assertThat(scheduleCount()).isZero();
        verify(model, times(1)).postForEntity(any(URI.class), any(HttpEntity.class), eq(MinutesSummarizeResult.class));
    }
}
