package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.common.response.BaseExceptionResponseStatus;
import org.focus.logmeet.controller.dto.schedule.ScheduleCreateRequest;
import org.focus.logmeet.controller.dto.schedule.ScheduleCreateResponse;
import org.focus.logmeet.domain.Project;
import org.focus.logmeet.domain.Schedule;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.domain.UserProject;
import org.focus.logmeet.domain.enums.Role;
import org.focus.logmeet.domain.enums.Status;
import org.focus.logmeet.repository.ProjectRepository;
import org.focus.logmeet.repository.ScheduleRepository;
import org.focus.logmeet.repository.UserProjectRepository;
import org.focus.logmeet.repository.UserRepository;
import org.focus.logmeet.security.annotation.CurrentUser;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.*;
import static org.focus.logmeet.common.response.BaseExceptionResponseStatus.USER_NOT_IN_PROJECT;
import static org.focus.logmeet.domain.enums.Role.LEADER;
import static org.focus.logmeet.domain.enums.Status.ACTIVE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ProjectRepository projectRepository;
    private final UserProjectRepository userProjectRepository;
    public ScheduleCreateResponse createSchedule(ScheduleCreateRequest request) { //TODO: 스케줄 생성 시 프로젝트 유저 검증 필요
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

    @Transactional
    @CurrentUser
    public void deleteSchedule(Long scheduleId) {
        log.info("스케줄 삭제 시도: scheduleId={}", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BaseException(SCHEDULE_NOT_FOUND));
        UserProject leaderProject = validateUserAndProject(schedule.getProject().getId());

        if (!leaderProject.getRole().equals(LEADER)) {
            log.info("권한이 없는 삭제 시도: scheduleId={}, userId={}", scheduleId, leaderProject.getUser().getId());
            throw new BaseException(USER_NOT_LEADER);
        }

        scheduleRepository.delete(schedule);
    }

    private UserProject validateUserAndProject(Long projectId) {
        User currentUser = CurrentUserHolder.get();

        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        return userProjectRepository.findByUserAndProject(currentUser, project)
                .orElseThrow(() -> new BaseException(USER_NOT_IN_PROJECT));
    }
}
