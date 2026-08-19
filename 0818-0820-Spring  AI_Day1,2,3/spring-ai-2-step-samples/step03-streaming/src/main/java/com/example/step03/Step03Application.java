package com.example.step03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * STEP 03 - 애플리케이션 진입점
 *
 * <p>이 모듈만 {@code spring-boot-starter-web(mvc)}가 아니라
 * <b>spring-boot-starter-webflux</b>를 사용한다(build.gradle 참고).
 * 스트리밍 응답 타입인 {@code Flux}를 다루려면 리액티브 스택이 필요하기 때문이다.
 * 따라서 내장 서버도 톰캣이 아니라 <b>Netty</b>가 뜬다. 포트는 8103.
 */
@SpringBootApplication
public class Step03Application {
    public static void main(String[] args) {
        SpringApplication.run(Step03Application.class, args);
    }
}
