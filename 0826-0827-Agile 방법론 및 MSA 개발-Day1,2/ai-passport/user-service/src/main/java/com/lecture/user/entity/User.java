package com.lecture.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Role {
        STUDENT, INSTRUCTOR
    }

    public void addCredit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("적립할 크레딧은 0보다 커야 합니다");
        }
        this.credit = (this.credit == null ? BigDecimal.ZERO : this.credit).add(amount);
    }

    public void deductCredit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("차감할 크레딧은 0보다 커야 합니다");
        }

        BigDecimal currentCredit = this.credit == null ? BigDecimal.ZERO : this.credit;
        if (currentCredit.compareTo(amount) < 0) {
            throw new IllegalArgumentException("크레딧이 부족합니다. 크레딧을 충전한 뒤 다시 시도해주세요.");
        }

        this.credit = currentCredit.subtract(amount);
    }
}
