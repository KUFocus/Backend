package org.focus.logmeet.domain;

import jakarta.persistence.*;
import lombok.*;
import org.focus.logmeet.domain.enums.ProjectColor;
import org.focus.logmeet.domain.enums.Role;
import org.focus.logmeet.domain.util.BaseTimeEntity;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "user_project")
public class UserProject extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_project_id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private Role role; // TODO: 역할 enum 변환

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectColor color;
}
