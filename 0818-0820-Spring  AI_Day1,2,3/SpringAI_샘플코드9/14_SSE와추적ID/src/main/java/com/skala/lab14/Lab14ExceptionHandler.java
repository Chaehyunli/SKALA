package com.skala.lab14;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 14장 미니 실습 — 실패도 서비스답게.
 *
 * <p>상세는 로그에만 남기고 사용자에게는 추적 ID 만 준다.
 * "안 돼요, traceId 3f9a1c2b 예요" 한마디면 로그에서 5초 만에 그 요청을 찾는다.
 */
@RestControllerAdvice(basePackages = "com.skala.springai.samples.lab14")
public class Lab14ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(Lab14ExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> 실패(Exception e) {
        String 추적 = UUID.randomUUID().toString().substring(0, 8);
        log.error("[{}] 처리 실패", 추적, e);
        return ResponseEntity.status(503).body(Map.of(
                "message", "잠시 후 다시 시도해 주세요.", "traceId", 추적));
    }
}
