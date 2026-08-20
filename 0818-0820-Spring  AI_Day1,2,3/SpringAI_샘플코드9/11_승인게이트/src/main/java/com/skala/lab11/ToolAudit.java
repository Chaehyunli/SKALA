package com.skala.lab11;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 11장 미니 실습 — 도구마다 로그를 넣지 않는다.
 *
 * <p>{@code @Tool} 이 붙은 메서드 전부를 한 곳에서 기록한다.
 * 도구가 늘어나도 감사 코드는 이 파일 하나로 끝난다.
 */
@Aspect
@Component
public class ToolAudit {

    private static final Logger log = LoggerFactory.getLogger("AI_TOOL_AUDIT");

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object 기록(ProceedingJoinPoint p) throws Throwable {
        String 이름 = p.getSignature().getName();
        try {
            Object r = p.proceed();
            log.info("[감사] {} {} 성공", 사용자(), 이름);
            return r;
        } catch (Exception e) {
            log.warn("[감사] {} {} 실패 {}", 사용자(), 이름, e.getMessage());
            throw e;
        }
    }

    /** 실무에서는 SecurityContext 에서 꺼낸다. 실습에서는 고정값으로 둔다. */
    private String 사용자() {
        return "user1";
    }
}
