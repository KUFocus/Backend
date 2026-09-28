package org.focus.logmeet.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_minutes_summary_request",
        columnNames = {"minutes_id", "request_key"}))
public class MinutesSummaryRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "minutes_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Minutes minutes;

    @Column(name = "request_key", length = 64, nullable = false)
    private String requestKey;

    @Column(length = 64, nullable = false)
    private String inputFingerprint;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String responseJson;
}
