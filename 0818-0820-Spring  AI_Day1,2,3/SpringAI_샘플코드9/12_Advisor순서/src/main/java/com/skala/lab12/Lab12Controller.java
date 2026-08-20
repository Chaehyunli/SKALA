package com.skala.lab12;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 12장 미니 실습 — 답 끝의 이모지로 Advisor 를, history 로 순서를 확인한다.
 */
@RestController
@Tag(name = "12장 실습 · Advisor 순서")
public class Lab12Controller {

    private final ChatClient chat;
    private final ChatMemory memory;

    public Lab12Controller(@Qualifier("lab12ChatClient") ChatClient chat, ChatMemory memory) {
        this.chat = chat;
        this.memory = memory;
    }

    @GetMapping("/lab12/ask")
    @Operation(summary = "이모지가 붙는지 본다", description = "붙으면 Advisor 가 걸린 것이다.")
    public String ask(@RequestParam String q,
                      @RequestParam(defaultValue = "s1") String sessionId) {
        return chat.prompt()
                .user(q)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call().content();
    }

    @GetMapping("/lab12/history")
    @Operation(summary = "대화 이력", description = "차단 Advisor 의 order 를 바꾼 뒤 여기를 보면 순서의 의미가 보인다.")
    public List<Map<String, String>> history(@RequestParam(defaultValue = "s1") String sessionId) {
        return memory.get(sessionId).stream()
                .map(m -> Map.of("role", m.getMessageType().getValue(),
                                 "text", String.valueOf(m.getText())))
                .toList();
    }
}
