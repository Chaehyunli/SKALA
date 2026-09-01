package com.lecture.enrollment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.time.LocalDateTime;



@Entity
@Table(name = "enrollments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.READY_FOR_APPROVAL;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Status {
        READY_FOR_APPROVAL, // 관리자 승인 대기
        ACTIVE,    // 승인된 Passport, Passport 사용 가능
        REJECTED,  // 반려된 Passport
        EXPIRED // Passport 만료
    }

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "agent_list", columnDefinition = "JSON", nullable = false)
    private List<AgentInfo> agentList;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    public void approve(Long adminId, LocalDateTime expiredAt) {
        if (this.status != Status.READY_FOR_APPROVAL) {
            throw new IllegalArgumentException("승인 대기 상태만 승인할 수 있습니다.");
        }

        this.status = Status.ACTIVE;
        this.approvedBy = adminId;
        this.approvedAt = LocalDateTime.now();
        this.expiredAt = expiredAt;
        this.rejectedReason = null;
    }

    public void reject(Long adminId, String reason) {
        if (this.status != Status.READY_FOR_APPROVAL) {
            throw new IllegalArgumentException("승인 대기 상태만 반려할 수 있습니다.");
        }

            this.status = Status.REJECTED;
            this.approvedBy = adminId;
            this.approvedAt = LocalDateTime.now();
            this.rejectedReason = reason;
    }

}
