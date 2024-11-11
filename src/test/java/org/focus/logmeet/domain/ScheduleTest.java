package org.focus.logmeet.domain;

import org.focus.logmeet.domain.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleTest {

    private Schedule schedule;

    @BeforeEach
    void setUp() {
        Project project = new Project();
        project.setName("테스트 프로젝트");

        schedule = Schedule.builder()
                .content("테스트 일정")
                .scheduleDate(LocalDateTime.now())
                .project(project)
                .build();
    }

    @Test
    @DisplayName("Project 필드 설정 테스트")
    void testProjectField() {
        assertNotNull(schedule.getProject());
        assertEquals("테스트 프로젝트", schedule.getProject().getName());
    }

    @Test
    @DisplayName("scheduleDate 필드 설정 테스트")
    void testScheduleDateField() {
        assertNotNull(schedule.getScheduleDate());
        assertTrue(schedule.getScheduleDate().isBefore(LocalDateTime.now().plusDays(1)));
    }

    @Test
    @DisplayName("content 필드 설정 테스트")
    void testContentField() {
        assertNotNull(schedule.getContent());
        assertEquals("테스트 일정", schedule.getContent());
    }

    @Test
    @DisplayName("status 필드 기본값 검증")
    void testStatusDefaultValue() {
        assertNotNull(schedule.getStatus());
        assertEquals(Status.ACTIVE, schedule.getStatus());
    }

    @Test
    @DisplayName("status 필드 수정 테스트")
    void testStatusFieldUpdate() {
        schedule.setStatus(Status.INACTIVE);
        assertEquals(Status.INACTIVE, schedule.getStatus());
    }
}
