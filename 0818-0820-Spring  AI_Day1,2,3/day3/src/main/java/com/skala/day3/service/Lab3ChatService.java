package com.skala.day3.service;

import com.skala.day3.web.dto.lab3.request.ChatRequest;
import com.skala.day3.web.dto.lab3.response.ChatMessageView;
import com.skala.day3.web.dto.lab3.response.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Lab3 상담 에이전트 호출 조립 — 대화 ID를 만드는 지점은 여기 한 곳뿐이다.
 * 흩어지면 남의 대화가 섞이는 사고가 난다.
 */
@Service
public class Lab3ChatService {

    private final ChatClient assistantChatClient;
    private final ChatMemory chatMemory;

    public Lab3ChatService(@Qualifier("lab3AssistantChatClient") ChatClient assistantChatClient,
                            ChatMemory chatMemory) {
        this.assistantChatClient = assistantChatClient;
        this.chatMemory = chatMemory;
    }

    public ChatResponse chat(ChatRequest request) {
        // sessionId 를 생략하면 "이 사용자의 기본 대화"로 이어진다 — 매번 새로 시작되지 않는다.
        // 격리된 새 대화가 필요하면(Step5 세션 격리 검증 등) 호출부가 다른 sessionId 를 명시적으로 보낸다.
        String sessionId = request.sessionId() != null ? request.sessionId() : defaultSessionId(request.userId());
        String answer = assistantChatClient.prompt()
                .user(request.message())
                .toolContext(Map.of("userId", request.userId()))              // 도구가 보는 사용자
                .advisors(a -> a
                        .param("userId", request.userId())                    // 감사 로그가 보는 사용자
                        .param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .content();
        return new ChatResponse(sessionId, answer);
    }

    public List<ChatMessageView> history(String sessionId) {
        return chatMemory.get(sessionId).stream()
                .map(m -> new ChatMessageView(m.getMessageType().getValue(), m.getText()))
                .toList();
    }

    private String defaultSessionId(String userId) {
        return userId + ":default";
    }
}
