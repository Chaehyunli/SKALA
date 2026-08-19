package com.example.step05;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * STEP 05 - 애플리케이션 진입점 (포트 8105)
 *
 * <p>대화 이력을 메모리에 보관하므로, 애플리케이션을 재시작하면 기억은 모두 사라진다.
 */
@SpringBootApplication
public class Step05Application {
    public static void main(String[] args) {
        SpringApplication.run(Step05Application.class, args);
    }
}
