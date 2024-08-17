package org.focus.logmeet.repository;

import org.focus.logmeet.domain.UserProject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProjectRepository extends JpaRepository<UserProject, Long> {
}
