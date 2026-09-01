package com.lecture.enrollment.service;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.entity.AgentInfo;
import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.entity.PermissionInfo;
import com.lecture.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private EnrollmentWriteService enrollmentWriteService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    void passportListCountsPermissionsAcrossAllAgents() {
        PermissionInfo permission = PermissionInfo.builder()
                .code("CRM_READ")
                .label("CRM 읽기")
                .description("CRM 고객 정보를 읽습니다.")
                .reason("분석에 필요합니다.")
                .build();
        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .userId(2L)
                .courseId(12L)
                .agentList(List.of(
                        AgentInfo.builder()
                                .agentCode("REVENUE_ANALYST")
                                .permissions(List.of(permission, permission))
                                .build(),
                        AgentInfo.builder()
                                .agentCode("DOCUMENT_ASSISTANT")
                                .permissions(List.of(permission))
                                .build()
                ))
                .riskLevel(Enrollment.RiskLevel.MEDIUM)
                .status(Enrollment.Status.READY_FOR_APPROVAL)
                .build();

        when(enrollmentRepository.findByUserIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of(enrollment));
        when(courseServiceClient.getCourse(12L))
                .thenReturn(Map.of("title", "분석 업무"));

        List<EnrollmentDto.PassportListResponse> responses =
                enrollmentService.getEnrollmentsByUser(2L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getAgentList()).hasSize(2);
        assertThat(responses.getFirst().getPermissionCount()).isEqualTo(3);
    }

    @Test
    void activePassportsExposePermissionsAndExpirationForReuseCheck() {
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(1);
        Enrollment enrollment = Enrollment.builder()
                .id(7L)
                .userId(2L)
                .courseId(12L)
                .agentList(List.of(AgentInfo.builder()
                        .agentCode("REVENUE_ANALYST")
                        .permissions(List.of(PermissionInfo.builder()
                                .code("CUSTOMER_READ")
                                .label("CRM 고객정보 조회")
                                .description("고객 정보를 읽습니다.")
                                .reason("분석에 필요합니다.")
                                .build()))
                        .build()))
                .status(Enrollment.Status.ACTIVE)
                .expiredAt(expiredAt)
                .build();

        when(enrollmentRepository.findByUserIdAndStatus(2L, Enrollment.Status.ACTIVE))
                .thenReturn(List.of(enrollment));

        List<EnrollmentDto.ActivePassportResponse> responses =
                enrollmentService.getActivePassports(2L);

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.getId()).isEqualTo(7L);
            assertThat(response.getUserId()).isEqualTo(2L);
            assertThat(response.getStatus()).isEqualTo(Enrollment.Status.ACTIVE);
            assertThat(response.getExpiredAt()).isEqualTo(expiredAt);
            assertThat(response.getAgentList()).hasSize(1);
        });
    }

    @Test
    void approvalSetsExpirationFromMissionUsagePeriod() {
        Enrollment enrollment = Enrollment.builder()
                .id(7L)
                .userId(2L)
                .courseId(12L)
                .agentList(List.of())
                .status(Enrollment.Status.READY_FOR_APPROVAL)
                .build();
        LocalDateTime beforeApproval = LocalDateTime.now();

        when(enrollmentRepository.findById(7L)).thenReturn(Optional.of(enrollment));
        when(courseServiceClient.getCourse(12L)).thenReturn(Map.of("usagePeriod", "HOURS_24"));

        enrollmentService.approveEnrollment(7L, 99L);

        assertThat(enrollment.getStatus()).isEqualTo(Enrollment.Status.ACTIVE);
        assertThat(enrollment.getExpiredAt())
                .isAfterOrEqualTo(beforeApproval.plusHours(24))
                .isBeforeOrEqualTo(LocalDateTime.now().plusHours(24));
    }
}
