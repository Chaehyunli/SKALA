package com.skala.day3.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 용도별로 빈을 나눈다 — Lab1(요약)·Lab2(문서 Q&A) 전용 ChatClient 를 한 곳에 모은다.
 *
 * <p>호출부마다 온도·토큰 상한을 정하게 두면 누군가는 기본값(0.7)으로 부르고,
 * 그날부터 답이 매번 달라진다. 용도별로 빈에서 못 박아 둔다.
 */
@Configuration
public class AiConfig {

    @Bean
    ChatClient lab1SummaryChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        너는 이커머스 주문 상담 도우미다.
                        주어진 주문 정보만 사용해 한국어 한 문장으로 요약한다.
                        추측하지 않는다. 정보가 부족하면 "정보가 부족합니다"라고 답한다.
                        """)
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.0)   // 요약은 매번 같아야 한다
                        .maxTokens(120)     // 비용 상한 — 길게 쓸 이유가 없다
                        .build())
                .build();
    }

    @Bean
    ChatClient lab2AskChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        아래 [근거]만 사용해 답한다. 근거에 없으면 "확인되지 않습니다"라고 답한다.
                        추측하지 않는다. 답변 끝에 사용한 출처를 [출처: 파일명] 형식으로 남긴다.
                        """)
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.0)   // 같은 질문·근거면 같은 답이 나와야 한다
                        .build())
                .build();
    }
}
