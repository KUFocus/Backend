package org.focus.logmeet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.focus.logmeet.common.exception.BaseException;
import org.focus.logmeet.controller.dto.schedule.*;
import org.focus.logmeet.domain.Project;
import org.focus.logmeet.domain.Schedule;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.domain.UserProject;
import org.focus.logmeet.repository.ProjectRepository;
import org.focus.logmeet.repository.ScheduleRepository;
import org.focus.logmeet.repository.UserProjectRepository;
import org.focus.logmeet.security.annotation.CurrentUser;
import org.focus.logmeet.security.aspect.CurrentUserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

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
    public void updateSchedule(Long scheduleId, ScheduleUpdateRequest request) {
        log.info("스케줄 수정 시도: scheduleId={}", scheduleId);
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BaseException(SCHEDULE_NOT_FOUND));
        UserProject leaderProject = validateUserAndProject(schedule.getProject().getId());
        if (!leaderProject.getRole().equals(LEADER)) {
            log.info("권한이 없는 수정 시도: scheduleId={}, userId={}", scheduleId, leaderProject.getUser().getId());
            throw new BaseException(USER_NOT_LEADER);
        }

        schedule.setContent(request.getScheduleContent());
        schedule.setScheduleDate(request.getScheduleDate());

        scheduleRepository.save(schedule);
        log.info("스케줄 수정 성공: scheduleId={}", scheduleId);
    }

    @CurrentUser
    public ScheduleInfoResult getSchedule(Long scheduleId) {
        log.info("스케줄 정보 조회 시도: scheduleId={}", scheduleId);
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BaseException(SCHEDULE_NOT_FOUND));
        Long projectId = schedule.getProject().getId();
        UserProject userProject = validateUserAndProject(projectId);
        return new ScheduleInfoResult(
                projectId,
                schedule.getProject().getName(),
                schedule.getContent(),
                schedule.getScheduleDate(),
                userProject.getColor()
        );
    }


    @CurrentUser
    public List<ScheduleMonthlyListResult> getScheduleOfProject(Long projectId, LocalDate yearMonth) {
        log.info("프로젝트의 월별 스케줄 리스트 조회 시도: projectId={}", projectId);
        UserProject userProject = validateUserAndProject(projectId);
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();
        List<Schedule> schedules = scheduleRepository.findSchedulesByProjectIdAndMonth(projectId, year, month);

        ZoneId zoneId = ZoneId.of("Asia/Seoul"); // KST 시간대
        return schedules.stream()
                .collect(Collectors.groupingBy(schedule -> schedule.getScheduleDate()
                                .atZone(ZoneOffset.UTC)
                                .withZoneSameInstant(zoneId)
                                .getDayOfMonth(),
                        Collectors.mapping(schedule -> userProject.getColor(), Collectors.toSet())))
                .entrySet().stream()
                .map(entry -> new ScheduleMonthlyListResult(
                        entry.getKey(), entry.getValue()))
                .toList();
    }

    @CurrentUser
    public List<ScheduleListResult> getScheduleOfProjectAt(Long projectId, LocalDate date) {
        log.info("프로젝트의 특정 날짜의 스케줄 리스트 조회 시도: projectId={}, date={}", projectId, date);
        UserProject userProject = validateUserAndProject(projectId);
        List<Schedule> schedules = scheduleRepository.findByProjectIdAndDate(projectId, date);

        return schedules.stream()
                .map(schedule -> new ScheduleListResult(
                        schedule.getId(),
                        schedule.getProject().getName(),
                        schedule.getContent(),
                        schedule.getScheduleDate(),
                        userProject.getColor()
                ))
                .toList();
    }

    @CurrentUser
    public List<ScheduleMonthlyListResult> getScheduleOfUser(LocalDate yearMonth) {
        User currentUser = CurrentUserHolder.get();

        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }
        log.info("유저의 월별 스케줄 리스트 조회 시도: userId={}", currentUser.getId());

        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();
        List<Schedule> schedules = scheduleRepository.findSchedulesByUserIdAndMonth(currentUser.getId(), year, month);
        log.info(schedules.toString());
        return schedules.stream()
                .collect(Collectors.groupingBy(
                        schedule -> schedule.getScheduleDate().getDayOfMonth(),
                        Collectors.mapping(schedule -> {
                            UserProject userProject = schedule.getProject().getUserProjects().stream()
                                    .filter(up -> up.getUser().getId().equals(currentUser.getId()))
                                    .findFirst()
                                    .orElseThrow(() -> new BaseException(USER_NOT_IN_PROJECT));
                            return userProject.getColor();
                        }, Collectors.toSet())
                ))
                .entrySet().stream()
                .map(entry -> new ScheduleMonthlyListResult(entry.getKey(), entry.getValue()))
                .toList();
    }
    @CurrentUser
    public List<ScheduleListResult> getScheduleOfUserAt(LocalDate date) {
        User currentUser = CurrentUserHolder.get();

        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }
        log.info("유저의 특정 날짜의 리스트 조회 시도: userId={}, date={}", currentUser.getId(), date);
        List<Schedule> schedules = scheduleRepository.findByUserIdAndDate(currentUser.getId(), date);
        return getScheduleListResults(currentUser, schedules);
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

    private UserProject validateUserAndProject(Long projectId) { //TODO: 코드 중복 없애기
        User currentUser = CurrentUserHolder.get();

        if (currentUser == null) {
            throw new BaseException(USER_NOT_AUTHENTICATED);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BaseException(PROJECT_NOT_FOUND));

        return userProjectRepository.findByUserAndProject(currentUser, project)
                .orElseThrow(() -> new BaseException(USER_NOT_IN_PROJECT));
    }

    private List<ScheduleListResult> getScheduleListResults(User currentUser, List<Schedule> schedules) {
        return schedules.stream()
                .map(schedule -> {
                    UserProject userProject = schedule.getProject().getUserProjects().stream()
                            .filter(up -> up.getUser().getId().equals(currentUser.getId()))
                            .findFirst()
                            .orElseThrow(() -> new BaseException(USER_NOT_IN_PROJECT));

                    return new ScheduleListResult(
                            schedule.getId(),
                            schedule.getProject().getName(),
                            schedule.getContent(),
                            schedule.getScheduleDate(),
                            userProject.getColor()
                    );
                })
                .toList();
    }
}
