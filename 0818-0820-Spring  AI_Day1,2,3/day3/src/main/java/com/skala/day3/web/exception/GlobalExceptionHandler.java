package com.skala.day3.web.exception;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.skala.day3.service.AdminAuthException;
import com.skala.day3.service.OrderNotFoundException;
import com.skala.day3.service.TicketNotFoundException;
import com.skala.day3.web.dto.common.ErrorResponse;

/**
 * 예외를 응답으로 바꾸는 자리는 한 곳이다 — Lab1·Lab2·Lab3 공통. AI 가 실패해도 화면은 살린다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(OrderNotFoundException e) {
        return ResponseEntity.status(404).body(new ErrorResponse("주문을 찾을 수 없습니다.", null));
    }

    @ExceptionHandler(TicketNotFoundException.class)
    ResponseEntity<ErrorResponse> ticketNotFound(TicketNotFoundException e) {
        return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage(), null));
    }

    @ExceptionHandler(AdminAuthException.class)
    ResponseEntity<ErrorResponse> adminAuth(AdminAuthException e) {
        return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)                     // 모델 오류·타임아웃·인제스트 IO 오류 포함
    ResponseEntity<ErrorResponse> unexpected(Exception e) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("[{}] 요청 처리 실패", traceId, e);        // 상세는 로그에만
        return ResponseEntity.status(503).body(new ErrorResponse(
                "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.", traceId));
    }
}
