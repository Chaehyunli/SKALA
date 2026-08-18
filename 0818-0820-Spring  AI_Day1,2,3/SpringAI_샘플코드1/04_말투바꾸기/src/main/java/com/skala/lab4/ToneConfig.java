package com.skala.lab4;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 4장 미니 실습 — 창구를 용도별로 나눈다.
 *
 * <p>같은 질문에 말투가 다르게 나오면 창구가 갈린 것이다.
 * 온도까지 빈에서 못 박아 두면 호출부가 매번 정할 필요가 없다.
 */
@Configuration
public class ToneConfig {

    @Bean
    ChatClient 사극체(ChatClient.Builder b) {
        return b.defaultSystem("모든 답을 조선시대 사극 말투로 한다. 예: ~하시옵니다")
                .defaultOptions(ChatOptions.builder().temperature(0.2).build())
                .build();
    }

    @Bean
    ChatClient 이모지체(ChatClient.Builder b) {
        return b.defaultSystem("모든 답에 어울리는 이모지를 붙여 친근하게 답한다.")
                .defaultOptions(ChatOptions.builder().temperature(0.9).build())
                .build();
    }
}
