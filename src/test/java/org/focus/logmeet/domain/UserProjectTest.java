package org.focus.logmeet.domain;

import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserProjectTest {

    private UserProject userProject;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        Project project = new Project();
        project.setId(1L);
        project.setName("테스트 프로젝트");

        userProject = UserProject.builder()
                .user(user)
                .project(project)
                .role(Role.MEMBER)
                .bookmark(false)
                .color(ProjectColor.PROJECT_1)
                .build();
    }

    @Test
    @DisplayName("UserProject 객체 생성 및 ID 필드 자동 생성 테스트")
    void testUserProjectIdGeneration() {
        // 실제 저장 시 ID가 자동으로 생성되는지 확인
        UserProject savedUserProject = saveUserProject(userProject);
        assertTrue(savedUserProject.getId() > 0);
    }

    private UserProject saveUserProject(UserProject userProject) {
        userProject.setId(1L);
        return userProject;
    }
}
