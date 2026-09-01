package com.lecture.enrollment.service;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.entity.AgentInfo;
import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseServiceClient courseServiceClient;
    private final EnrollmentWriteService enrollmentWriteService;

    /**
     * Passport 신청
     * 1. 원본 업무 요청 존재 확인
     * 2. 중복 수강 확인
     * 3. READY_FOR_APPROVAL 상태로 Passport 생성
     */
    public EnrollmentDto.EnrollmentResponse enroll(Long userId, EnrollmentDto.EnrollRequest request) {

        Long courseId = request.getCourseId();

        if (!courseServiceClient.existsCourse(courseId)) {
            throw new IllegalArgumentException("존재하지 않는 업무 요청입니다: " + courseId);
        }

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new IllegalArgumentException("이미 Passport를 신청한 업무 요청입니다.");
        }

        Enrollment enrollment = enrollmentWriteService.createReadyForApprovalEnrollment(userId, request);


        log.info(
                "[EnrollmentService] Passport 승인 요청 완료 - enrollmentId: {}, userId: {}, courseId: {}",
                enrollment.getId(),
                userId,
                courseId
            );

        return EnrollmentDto.EnrollmentResponse.from(enrollment);
    }


    /**
     * 로그인 사용자의 Passport 목록을 최신 신청순으로 조회
     */
    public List<EnrollmentDto.PassportListResponse> getEnrollmentsByUser(Long userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return enrollments.stream()
                .map(enrollment -> {
                    Map<String, Object> courseInfo = courseServiceClient.getCourse(enrollment.getCourseId());

                    int permissionCount = countPermissions(enrollment.getAgentList());

                    return EnrollmentDto.PassportListResponse.builder()
                            .id(enrollment.getId())
                            .courseId(enrollment.getCourseId())
                            .courseTitle((String) courseInfo.get("title"))
                            .agentList(enrollment.getAgentList())
                            .riskLevel(enrollment.getRiskLevel())
                            .permissionCount(permissionCount)
                            .status(enrollment.getStatus())
                            .createdAt(enrollment.getCreatedAt())
                            .expiredAt(enrollment.getExpiredAt())
                            .build();
                })
                .toList();
    }

    /**
     * 로그인 사용자의 Passport 상세 조회
     */
    public EnrollmentDto.PassportDetailResponse getEnrollmentDetail(Long id, Long userId) {
        Enrollment enrollment = enrollmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Passport를 찾을 수 없습니다."));

        return toPassportDetail(enrollment);
    }

    public EnrollmentDto.PassportDetailResponse getEnrollmentByCourse(Long courseId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("연결된 Passport를 찾을 수 없습니다."));

        return toPassportDetail(enrollment);
    }

    public EnrollmentDto.PassportDetailResponse getEnrollmentDetailForAdmin(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Passport를 찾을 수 없습니다."));

        return toPassportDetail(enrollment);
    }

    /** 본인 Passport를 에이전트가 읽는 권한 매니페스트 YAML로 내보낸다. */
    public String getPassportYaml(Long id, Long userId) {
        Enrollment enrollment = enrollmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Passport를 찾을 수 없습니다."));
        return PassportManifestBuilder.toYaml(enrollment, courseServiceClient.getCourse(enrollment.getCourseId()));
    }

    /** 관리자용: 임의 사용자 Passport의 권한 매니페스트 YAML. */
    public String getPassportYamlForAdmin(Long id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Passport를 찾을 수 없습니다."));
        return PassportManifestBuilder.toYaml(enrollment, courseServiceClient.getCourse(enrollment.getCourseId()));
    }

    private EnrollmentDto.PassportDetailResponse toPassportDetail(Enrollment enrollment) {

        Map<String, Object> courseInfo = courseServiceClient.getCourse(enrollment.getCourseId());

        return EnrollmentDto.PassportDetailResponse.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUserId())
                .courseId(enrollment.getCourseId())
                .courseTitle((String) courseInfo.get("title"))
                .courseDescription((String) courseInfo.get("description"))
                .summary(enrollment.getSummary())
                .agentList(enrollment.getAgentList())
                .riskLevel(enrollment.getRiskLevel())
                .status(enrollment.getStatus())
                .approvedBy(enrollment.getApprovedBy())
                .approvedAt(enrollment.getApprovedAt())
                .rejectedReason(enrollment.getRejectedReason())
                .expiredAt(enrollment.getExpiredAt())
                .createdAt(enrollment.getCreatedAt())
                .updatedAt(enrollment.getUpdatedAt())
                .build();
    }

    /**
     * 관리자용 Passport 승인 대기 목록을 오래된 신청순으로 조회
     */
    public List<EnrollmentDto.ReadyForApprovalPassportResponse> getReadyForApprovalEnrollments() {
        List<Enrollment> enrollments = enrollmentRepository.findByStatusOrderByCreatedAtAsc(
                Enrollment.Status.READY_FOR_APPROVAL
        );

        return enrollments.stream()
                .map(enrollment -> {
                    Map<String, Object> courseInfo = courseServiceClient.getCourse(enrollment.getCourseId());

                    int permissionCount = countPermissions(enrollment.getAgentList());

                    return EnrollmentDto.ReadyForApprovalPassportResponse.builder()
                            .id(enrollment.getId())
                            .courseId(enrollment.getCourseId())
                            .userId(enrollment.getUserId())
                            .courseTitle((String) courseInfo.get("title"))
                            .agentList(enrollment.getAgentList())
                            .riskLevel(enrollment.getRiskLevel())
                            .permissionCount(permissionCount)
                            .status(enrollment.getStatus())
                            .createdAt(enrollment.getCreatedAt())
                            .build();
                })
                .toList();
    }

    public List<EnrollmentDto.AdminPassportResponse> getAllEnrollments() {
        return enrollmentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(enrollment -> {
                    Map<String, Object> courseInfo = courseServiceClient.getCourse(enrollment.getCourseId());
                    int permissionCount = countPermissions(enrollment.getAgentList());

                    return EnrollmentDto.AdminPassportResponse.builder()
                            .id(enrollment.getId())
                            .courseId(enrollment.getCourseId())
                            .userId(enrollment.getUserId())
                            .courseTitle((String) courseInfo.get("title"))
                            .agentList(enrollment.getAgentList())
                            .riskLevel(enrollment.getRiskLevel())
                            .permissionCount(permissionCount)
                            .status(enrollment.getStatus())
                            .createdAt(enrollment.getCreatedAt())
                            .approvedBy(enrollment.getApprovedBy())
                            .approvedAt(enrollment.getApprovedAt())
                            .rejectedReason(enrollment.getRejectedReason())
                            .build();
                })
                .toList();
    }

    /**
     * 관리자가 승인 대기 Passport를 승인
     */
    @Transactional
    public EnrollmentDto.ApprovePassportResponse approveEnrollment(Long id, Long adminId) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Passport를 찾을 수 없습니다."));

        Map<String, Object> courseInfo = courseServiceClient.getCourse(enrollment.getCourseId());
        LocalDateTime expiredAt = calculateExpiration((String) courseInfo.get("usagePeriod"));
        enrollment.approve(adminId, expiredAt);

        return EnrollmentDto.ApprovePassportResponse.builder()
                .id(enrollment.getId())
                .status(enrollment.getStatus())
                .approvedBy(enrollment.getApprovedBy())
                .approvedAt(enrollment.getApprovedAt())
                .build();
    }

    /**
     * 관리자가 승인 대기 Passport를 반려
     */
    @Transactional
    public EnrollmentDto.RejectPassportResponse rejectEnrollment(
            Long id,
            Long adminId,
            String reason
    ) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Passport를 찾을 수 없습니다."));

        enrollment.reject(adminId, reason);

        return EnrollmentDto.RejectPassportResponse.builder()
                .id(enrollment.getId())
                .status(enrollment.getStatus())
                .approvedBy(enrollment.getApprovedBy())
                .approvedAt(enrollment.getApprovedAt())
                .rejectedReason(enrollment.getRejectedReason())
                .build();
    }

    /**
     * 수강 이력 조회 - 추천 서비스용
     */
    public EnrollmentDto.EnrollmentHistoryResponse getEnrollmentHistory(Long userId) {
        List<Long> activeCourseIds = enrollmentRepository
                .findByUserIdAndStatus(userId, Enrollment.Status.ACTIVE)
                .stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toList());

        return EnrollmentDto.EnrollmentHistoryResponse.builder()
                .userId(userId)
                .activeCourseIds(activeCourseIds)
                .build();
    }

    public List<EnrollmentDto.ActivePassportResponse> getActivePassports(Long userId) {
        return enrollmentRepository.findByUserIdAndStatus(userId, Enrollment.Status.ACTIVE)
                .stream()
                .map(EnrollmentDto.ActivePassportResponse::from)
                .toList();
    }

    @Transactional
    public void cancelEnrollment(Long id, Long userId) {
        Enrollment enrollment = enrollmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Passport를 찾을 수 없습니다."));

        if (enrollment.getStatus() != Enrollment.Status.READY_FOR_APPROVAL) {
            throw new IllegalArgumentException("승인 대기 중인 Passport만 취소할 수 있습니다.");
        }

        enrollmentRepository.delete(enrollment);
    }

    private int countPermissions(List<AgentInfo> agentList) {
        if (agentList == null) {
            return 0;
        }

        return agentList.stream()
                .map(AgentInfo::getPermissions)
                .filter(java.util.Objects::nonNull)
                .mapToInt(List::size)
                .sum();
    }

    private LocalDateTime calculateExpiration(String usagePeriod) {
        LocalDateTime now = LocalDateTime.now();
        if ("HOURS_24".equals(usagePeriod)) {
            return now.plusHours(24);
        }
        if ("DAYS_7".equals(usagePeriod)) {
            return now.plusDays(7);
        }
        return null;
    }

}
