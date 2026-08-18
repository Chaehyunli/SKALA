package com.skala.lab2;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 3장 미니 실습 — AI 작명 봇과 고장 재현.
 *
 * <p>키를 지우고 재시작하면 기동 자체가 실패한다
 * ({@code Could not resolve placeholder 'OPENAI_API_KEY'}).
 * 그 화면을 한 번 보고 넘어가는 것이 이 실습의 절반이다.
 */
@RestController
@Tag(name = "3장 실습 · 작명 봇")
public class NamingBot {

    private final ChatClient chat;

    public NamingBot(ChatClient.Builder builder) {
        this.chat = builder                                    // 창구를 하나 만든다
                .defaultSystem("""
                        너는 작명가다. 주어진 키워드로 팀 이름을 3개 제안한다.
                        각 이름 뒤에 한 줄 이유를 붙인다. 한국어로 답한다.
                        """)
                .build();
    }

    @GetMapping("/lab2/name")                                  // 말을 건다
    @Operation(summary = "키워드로 팀 이름 3개", description = "창작이라 매번 조금씩 다르다. 그게 정상이다.")
    public String name(@RequestParam String keyword) {
        return chat.prompt()
                .user(u -> u.text("키워드: {k}").param("k", keyword))
                .call().content();                             // 답을 받는다
    }
}
