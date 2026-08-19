package com.skala.day2.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 문서 Q&A(Lab2) 전용 ChatClient 빈 — 근거 답변 전용.
 *
 * <p>Lab1(주문 요약)과 용도가 다르므로 빈을 따로 둔다. 근거에 없는 내용을 추측해 답하지 않도록
 * 시스템 프롬프트에 거절 지시를 못 박아 둔다.
 */
@Configuration
public class Lab2AiConfig {

    @Bean
    ChatClient askChatClient(ChatClient.Builder builder) {
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
