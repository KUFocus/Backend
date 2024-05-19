package org.focus.logmeet.repository;

import org.focus.logmeet.domain.Minutes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinutesRepository extends JpaRepository<Minutes, Long> {
}
