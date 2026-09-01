package com.lecture.course.service;

import com.lecture.course.analysis.ResolvedAnalysisResult;
import com.lecture.course.dto.CourseDto;
import com.lecture.course.entity.Course;
import com.lecture.course.llm.AnalysisMission;
import com.lecture.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** Persists analysis state in short transactions so remote LLM calls never hold a DB transaction. */
@Service
@RequiredArgsConstructor
public class CourseAnalysisWriteService {

    private final CourseRepository courseRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalysisMission startAnalysis(Long courseId, Long userId) {
        Course course = findOwnedCourse(courseId, userId);
        course.startAnalysis();
        return new AnalysisMission(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCategory().name(),
                course.getUsagePeriod(),
                course.getDataSensitivity());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CourseDto.AnalysisResponse completeAnalysis(
            Long courseId,
            Long userId,
            ResolvedAnalysisResult analysis
    ) {
        Course course = findOwnedCourse(courseId, userId);
        List<Course.AnalysisAgent> agentList = analysis.agentList().stream()
                .map(agent -> Course.AnalysisAgent.builder()
                        .agentCode(agent.agentCode())
                        .permissions(toEntityPermissions(agent.permissions()))
                        .excludedPermissions(toEntityPermissions(agent.excludedPermissions()))
                        .build())
                .toList();
        course.completeAnalysis(agentList, analysis.riskLevel(), analysis.summary());
        return CourseDto.AnalysisResponse.from(course);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failAnalysis(Long courseId, Long userId) {
        Course course = findOwnedCourse(courseId, userId);
        course.failAnalysis();
    }

    private List<Course.AnalysisPermission> toEntityPermissions(
            List<ResolvedAnalysisResult.AnalysisPermission> permissions
    ) {
        return permissions.stream().map(permission -> Course.AnalysisPermission.builder()
                .code(permission.code())
                .label(permission.label())
                .description(permission.description())
                .reason(permission.reason())
                .build()).toList();
    }

    private Course findOwnedCourse(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("업무 요청을 찾을 수 없습니다: " + courseId));
        if (!Objects.equals(course.getInstructorId(), userId)) {
            throw new IllegalArgumentException("다른 사용자의 업무 요청입니다");
        }
        return course;
    }
}
