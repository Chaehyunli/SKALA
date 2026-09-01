package com.lecture.enrollment.service;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentWriteService {

    private final EnrollmentRepository enrollmentRepository;

    /**
     * Passport 승인 요청서를 READY_FOR_APPROVAL 상태로 생성
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Enrollment createReadyForApprovalEnrollment(
            Long userId,
            EnrollmentDto.EnrollRequest request
    ) {
    
        Enrollment enrollment = enrollmentRepository.save(
                Enrollment.builder()
                        .userId(userId)
                        .courseId(request.getCourseId())
                        .agentList(request.getAgentList())
                        .riskLevel(request.getRiskLevel())
                        .summary(request.getSummary())
                        .status(Enrollment.Status.READY_FOR_APPROVAL)
                        .build()
        );

        log.info(
                "[EnrollmentWriteService] Passport 승인 요청 생성 완료 - enrollmentId: {}, userId: {}, courseId: {}",
                enrollment.getId(),
                userId,
                request.getCourseId()
        );

        return enrollment;
       
    }
}
