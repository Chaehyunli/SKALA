package com.example.step04;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * STEP 04 - Advisor 설정
 *
 * <p><b>Advisor란?</b><br>
 * ChatClient의 요청/응답 파이프라인에 끼어드는 <b>인터셉터(가로채기 계층)</b>다.
 * Servlet Filter나 Spring AOP를 떠올리면 이해가 쉽다.
 *
 * <pre>
 *   user 요청
 *      ↓  [Advisor 1] → [Advisor 2] → ... (요청 가공: 대화 이력 주입, 문서 검색 결과 추가 등)
 *   LLM 호출
 *      ↑  ... ← [Advisor 2] ← [Advisor 1] (응답 가공: 로깅, 후처리)
 *   답변 반환
 * </pre>
 *
 * <p>Spring AI의 주요 기능 대부분이 Advisor로 제공된다.
 * <ul>
 *   <li>{@code SimpleLoggerAdvisor} : 요청/응답 로깅 (이번 STEP)</li>
 *   <li>{@code MessageChatMemoryAdvisor} : 대화 기억 → STEP 05</li>
 *   <li>{@code QuestionAnswerAdvisor} : RAG 문서 검색 주입 → STEP 06</li>
 * </ul>
 * 필요한 기능을 <b>조합(compose)</b>해서 쓰는 것이 Advisor의 핵심 가치다.
 */
@Configuration
public class AiConfig {

    /**
     * 완성된 ChatClient를 Bean으로 등록한다.
     *
     * <p>컨트롤러마다 Builder로 각자 만들면 Advisor 설정이 중복되고 누락되기 쉽다.
     * 이렇게 한 곳에서 조립해 두면 모든 컨트롤러가 동일한 정책(로깅 등)을 공유한다.
     *
     * <p>참고: 자동 구성이 등록해 둔 것은 {@code ChatClient.Builder}이지
     * {@code ChatClient}가 아니므로, 여기서 ChatClient를 Bean으로 올려도 충돌하지 않는다.
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                // defaultAdvisors : 이 ChatClient의 모든 호출에 항상 적용할 Advisor 목록.
                // 특정 요청에만 붙이고 싶으면 호출부에서 .advisors(...)를 쓴다.
                //
                // SimpleLoggerAdvisor는 LLM에 실제로 전달된 최종 프롬프트와
                // 원본 응답을 DEBUG 레벨로 남긴다. 프롬프트가 의도대로 조립됐는지
                // 확인하는 가장 빠른 디버깅 수단이다.
                //
                // 로그를 보려면 application.yml에서 레벨을 반드시 낮춰야 한다.
                //   logging.level.org.springframework.ai.chat.client.advisor: DEBUG
                //
                // 주의: 프롬프트 전문이 로그에 남으므로 운영 환경에서는
                // 개인정보 유출 위험을 고려해 켜고 끄기를 결정해야 한다.
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
