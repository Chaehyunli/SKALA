package com.lecture.course.dto;

import com.lecture.course.entity.Course;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CourseDto {

    // 업무 요청 생성
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "업무 제목은 필수입니다")
        private String title;

        @NotBlank(message = "업무 내용은 필수입니다")
        private String description;

        @NotNull(message = "업무 유형은 필수입니다")
        private Course.Category category;

        private String usagePeriod;

        private String dataSensitivity;

        // 기존 강의 생성 요청 계약을 유지하기 위한 필드이며 업무 요청에는 사용하지 않음
        private BigDecimal price;
    }

    // 강의 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourseResponse {
        private Long id;
        private String title;
        private String description;
        private Course.Category category;
        private BigDecimal price;
        private Long instructorId;
        private String usagePeriod;
        private String dataSensitivity;
        private Course.Status status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private AnalysisResponse analysis;

        public static CourseResponse from(Course course) {
            return CourseResponse.builder()
                    .id(course.getId())
                    .title(course.getTitle())
                    .description(course.getDescription())
                    .category(course.getCategory())
                    .price(course.getPrice())
                    .instructorId(course.getInstructorId())
                    .usagePeriod(course.getUsagePeriod())
                    .dataSensitivity(course.getDataSensitivity())
                    .status(course.getStatus())
                    .createdAt(course.getCreatedAt())
                    .updatedAt(course.getUpdatedAt())
                    .analysis(AnalysisResponse.from(course))
                    .build();
        }
    }

    // 분석에 포함된 권한
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PermissionResponse {
        private String code;
        private String label;
        private String description;
        private String reason;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExcludedPermissionResponse {
        private String code;
        private String label;
        private String description;
        private String reason;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalysisResponse {
        private Long courseId;
        private List<AgentResponse> agentList;
        private String riskLevel;
        private String summary;

        public static AnalysisResponse from(Course course) {
            if (course.getAnalysisAgentList() == null) {
                return null;
            }

            List<AgentResponse> agentList = course.getAnalysisAgentList().stream()
                    .map(agent -> AgentResponse.builder()
                            .agentCode(agent.getAgentCode())
                            .permissions(toPermissions(agent.getPermissions()))
                            .excludedPermissions(toExcludedPermissions(agent.getExcludedPermissions()))
                            .build())
                    .toList();

            return AnalysisResponse.builder()
                    .courseId(course.getId())
                    .agentList(agentList)
                    .riskLevel(course.getAnalysisRiskLevel())
                    .summary(course.getAnalysisSummary())
                    .build();
        }

        private static List<PermissionResponse> toPermissions(List<Course.AnalysisPermission> permissions) {
            if (permissions == null) {
                return List.of();
            }
            return permissions.stream().map(permission -> PermissionResponse.builder()
                    .code(permission.getCode())
                    .label(permission.getLabel())
                    .description(permission.getDescription())
                    .reason(permission.getReason())
                    .build()).toList();
        }

        private static List<ExcludedPermissionResponse> toExcludedPermissions(
                List<Course.AnalysisPermission> permissions
        ) {
            if (permissions == null) {
                return List.of();
            }
            return permissions.stream().map(permission -> ExcludedPermissionResponse.builder()
                    .code(permission.getCode())
                    .label(permission.getLabel())
                    .description(permission.getDescription())
                    .reason(permission.getReason())
                    .build()).toList();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgentResponse {
        private String agentCode;
        private List<PermissionResponse> permissions;
        private List<ExcludedPermissionResponse> excludedPermissions;
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

    // 추천 서비스용 응답 (카테고리 기반 미수강 강의 목록)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendResponse {
        private List<CourseResponse> courses;
        private Course.Category category;
    }
}
