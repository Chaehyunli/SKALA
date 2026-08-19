package com.example.step04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * STEP 04 - 애플리케이션 진입점
 *
 * <p>이 STEP부터는 ChatClient를 컨트롤러 안에서 만들지 않고
 * {@link AiConfig}에서 Bean으로 만들어 공유한다. 포트는 8104.
 */
@SpringBootApplication
public class Step04Application {
    public static void main(String[] args) {
        SpringApplication.run(Step04Application.class, args);
    }
}
