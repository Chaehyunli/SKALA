package com.example.step07;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * STEP 07 - 애플리케이션 진입점 (포트 8107)
 *
 * <p>{@code @ComponentScan}이 {@link CourseTools}({@code @Component})를 Bean으로 등록하고,
 * 그 Bean이 {@code ToolController}에 주입된다.
 */
@SpringBootApplication
public class Step07Application {
    public static void main(String[] args) {
        SpringApplication.run(Step07Application.class, args);
    }
}
