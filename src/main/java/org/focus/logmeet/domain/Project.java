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
public class Project extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private List<Minutes> minutes;

    @Column(length = 30, nullable = false)
    private String name;

    @Column(length = 500)
    private String content;

    private String status;
}
