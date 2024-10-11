package org.focus.logmeet.repository;

import org.focus.logmeet.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("SELECT s FROM Schedule s JOIN FETCH s.project p WHERE p.id = :projectId")
    List<Schedule> findSchedulesByProjectId(@Param("projectId") Long projectId);

}
