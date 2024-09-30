package org.focus.logmeet.repository;

import org.focus.logmeet.domain.Project;
import org.focus.logmeet.domain.User;
import org.focus.logmeet.domain.UserProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProjectRepository extends JpaRepository<UserProject, Long> {
    Optional<UserProject> findByUserAndProject(User user, Project project);
    Optional<UserProject> findByUserIdAndProject(Long userId, Project project);

    List<UserProject> findAllByUser(User user);
}
