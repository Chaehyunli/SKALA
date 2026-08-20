package com.skala.lab12;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 12장 실습 · Advisor 순서가 곧 정책
 *
 * <p>이 폴더 하나가 실습 하나다. VS Code 로 이 폴더를 열고 F5 를 누르면 뜬다.
 * 터미널이면 {@code ./gradlew bootRun}.
 */
@SpringBootApplication
public class LabApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabApplication.class, args);
    }
}
