package org.focus.logmeet.repository;

import jakarta.persistence.LockModeType;
import org.focus.logmeet.domain.Minutes;
import org.focus.logmeet.domain.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MinutesRepository extends JpaRepository<Minutes, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Minutes m WHERE m.id = :minutesId")
    Optional<Minutes> findByIdForSummary(@Param("minutesId") Long minutesId);

    @Query("SELECT m FROM Minutes m WHERE m.status = :status AND m.createdAt <= :timeLimit")
    List<Minutes> findOldTemporaryMinutes(@Param("status") Status status, @Param("timeLimit") LocalDateTime timeLimit);

    List<Minutes> findAllByProjectId(Long projectId);

    @Query("SELECT m FROM Minutes m JOIN m.project p JOIN p.userProjects up WHERE up.user.id = :userId")
    List<Minutes> findAllByUserProjects_UserId(@Param("userId") Long userId);

}
