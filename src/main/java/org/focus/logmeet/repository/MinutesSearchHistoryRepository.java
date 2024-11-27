package org.focus.logmeet.repository;

import jakarta.transaction.Transactional;
import org.focus.logmeet.domain.MinutesSearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MinutesSearchHistoryRepository extends JpaRepository<MinutesSearchHistory, Long> {
    List<MinutesSearchHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<MinutesSearchHistory> findByUserIdOrderByCreatedAtAsc(Long userId);
    boolean existsByUserIdAndMinutesId(Long userId, Long minutesId);
    @Transactional
    void deleteByUserIdAndMinutesId(Long userId, Long minutesId);
}
