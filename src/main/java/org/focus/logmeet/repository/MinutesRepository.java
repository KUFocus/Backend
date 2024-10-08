package org.focus.logmeet.repository;

import org.focus.logmeet.domain.Minutes;
import org.focus.logmeet.domain.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MinutesRepository extends JpaRepository<Minutes, Long> {
    @Query("SELECT m FROM Minutes m WHERE m.status = :status AND m.createdAt <= :timeLimit")
    List<Minutes> findOldTemporaryMinutes(@Param("status") Status status, @Param("timeLimit") LocalDateTime timeLimit);

    List<Minutes> findAllByProjectId(Long projectId);
}
