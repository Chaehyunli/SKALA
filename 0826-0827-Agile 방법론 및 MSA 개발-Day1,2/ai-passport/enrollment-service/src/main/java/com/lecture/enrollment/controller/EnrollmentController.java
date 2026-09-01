package com.lecture.enrollment.controller;

import com.lecture.enrollment.dto.EnrollmentDto;
import com.lecture.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * POST /api/enrollments
     * LLM 분석 결과를 바탕으로 Passport 승인 요청 생성
     * Gateway에서 X-User-Id 헤더로 사용자 ID 전달
     */
    @PostMapping
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.EnrollmentResponse>> enroll(
            @Valid @RequestBody EnrollmentDto.EnrollRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        EnrollmentDto.EnrollmentResponse response =
                enrollmentService.enroll(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EnrollmentDto.ApiResponse.success(response));
    }

    /**
     * GET /enrollments/my - 내 Passport 목록 조회
     * Gateway가 전달한 X-User-Id 헤더를 사용
     */
    @GetMapping("/my")
    public ResponseEntity<EnrollmentDto.ApiResponse<List<EnrollmentDto.PassportListResponse>>> getMyEnrollments(
            @RequestHeader("X-User-Id") Long userId) {

        List<EnrollmentDto.PassportListResponse> response =
                enrollmentService.getEnrollmentsByUser(userId);
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(response));
    }

    /**
     * GET /api/enrollments/{id} - 내 Passport 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.PassportDetailResponse>> getEnrollmentDetail(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        EnrollmentDto.PassportDetailResponse response =
                enrollmentService.getEnrollmentDetail(id, userId);
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(response));
    }

    /**
     * GET /api/enrollments/{id}/passport.yml
     * 본인 Passport를 에이전트가 읽는 권한 매니페스트 YAML로 다운로드
     */
    @GetMapping("/{id}/passport.yml")
    public ResponseEntity<byte[]> exportPassportYaml(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        return yamlResponse(id, enrollmentService.getPassportYaml(id, userId));
    }

    /** GET /api/enrollments/admin/{id}/passport.yml - 관리자용 권한 매니페스트 YAML */
    @GetMapping("/admin/{id}/passport.yml")
    public ResponseEntity<byte[]> exportPassportYamlForAdmin(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole) {

        validateAdmin(userRole);
        return yamlResponse(id, enrollmentService.getPassportYamlForAdmin(id));
    }

    private ResponseEntity<byte[]> yamlResponse(Long id, String yaml) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "yaml", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"passport-" + id + ".yml\"")
                .body(yaml.getBytes(StandardCharsets.UTF_8));
    }

    /** GET /api/enrollments/my/course/{courseId} - 업무 요청에 연결된 내 Passport */
    @GetMapping("/my/course/{courseId}")
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.PassportDetailResponse>> getMyEnrollmentByCourse(
            @PathVariable Long courseId,
            @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.getEnrollmentByCourse(courseId, userId)
        ));
    }

    /**
     * GET /api/enrollments/admin/ready-for-approval - 관리자 승인 대기 목록 조회
     */
    @GetMapping({"/admin/pending", "/admin/ready-for-approval"})
    public ResponseEntity<EnrollmentDto.ApiResponse<List<EnrollmentDto.ReadyForApprovalPassportResponse>>> getReadyForApprovalEnrollments(
            @RequestHeader("X-User-Role") String userRole) {

        validateAdmin(userRole);

        List<EnrollmentDto.ReadyForApprovalPassportResponse> response =
                enrollmentService.getReadyForApprovalEnrollments();
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(response));
    }

    /** GET /api/enrollments/admin/all - 관리자 전체 Passport 이력 */
    @GetMapping("/admin/all")
    public ResponseEntity<EnrollmentDto.ApiResponse<List<EnrollmentDto.AdminPassportResponse>>> getAllEnrollments(
            @RequestHeader("X-User-Role") String userRole) {

        validateAdmin(userRole);
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.getAllEnrollments()
        ));
    }

    /** GET /api/enrollments/admin/{id} - 관리자 Passport 상세 조회 */
    @GetMapping("/admin/{id}")
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.PassportDetailResponse>> getEnrollmentDetailForAdmin(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String userRole) {

        validateAdmin(userRole);
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(
                enrollmentService.getEnrollmentDetailForAdmin(id)
        ));
    }

    /**
     * PUT/PATCH /api/enrollments/{id}/approve - 관리자 Passport 승인
     */
    @RequestMapping(value = "/{id}/approve", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.ApprovePassportResponse>> approveEnrollment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestHeader("X-User-Role") String userRole) {

        validateAdmin(userRole);

        EnrollmentDto.ApprovePassportResponse response =
                enrollmentService.approveEnrollment(id, adminId);
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(response));
    }

    /**
     * PATCH /api/enrollments/{id}/reject - 관리자 Passport 반려
     */
    @RequestMapping(value = "/{id}/reject", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<EnrollmentDto.ApiResponse<EnrollmentDto.RejectPassportResponse>> rejectEnrollment(
            @PathVariable Long id,
            @Valid @RequestBody EnrollmentDto.RejectPassportRequest request,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestHeader("X-User-Role") String userRole) {

        validateAdmin(userRole);

        EnrollmentDto.RejectPassportResponse response =
                enrollmentService.rejectEnrollment(id, adminId, request.getReason());
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(response));
    }

    /**
     * GET /enrollments/user/{userId} - 특정 사용자 수강 목록 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<EnrollmentDto.ApiResponse<List<EnrollmentDto.PassportListResponse>>> getEnrollments(
            @PathVariable Long userId) {

        List<EnrollmentDto.PassportListResponse> response =
                enrollmentService.getEnrollmentsByUser(userId);
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(response));
    }

    /**
     * GET /enrollments/internal/history/{userId} - 수강 이력 조회 (Recommend Service용)
     */
    @GetMapping("/internal/history/{userId}")
    public ResponseEntity<EnrollmentDto.EnrollmentHistoryResponse> getEnrollmentHistory(
            @PathVariable Long userId) {

        return ResponseEntity.ok(enrollmentService.getEnrollmentHistory(userId));
    }

    /** GET /api/enrollments/internal/active/{userId} - Passport 재사용 판정용 */
    @GetMapping("/internal/active/{userId}")
    public ResponseEntity<List<EnrollmentDto.ActivePassportResponse>> getActivePassports(
            @PathVariable Long userId) {

        return ResponseEntity.ok(enrollmentService.getActivePassports(userId));
    }

    /** DELETE /api/enrollments/{id} - 본인의 승인 대기 Passport 취소 */
    @DeleteMapping("/{id}")
    public ResponseEntity<EnrollmentDto.ApiResponse<Void>> cancelEnrollment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        enrollmentService.cancelEnrollment(id, userId);
        return ResponseEntity.ok(EnrollmentDto.ApiResponse.success(null));
    }

    private void validateAdmin(String userRole) {
    if (!"INSTRUCTOR".equalsIgnoreCase(userRole)) {
        throw new IllegalArgumentException("관리자만 조회할 수 있습니다.");
        }
}
}
