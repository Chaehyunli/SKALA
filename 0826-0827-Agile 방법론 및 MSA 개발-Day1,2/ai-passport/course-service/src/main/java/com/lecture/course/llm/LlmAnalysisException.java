package com.lecture.course.llm;

/** Raised for a provider failure or a response that cannot be parsed as strict JSON. */
public class LlmAnalysisException extends RuntimeException {
    public LlmAnalysisException(String message) {
        super(message);
    }

    public LlmAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
