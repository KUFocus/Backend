package org.focus.logmeet.repository;

import org.focus.logmeet.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
