package com.lecture.course.llm;

/** Mission fields that are safe and necessary to send to the LLM. */
public record AnalysisMission(
        Long courseId,
        String title,
        String description,
        String category,
        String usagePeriod,
        String dataSensitivity
) {
}
