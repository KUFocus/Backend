package org.focus.logmeet.repository;

import org.focus.logmeet.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("SELECT s FROM Schedule s JOIN FETCH s.project p WHERE p.id = :projectId")
    List<Schedule> findSchedulesByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT s FROM Schedule s WHERE s.project.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)")
    List<Schedule> findSchedulesByUserId(@Param("userId") Long userId);
}
