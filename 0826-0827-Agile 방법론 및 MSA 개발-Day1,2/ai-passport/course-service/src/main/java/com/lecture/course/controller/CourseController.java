package com.lecture.course.controller;

import com.lecture.course.dto.CourseDto;
import com.lecture.course.entity.Course;
import com.lecture.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * POST /api/courses - 업무 요청 생성
     * 기존 강의 모델과의 호환을 위해 X-User-Id를 instructorId로 전달
     */
    @PostMapping
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.CourseResponse>> createCourse(
            @Valid @RequestBody CourseDto.CreateRequest request,
            @RequestHeader("X-User-Id") Long instructorId) {

        CourseDto.CourseResponse response = courseService.createCourse(request, instructorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CourseDto.ApiResponse.success(response));
    }

    /**
     * GET /api/courses - 전체 활성 강의 목록
     */
    @GetMapping
    public ResponseEntity<CourseDto.ApiResponse<List<CourseDto.CourseResponse>>> getAllCourses() {
        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.getAllCourses())
        );
    }

    /**
     * GET /api/courses/{id} - 업무 요청 상세
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourseDto.ApiResponse<CourseDto.CourseResponse>> getCourse(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long instructorId) {

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(
                        courseService.getCourse(id, instructorId)
                )
        );
    }

    /**
     * GET /api/courses/category/{category} - 카테고리별 활성 강의
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<CourseDto.ApiResponse<List<CourseDto.CourseResponse>>> getCoursesByCategory(
            @PathVariable Course.Category category) {
        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(courseService.getCoursesByCategory(category))
        );
    }

    /**
     * GET /api/courses/internal/exists/{id} - 강의 존재 여부 (Enrollment Service 호출)
     */
    @GetMapping("/internal/exists/{id}")
    public ResponseEntity<Boolean> existsCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.existsCourse(id));
    }

    /**
     * GET /api/courses/internal/{id} - 업무 요청 내부 조회
     * - Enrollment 및 Recommend Service의 응답 조립 시 사용
     * - 래퍼 없이 CourseResponse만 직접 반환
     */
    @GetMapping("/internal/{id}")
    public ResponseEntity<CourseDto.CourseResponse> getCourseInternal(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourse(id));
    }

    /**
     * GET /api/courses/internal/recommend - 추천 서비스용 미수강 강의 조회
     * category: 카테고리, excludeIds: 이미 수강한 강의 ID 목록
     */
    @GetMapping("/internal/recommend")
    public ResponseEntity<List<CourseDto.CourseResponse>> getRecommendCourses(
            @RequestParam Course.Category category,
            @RequestParam(defaultValue = "") List<Long> excludeIds) {
        return ResponseEntity.ok(courseService.getRecommendCourses(category, excludeIds));
    }

    /**
     * POST /api/courses/{id}/analyze - 업무 요청 분석
     */
    @PostMapping("/{id}/analyze")
    public ResponseEntity<
            CourseDto.ApiResponse<CourseDto.AnalysisResponse>
    > analyzeCourse(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long instructorId
    ) {
        CourseDto.AnalysisResponse response =
                courseService.analyzeCourse(id, instructorId);

        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(response)
        );
    }

    // GET /api/courses/my - 내 업무 요청 목록
    @GetMapping("/my")
    public ResponseEntity<
            CourseDto.ApiResponse<List<CourseDto.CourseResponse>>
    > getMyCourses(
            @RequestHeader("X-User-Id") Long instructorId
    ) {
        return ResponseEntity.ok(
                CourseDto.ApiResponse.success(
                        courseService.getMyCourses(instructorId)
                )
        );
    }
}
