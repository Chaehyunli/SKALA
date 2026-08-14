package com.sk.skala.myapp.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;

import com.sk.skala.myapp.repository.UserRepository;
import com.sk.skala.myapp.service.UserService;

import lombok.extern.slf4j.Slf4j;

/**
 * ProxyFactory를 이용한 수동 AOP 프록시 생성 예시.
 *
 * MetricsAspect(@Aspect 방식)와 동일한 기능(실행 시간 측정)을
 * Spring AOP 저수준 API인 ProxyFactory로 직접 구현한 것이다.
 *
 * 참고용 구현으로, @Configuration이 없어 Spring 빈으로 등록되지 않는다.
 * (동시에 활성화하면 @Aspect 방식과 이중으로 프록시가 걸리기 때문)
 */
@Slf4j
public class UserServiceConfig {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Bean
    public UserService userService(UserRepository userRepository) {
        UserService userService = new UserService(userRepository);

        ProxyFactory proxyFactory = new ProxyFactory(userService);
        proxyFactory.setProxyTargetClass(true);

        proxyFactory.addAdvice((MethodInterceptor) invocation -> {
            String methodName = invocation.getMethod().getName();

            LocalDateTime startTime = LocalDateTime.now();
            long startMillis = System.currentTimeMillis();

            log.info("[Proxy] {} 메소드 시작: {}", methodName, startTime.format(TIME_FORMATTER));
            try {
                return invocation.proceed();
            } finally {
                long elapsed = System.currentTimeMillis() - startMillis;
                LocalDateTime endTime = LocalDateTime.now();
                log.info("[Proxy] {} 메소드 종료: {} | 총 소요 시간: {} ms",
                        methodName, endTime.format(TIME_FORMATTER), elapsed);
            }
        });

        return (UserService) proxyFactory.getProxy();
    }
}
