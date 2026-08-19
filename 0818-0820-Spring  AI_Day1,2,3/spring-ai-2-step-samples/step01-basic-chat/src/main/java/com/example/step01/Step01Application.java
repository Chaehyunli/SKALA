package com.example.step01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * STEP 01 - 애플리케이션 진입점(Entry Point)
 *
 * <p>{@code @SpringBootApplication}은 아래 3개 애노테이션을 합친 메타 애노테이션이다.
 * <ul>
 *   <li>{@code @SpringBootConfiguration} : 이 클래스를 Bean 설정 클래스로 인식</li>
 *   <li>{@code @EnableAutoConfiguration} : classpath에 있는 starter를 보고 Bean을 자동 구성.
 *       여기서는 {@code spring-ai-starter-model-openai}가 감지되어
 *       {@code OpenAiChatModel}과 {@code ChatClient.Builder}가 자동으로 등록된다.</li>
 *   <li>{@code @ComponentScan} : 이 클래스가 속한 패키지(com.example.step01) 이하를 스캔해
 *       {@code @RestController}, {@code @Component} 등을 Bean으로 등록</li>
 * </ul>
 *
 * <p>즉, 개발자가 OpenAI 연동 코드를 직접 작성하지 않아도
 * application.yml의 api-key / model 설정만으로 ChatClient를 주입받을 수 있다.
 */
@SpringBootApplication
public class Step01Application {

    /**
     * 내장 톰캣을 띄우고 Spring ApplicationContext를 초기화한다.
     * 포트는 application.yml의 {@code server.port}(8101)를 따른다.
     */
    public static void main(String[] args) {
        SpringApplication.run(Step01Application.class, args);
    }
}
