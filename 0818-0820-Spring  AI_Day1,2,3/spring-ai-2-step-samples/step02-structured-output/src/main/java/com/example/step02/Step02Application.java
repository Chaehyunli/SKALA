package com.example.step02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * STEP 02 - 애플리케이션 진입점
 *
 * <p>STEP 01과 구조는 같다. 달라지는 것은 컨트롤러가 String이 아니라
 * <b>Java 객체(record)</b>를 반환한다는 점이다.
 * 포트는 8102 (application.yml).
 */
@SpringBootApplication
public class Step02Application {
    public static void main(String[] args) {
        SpringApplication.run(Step02Application.class, args);
    }
}
