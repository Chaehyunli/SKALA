package com.sk.skala.myapp.aspect;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @Aspect를 활용한 메트릭(실행 시간) 측정 예시.
 *
 * UserService의 모든 public 메소드 호출 시
 * 시작 시각, 종료 시각, 총 소요 시간을 로그로 출력한다.
 *
 * UserServiceConfig의 ProxyFactory 방식과 동일한 기능을
 * Spring AOP 표준 방식(@Aspect)으로 구현한 것이다.
 */
@Slf4j
@Aspect
@Component
public class MetricsAspect {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Before("execution(* com.sk.skala.myapp.service.UserService.*(..))")    
    public void logControllerStart(JoinPoint joinPoint) {
        String targetName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.info("[AOP before] @Metrics 시작 - 대상: {}, 메소드: {}", targetName, methodName);
    }

    /**
     * UserService의 모든 메소드에 Around Advice 적용.
     *
     * Pointcut 표현식:
     *   execution(* com.sk.skala.myapp.service.UserService.*(..))
     *   - 반환 타입: * (모든 타입)
     *   - 클래스:   UserService
     *   - 메소드:   * (모든 메소드)
     *   - 파라미터: .. (모든 파라미터)
     */
    @Around("execution(* com.sk.skala.myapp.service.UserService.*(..))")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();

        LocalDateTime startTime = LocalDateTime.now();
        long startMillis = System.currentTimeMillis();

        log.info("[AOP] {} 메소드 시작: {}", methodName, startTime.format(TIME_FORMATTER));
        try {
            return joinPoint.proceed(); // 실제 메소드 실행
        } finally {
            long elapsed = System.currentTimeMillis() - startMillis;
            LocalDateTime endTime = LocalDateTime.now();
            log.info("[AOP] {} 메소드 종료: {} | 총 소요 시간: {} ms",
                    methodName, endTime.format(TIME_FORMATTER), elapsed);
        }
    }
}
