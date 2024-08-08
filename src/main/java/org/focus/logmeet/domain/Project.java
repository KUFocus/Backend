package org.focus.logmeet.domain;

import jakarta.persistence.*;
import lombok.*;
import org.focus.logmeet.domain.util.BaseTimeEntity;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Project extends BaseTimeEntity { //TODO: 색깔 정보 추가
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserProject> userProjects;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private List<Minutes> minutes;

    @Column(length = 30, nullable = false)
    private String name;

    @Column(length = 500)
    private String content;
    private String status;
}
