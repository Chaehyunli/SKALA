package com.example.step09;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * STEP 09 - 애플리케이션 진입점
 *
 * <p>이 모듈의 주인공은 main()이 아니라 <b>테스트 코드</b>({@code RagEvaluationTest})다.
 * 이 클래스는 {@code @SpringBootTest}가 컨텍스트를 띄울 때 기준점으로 삼는
 * 설정 클래스 역할을 한다.
 *
 * <p>웹 스타터가 없어 서버 포트를 열지 않으므로 application.yml에도 server.port가 없다.
 *
 * <p><b>실행 전 준비</b> : STEP 06과 같은 pgvector가 필요하다.
 * <pre>
 *   cd ../step06-rag-pgvector &amp;&amp; docker compose up -d
 *   cd ../ &amp;&amp; ./gradlew :step09-evaluation:test
 * </pre>
 */
@SpringBootApplication
public class Step09Application {
    public static void main(String[] args) {
        SpringApplication.run(Step09Application.class, args);
    }
}
