package com.sk.skala.myapp.aspect;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @Metrics 애노테이션을 활용한 메트릭(실행 시간) 측정 예시.
 *
 * execution 표현식으로 패키지/클래스를 직접 지정하는 대신,
 * {@link Metrics} 애노테이션이 붙은 메소드에만 Advice를 적용한다.
 *
 * MetricsAspect(execution 기반)와 동일한 기능을
 * 애노테이션 기반 Pointcut(@annotation)으로 구현한 것이다.
 */
@Slf4j
@Aspect
@Component
public class MetricsAnnotationAspect {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * @Metrics 애노테이션이 붙은 메소드를 대상으로 하는 Pointcut.
     *
     * Pointcut 표현식:
     *   @annotation(com.sk.skala.myapp.aspect.Metrics)
     *   - 메소드에 @Metrics 애노테이션이 붙어 있는 경우에만 매칭된다.
     */
    @Pointcut("@annotation(com.sk.skala.myapp.aspect.Metrics)")
    public void metricsAnnotation() {
    }

    /**
     * @Metrics가 붙은 메소드에 Around Advice 적용하여 실행 시간을 측정한다.
     */
    @Around("metricsAnnotation()")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();

        LocalDateTime startTime = LocalDateTime.now();
        long startMillis = System.currentTimeMillis();

        log.info("[AOP Annotation Around] {} 메소드 시작: {}", methodName, startTime.format(TIME_FORMATTER));
        try {
            return joinPoint.proceed(); // 실제 메소드 실행
        } finally {
            long elapsed = System.currentTimeMillis() - startMillis;
            LocalDateTime endTime = LocalDateTime.now();
            log.info("[AOP Annotation Around] {} 메소드 종료: {} | 총 소요 시간: {} ms",
                    methodName, endTime.format(TIME_FORMATTER), elapsed);
        }
    }
}
