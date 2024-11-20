package org.focus.logmeet.repository;

import org.focus.logmeet.domain.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {
    boolean existsByCode(String code);
    Optional<InviteCode> findByCodeAndExpirationDateAfter(String code, LocalDateTime dateTime);
    @Query("SELECT ic FROM InviteCode ic WHERE ic.project.id = :projectId AND ic.expirationDate > :currentTime")
    Optional<InviteCode> findValidCodeByProjectId(@Param("projectId") Long projectId, @Param("currentTime") LocalDateTime currentTime);
}
