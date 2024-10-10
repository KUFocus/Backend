package org.focus.logmeet.domain;

import jakarta.persistence.*;
import lombok.*;
import org.focus.logmeet.domain.enums.Status;
import org.focus.logmeet.domain.util.BaseTimeEntity;

import java.lang.reflect.Type;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Project extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserProject> userProjects;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Schedule> schedules;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Minutes> minutes;

    @Column(length = 30, nullable = false)
    private String name;

    @Column(length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;
}
