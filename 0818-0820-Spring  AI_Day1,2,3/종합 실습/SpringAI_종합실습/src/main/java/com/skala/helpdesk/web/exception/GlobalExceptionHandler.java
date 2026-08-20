package com.skala.helpdesk.web.exception;

import com.skala.helpdesk.service.AdminAuthException;
import com.skala.helpdesk.service.ComplianceTicketNotFoundException;
import com.skala.helpdesk.web.dto.common.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** 예외를 응답으로 바꾸는 자리는 한 곳이다. AI가 실패해도 화면은 살린다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ComplianceTicketNotFoundException.class)
    ResponseEntity<ErrorResponse> ticketNotFound(ComplianceTicketNotFoundException e) {
        return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage(), null));
    }

    @ExceptionHandler(AdminAuthException.class)
    ResponseEntity<ErrorResponse> adminAuth(AdminAuthException e) {
        return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage(), null));
    }

    // UserSessionService가 던지는 401/403/404 등 — 아래 Exception.class 핸들러가 먼저 가로채 503으로
    // 뭉개지 않도록 상태 코드를 그대로 살려서 응답한다.
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> statusException(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason(), null));
    }

    @ExceptionHandler(Exception.class)                     // 모델 오류·타임아웃·인제스트 IO 오류 포함
    ResponseEntity<ErrorResponse> unexpected(Exception e) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("[{}] 요청 처리 실패", traceId, e);        // 상세는 로그에만
        return ResponseEntity.status(503).body(new ErrorResponse(
                "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.", traceId));
    }
}
