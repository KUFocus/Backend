package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.focus.logmeet.controller.dto.schedule.ScheduleCreateRequest;
import org.focus.logmeet.controller.dto.schedule.ScheduleCreateResponse;
import org.focus.logmeet.domain.Project;
import org.focus.logmeet.domain.Schedule;
import org.focus.logmeet.domain.enums.Status;
import org.focus.logmeet.repository.ProjectRepository;
import org.focus.logmeet.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.PROJECT_NOT_FOUND;
import static org.focus.logmeet.domain.enums.Status.ACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ProjectRepository projectRepository;
    public ScheduleCreateResponse createSchedule(ScheduleCreateRequest request) {
        log.info("스케줄 생성 시도: scheduleContent={}, scheduleDate={}", request.getScheduleContent(), request.getScheduleDate());
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        Schedule schedule = Schedule.builder()
                .content(request.getScheduleContent())
                .scheduleDate(request.getScheduleDate())
                .project(project)
                .status(ACTIVE)
                .build();

        scheduleRepository.save(schedule);
        log.info("스케줄 생성 성공: scheduleId={}", schedule.getId());

        return new ScheduleCreateResponse(schedule.getId());
    }
}
