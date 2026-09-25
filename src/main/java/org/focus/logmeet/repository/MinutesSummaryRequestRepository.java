package org.focus.logmeet.repository;

import org.focus.logmeet.domain.MinutesSummaryRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MinutesSummaryRequestRepository extends JpaRepository<MinutesSummaryRequest, Long> {
    Optional<MinutesSummaryRequest> findByMinutesIdAndRequestKey(Long minutesId, String requestKey);
}
