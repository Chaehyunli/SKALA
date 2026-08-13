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

@Slf4j
@Aspect
@Component
public class MetricsAnnotationAspect {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Pointcut("@annotation(com.sk.skala.myapp.aspect.Metrics)")
    public void metricsAnnotation() {
    }

    @Before("metricsAnnotation()")
    public void logControllerStart(JoinPoint joinPoint) {
        String targetName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.info("[AOP Annotation Before] @Metrics 시작 - 대상: {}, 메소드: {}", targetName, methodName);
    }

    @Around("metricsAnnotation()")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();

        LocalDateTime startTime = LocalDateTime.now();
        long startMillis = System.currentTimeMillis();

        log.info("[AOP Annotation Around] {} 메소드 시작: {}", methodName, startTime.format(TIME_FORMATTER));
        try {
            return joinPoint.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - startMillis;
            LocalDateTime endTime = LocalDateTime.now();
            log.info("[AOP Annotation Around] {} 메소드 종료: {} | 총 소요 시간: {} ms",
                    methodName, endTime.format(TIME_FORMATTER), elapsed);
        }
    }
}
