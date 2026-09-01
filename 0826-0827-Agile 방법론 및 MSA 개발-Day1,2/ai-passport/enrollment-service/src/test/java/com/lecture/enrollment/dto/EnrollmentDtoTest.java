package com.lecture.enrollment.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lecture.enrollment.entity.Enrollment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requestJsonMatchesAgentListRequestContract() throws Exception {
        ClassPathResource resource = new ClassPathResource("static/request.json");

        EnrollmentDto.EnrollRequest request;
        try (InputStream inputStream = resource.getInputStream()) {
            request = objectMapper.readValue(inputStream, EnrollmentDto.EnrollRequest.class);
        }

        Set<ConstraintViolation<EnrollmentDto.EnrollRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
        assertThat(request.getCourseId()).isEqualTo(12L);
        assertThat(request.getAgentList()).hasSize(3);
        assertThat(request.getAgentList())
                .extracting(agent -> agent.getAgentCode())
                .containsExactly("REVENUE_ANALYST", "CUSTOMER_SUPPORT", "COMMON_AGENT");
        assertThat(request.getAgentList())
                .allSatisfy(agent -> assertThat(agent.getPermissions()).isNotEmpty());
    }

    @Test
    void enrollmentResponseKeepsAllAgents() throws Exception {
        ClassPathResource resource = new ClassPathResource("static/request.json");

        EnrollmentDto.EnrollRequest request;
        try (InputStream inputStream = resource.getInputStream()) {
            request = objectMapper.readValue(inputStream, EnrollmentDto.EnrollRequest.class);
        }

        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .userId(2L)
                .courseId(request.getCourseId())
                .agentList(request.getAgentList())
                .riskLevel(request.getRiskLevel())
                .summary(request.getSummary())
                .status(Enrollment.Status.READY_FOR_APPROVAL)
                .build();

        EnrollmentDto.EnrollmentResponse response = EnrollmentDto.EnrollmentResponse.from(enrollment);

        assertThat(response.getAgentList()).hasSize(3);
        assertThat(response.getAgentList()).containsExactlyElementsOf(request.getAgentList());
    }
}
