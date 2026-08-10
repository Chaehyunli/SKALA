package com.sk.skala.myapp.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sk.skala.myapp.repository.UserRepository;
import com.sk.skala.myapp.service.UserService;

import lombok.extern.slf4j.Slf4j;

/**
 * @Configuration + @Bean을 활용한 Proxy 패턴 예시.
 *
 * UserService를 직접 @Service로 등록하지 않고,
 * 이 설정 클래스에서 CGLIB 기반 프록시 객체로 감싸 스프링 컨테이너에 등록한다.
 *
 * 프록시는 메소드 이름이 "get"으로 시작하는 getter 메소드 호출 시
 * 시작 시각, 종료 시각, 총 소요 시간을 로그로 출력한다.
 */
@Slf4j
@Configuration
public class UserServiceConfig {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Bean
    public UserService userService(UserRepository userRepository) {
        // 1. 실제 대상(Target) 객체 생성
        UserService userService = new UserService(userRepository);

        // 2. ProxyFactory를 이용해 CGLIB 기반 프록시 생성
        ProxyFactory proxyFactory = new ProxyFactory(userService);
        proxyFactory.setProxyTargetClass(true); // 인터페이스 없이 CGLIB 서브클래스 방식 사용

        // 3. getter 메소드에 대한 실행 시간 측정 Interceptor 등록
        proxyFactory.addAdvice(new MethodInterceptor() {
            @Override
            public Object invoke(MethodInvocation invocation) throws Throwable {
                String methodName = invocation.getMethod().getName();

                // 모든 메소드에 시간 측정 적용
                LocalDateTime startTime = LocalDateTime.now();
                long startMillis = System.currentTimeMillis();

                log.info("[Proxy] {} 메소드 시작: {}", methodName, startTime.format(TIME_FORMATTER));
                try {
                    return invocation.proceed(); // 실제 메소드 실행
                } finally {
                    long elapsed = System.currentTimeMillis() - startMillis;
                    LocalDateTime endTime = LocalDateTime.now();
                    log.info("[Proxy] {} 메소드 종료: {} | 총 소요 시간: {} ms",
                            methodName, endTime.format(TIME_FORMATTER), elapsed);
                }
            }
        });

        return (UserService) proxyFactory.getProxy();
    }
}
