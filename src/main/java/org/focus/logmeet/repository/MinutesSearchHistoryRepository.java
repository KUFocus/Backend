package org.focus.logmeet.repository;

import org.focus.logmeet.domain.MinutesSearch;
import org.focus.logmeet.domain.MinutesSearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MinutesSearchHistoryRepository extends JpaRepository<MinutesSearchHistory, Long> {
    List<MinutesSearchHistory> findByUserId(Long userId);

}
