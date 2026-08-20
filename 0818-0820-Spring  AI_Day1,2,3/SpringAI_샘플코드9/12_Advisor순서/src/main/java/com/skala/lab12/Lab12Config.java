package com.skala.lab12;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 12장 미니 실습 — Advisor 조립. 순서가 정책이다.
 *
 * <p>실험: 차단(SafeGuard)의 order 를 100 → 250 으로 바꾸고 위험한 문장을 한 번 보낸 뒤
 * {@code GET /lab12/history} 를 보면, 막았어야 할 문장이 기록에 남아 있다.
 * 확인한 뒤에는 반드시 되돌린다.
 */
@Configuration
public class Lab12Config {

    /** 실험용으로 바꿔 볼 값. 100 이면 기억보다 앞(정상), 250 이면 기억보다 뒤(사고). */
    static final int 차단_ORDER = 100;

    @Bean
    ChatClient lab12ChatClient(ChatClient.Builder builder,
                              ChatMemory chatMemory,
                              이모지Advisor 이모지) {
        return builder
                .defaultAdvisors(
                        SafeGuardAdvisor.builder()                       // order 100 차단
                                .sensitiveWords(List.of("주민등록번호", "카드번호", "비밀번호"))
                                .failureResponse("민감정보가 포함된 요청은 처리할 수 없습니다.")
                                .order(차단_ORDER)
                                .build(),
                        MessageChatMemoryAdvisor.builder(chatMemory)     // order 200 기억
                                .order(200)
                                .build(),
                        이모지)                                           // order 250
                .build();
    }
}
