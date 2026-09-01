package com.lecture.enrollment.dto;

import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.entity.AgentInfo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class EnrollmentDto {

    // Passport 신청 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollRequest {
        @NotNull(message = "업무 요청 ID는 필수입니다")
        private Long courseId;

        @Valid
        @NotEmpty(message = "Agent 목록은 한 개 이상 필요합니다")
        private List<AgentInfo> agentList;

        @NotNull(message = "위험도는 필수입니다")
        private Enrollment.RiskLevel riskLevel;

        @NotBlank(message = "분석 요약은 필수입니다")
        private String summary;
    }


    // 강의 요약 정보 (내 수강 목록 표시용)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseSummary {
        private Long id;
        private String title;
        private String description;
        private String category;
        private Integer price;
        private String thumbnail;
        private String instructorName;
    }

    // Passport 신청 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentResponse {
        private Long id;
        private Long userId;
        private Long courseId;

        private List<AgentInfo> agentList;

        private Enrollment.RiskLevel riskLevel;
        private String summary;
        private Enrollment.Status status;

        private Long approvedBy;
        private LocalDateTime approvedAt;
        private String rejectedReason;
        private LocalDateTime expiredAt;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // 추가
        private CourseSummary course;


        public static EnrollmentResponse from(Enrollment enrollment) {
            return from(enrollment, null);
        }


        public static EnrollmentResponse from(
                Enrollment enrollment,
                CourseSummary course
        ) {
            return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUserId())
                .courseId(enrollment.getCourseId())
                .agentList(enrollment.getAgentList())
                .riskLevel(enrollment.getRiskLevel())
                .summary(enrollment.getSummary())
                .status(enrollment.getStatus())
                .approvedBy(enrollment.getApprovedBy())
                .approvedAt(enrollment.getApprovedAt())
                .rejectedReason(enrollment.getRejectedReason())
                .expiredAt(enrollment.getExpiredAt())
                .createdAt(enrollment.getCreatedAt())
                .updatedAt(enrollment.getUpdatedAt())
                .course(course)
                    .build();
        }
    }

    // 내 Passport 목록 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PassportListResponse {
        private Long id;
        private Long courseId;
        private String courseTitle;
        private List<AgentInfo> agentList;
        private Enrollment.RiskLevel riskLevel;
        private Integer permissionCount;
        private Enrollment.Status status;
        private LocalDateTime createdAt;
        private LocalDateTime expiredAt;
    }

    // Passport 상세 조회 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PassportDetailResponse {
        private Long id;
        private Long userId;
        private Long courseId;
        private String courseTitle;
        private String courseDescription;
        private String summary;
        private List<AgentInfo> agentList;
        private Enrollment.RiskLevel riskLevel;
        private Enrollment.Status status;
        private Long approvedBy;
        private LocalDateTime approvedAt;
        private String rejectedReason;
        private LocalDateTime expiredAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // 관리자 승인 대기 목록 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReadyForApprovalPassportResponse {
        private Long id;
        private Long courseId;
        private Long userId;
        private String courseTitle;
        private List<AgentInfo> agentList;
        private Enrollment.RiskLevel riskLevel;
        private Integer permissionCount;
        private Enrollment.Status status;
        private LocalDateTime createdAt;
    }

    // 관리자 전체 Passport 이력 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminPassportResponse {
        private Long id;
        private Long courseId;
        private Long userId;
        private String courseTitle;
        private List<AgentInfo> agentList;
        private Enrollment.RiskLevel riskLevel;
        private Integer permissionCount;
        private Enrollment.Status status;
        private LocalDateTime createdAt;
        private Long approvedBy;
        private LocalDateTime approvedAt;
        private String rejectedReason;
    }

    // Passport 승인 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApprovePassportResponse {
        private Long id;
        private Enrollment.Status status;
        private Long approvedBy;
        private LocalDateTime approvedAt;
    }

    // Passport 반려 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RejectPassportRequest {
        @NotBlank(message = "반려 사유는 필수입니다")
        private String reason;
    }

    // Passport 반려 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RejectPassportResponse {
        private Long id;
        private Enrollment.Status status;
        private Long approvedBy;
        private LocalDateTime approvedAt;
        private String rejectedReason;
    }

    // 추천 서비스용: 수강 이력 조회 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrollmentHistoryResponse {
        private Long userId;
        private List<Long> activeCourseIds;
    }

    // Recommend Service가 단일 Passport 재사용 여부를 판정할 때 사용하는 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivePassportResponse {
        private Long id;
        private Long userId;
        private List<AgentInfo> agentList;
        private Enrollment.Status status;
        private LocalDateTime expiredAt;

        public static ActivePassportResponse from(Enrollment enrollment) {
            return ActivePassportResponse.builder()
                    .id(enrollment.getId())
                    .userId(enrollment.getUserId())
                    .agentList(enrollment.getAgentList())
                    .status(enrollment.getStatus())
                    .expiredAt(enrollment.getExpiredAt())
                    .build();
        }
    }

    // 공통 API 응답 래퍼
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message("성공")
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
