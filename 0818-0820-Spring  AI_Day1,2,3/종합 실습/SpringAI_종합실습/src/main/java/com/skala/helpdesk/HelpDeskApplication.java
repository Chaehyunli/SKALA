package com.skala.helpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 종합 실습 · SKALA HelpDesk AI — 해외주식 모의투자 상담 에이전트.
 * RAG(신고·거래 규정) + Tool(시세·환율 조회, 매수·매도) + Advisor(기억·안전·관찰)을 조립한다.
 *
 * <p>VS Code로 이 폴더를 열고 F5를 누르면 뜬다. 터미널이면 {@code ./gradlew bootRun}.
 * 실행 전 docker-compose.yml로 pgvector를 띄우고 .env를 채워야 한다 — README 참고.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync   // ComplianceMailService.notify()가 SMTP 전송으로 매수 체결 응답을 지연시키지 않도록 비동기 실행한다.
public class HelpDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelpDeskApplication.class, args);
    }
}
