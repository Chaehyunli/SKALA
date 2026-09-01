package com.lecture.course.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    // 기존 강의 모델과의 호환을 위해 유지하며 업무 요청에는 0을 저장
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // 기존 필드명을 유지하고 업무 요청을 생성한 사용자 ID를 저장
    @Column(nullable = false)
    private Long instructorId;

    @Column(name = "usage_period")
    private String usagePeriod;

    @Column(name = "data_sensitivity")
    private String dataSensitivity;

    // 업무 요청 분석 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.CREATED;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_agent_list", columnDefinition = "JSON")
    private List<AnalysisAgent> analysisAgentList;

    @Column(name = "analysis_risk_level")
    private String analysisRiskLevel;

    @Column(name = "analysis_summary", columnDefinition = "TEXT")
    private String analysisSummary;

    public enum Category {
        // 업무요청 모델 카테고리
        CUSTOMER_ANALYSIS, ANALYSIS_TASK, DATA_LOOKUP, COMMUNICATION, DOCUMENT,

        // 기존 강의안 코드
        BACKEND, FRONTEND, DEVOPS, DATA_SCIENCE, MOBILE, SECURITY, DATABASE, OTHER
    }

    public enum Status {
        // 업무 요청 분석 상태
        CREATED, ANALYZING, ANALYZED, FAILED,
        // 기존 강의 상태
        ACTIVE, INACTIVE
    }

    public void startAnalysis() {
        this.status = Status.ANALYZING;
    }

    public void completeAnalysis(
            List<AnalysisAgent> agentList,
            String riskLevel,
            String summary
    ) {
        this.analysisAgentList = agentList;
        this.analysisRiskLevel = riskLevel;
        this.analysisSummary = summary;
        this.status = Status.ANALYZED;
    }

    // LLM 연동 시 분석 실패 상태를 저장하기 위해 유지
    public void failAnalysis() {
        this.status = Status.FAILED;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalysisPermission {
        private String code;
        @JsonAlias("name")
        private String label;
        private String description;
        private String reason;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalysisAgent {
        private String agentCode;
        private List<AnalysisPermission> permissions;
        private List<AnalysisPermission> excludedPermissions;
    }
}
