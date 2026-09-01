package com.lecture.course.service;

import com.lecture.course.dto.CourseDto;
import com.lecture.course.entity.Course;
import com.lecture.course.llm.AnalysisMission;
import com.lecture.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private static final BigDecimal ANALYSIS_CREDIT_COST = BigDecimal.valueOf(120);

    private final CourseRepository courseRepository;
    private final LlmCourseAnalysisService llmCourseAnalysisService;
    private final CourseAnalysisWriteService courseAnalysisWriteService;
    private final UserCreditClient userCreditClient;

    /**
     * 업무 요청 생성
     * 기존 강의 모델과의 호환을 위해 instructorId에 요청자 ID를 저장하고 가격은 0으로 설정
     */
    @Transactional
    public CourseDto.CourseResponse createCourse(
            CourseDto.CreateRequest request,
            Long instructorId
    ) {
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(BigDecimal.ZERO)
                .instructorId(instructorId)
                .usagePeriod(request.getUsagePeriod())
                .dataSensitivity(request.getDataSensitivity())
                .status(Course.Status.CREATED)
                .build();

        return CourseDto.CourseResponse.from(courseRepository.save(course));
    }

    /**
     * 내부 서비스용 업무 요청 단건 조회
     */
    public CourseDto.CourseResponse getCourse(Long id) {
        Course course = findCourseById(id);
        return CourseDto.CourseResponse.from(course);
    }

    // 요청자 본인의 업무 요청 상세 조회
    public CourseDto.CourseResponse getCourse(
            Long id,
            Long instructorId
    ) {
        Course course = findCourseById(id);
        validateOwner(course, instructorId);
        return CourseDto.CourseResponse.from(course);
    }

    /**
     * 전체 활성 강의 목록 조회
     */
    public List<CourseDto.CourseResponse> getAllCourses() {
        return courseRepository.findByStatus(Course.Status.ACTIVE).stream()
                .map(CourseDto.CourseResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 강의 조회
     */
    public List<CourseDto.CourseResponse> getCoursesByCategory(Course.Category category) {
        return courseRepository.findByCategoryAndStatus(category, Course.Status.ACTIVE).stream()
                .map(CourseDto.CourseResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 강의 존재 여부 확인 (Enrollment Service → Course Service REST 호출용)
     */
    public boolean existsCourse(Long id) {
        return courseRepository.existsById(id);
    }

    /**
     * 추천 서비스용: 카테고리별 미수강 강의 조회
     * - excludeCourseIds: 이미 수강한 강의 ID 목록
     */
    public List<CourseDto.CourseResponse> getRecommendCourses(
            Course.Category category, List<Long> excludeCourseIds) {

        List<Course> courses = excludeCourseIds.isEmpty()
                ? courseRepository.findByCategoryAndStatus(category, Course.Status.ACTIVE)
                : courseRepository.findByCategoryAndStatusAndIdNotIn(
                        category, Course.Status.ACTIVE, excludeCourseIds);

        // 최신 생성순 정렬
        return courses.stream()
                .sorted(Comparator.comparing(
                        Course::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(CourseDto.CourseResponse::from)
                .collect(Collectors.toList());
    }

    private Course findCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "업무 요청을 찾을 수 없습니다: " + id
                ));
    }

    /** LLM의 1차 Agent 선택과 2차 권한 선택을 실행하고 결과를 다중 Agent JSON으로 저장한다. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CourseDto.AnalysisResponse analyzeCourse(
            Long courseId,
            Long instructorId
    ) {
        // Validate ownership before charging, then charge on the server so a client cannot bypass it.
        validateOwner(findCourseById(courseId), instructorId);
        userCreditClient.deductAnalysisCredit(instructorId, ANALYSIS_CREDIT_COST);
        AnalysisMission mission = courseAnalysisWriteService.startAnalysis(courseId, instructorId);
        try {
            return courseAnalysisWriteService.completeAnalysis(
                    courseId,
                    instructorId,
                    llmCourseAnalysisService.analyze(mission));
        } catch (RuntimeException e) {
            courseAnalysisWriteService.failAnalysis(courseId, instructorId);
            throw e;
        }
    }

    // 업무 요청 소유자 확인
    private void validateOwner(Course course, Long instructorId) {
        if (!Objects.equals(course.getInstructorId(), instructorId)) {
            throw new IllegalArgumentException(
                    "다른 사용자의 업무 요청입니다"
            );
        }
    }

    // 본인이 생성한 업무 요청 목록 조회
    public List<CourseDto.CourseResponse> getMyCourses(Long instructorId) {
        return courseRepository
                .findByInstructorIdOrderByCreatedAtDesc(instructorId)
                .stream()
                .map(CourseDto.CourseResponse::from)
                .collect(Collectors.toList());
    }
}
