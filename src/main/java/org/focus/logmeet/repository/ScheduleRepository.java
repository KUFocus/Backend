package org.focus.logmeet.repository;

import org.focus.logmeet.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("SELECT s FROM Schedule s JOIN FETCH s.project p WHERE p.id = :projectId AND FUNCTION('YEAR', s.scheduleDate) = :year AND FUNCTION('MONTH', s.scheduleDate) = :month")
    List<Schedule> findSchedulesByProjectIdAndMonth(@Param("projectId") Long projectId, @Param("year") int year, @Param("month") int month);

    @Query("SELECT s FROM Schedule s WHERE s.project.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId) AND FUNCTION('YEAR', s.scheduleDate) = :year AND FUNCTION('MONTH', s.scheduleDate) = :month")
    List<Schedule> findSchedulesByUserIdAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

    // 프로젝트 캘린더 일정 조회
    @Query("SELECT s FROM Schedule s WHERE s.project.id = :projectId AND DATE(s.scheduleDate) = :date")
    List<Schedule> findByProjectIdAndDate(@Param("projectId") Long projectId, @Param("date") LocalDate date);

    // 개인 캘린더 일정 조회
    @Query("SELECT s FROM Schedule s WHERE s.project.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId) AND DATE(s.scheduleDate) = :date")
    List<Schedule> findByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

}
