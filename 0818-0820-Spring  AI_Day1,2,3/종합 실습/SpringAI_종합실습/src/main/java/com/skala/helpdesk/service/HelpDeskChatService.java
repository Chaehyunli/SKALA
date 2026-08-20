package com.skala.helpdesk.service;

import com.skala.helpdesk.web.dto.chat.response.AnswerDto;
import com.skala.helpdesk.web.dto.chat.response.ChatMessageView;
import com.skala.helpdesk.web.dto.chat.response.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 채팅 처리 조립 — 대화 ID를 만드는 지점은 여기 한 곳뿐이다(day3 Lab3ChatService 원칙 그대로).
 * 섞이면 남의 대화가 이어지는 사고가 난다.
 */
@Service
public class HelpDeskChatService {

    private static final Logger log = LoggerFactory.getLogger(HelpDeskChatService.class);

    private final ChatClient helpDeskClient;
    private final ChatMemory chatMemory;
    private final ToolUsageTracker usageTracker;
    private final boolean simulatePrimaryFailure;

    public HelpDeskChatService(ChatClient helpDeskClient,
                                ChatMemory chatMemory,
                                ToolUsageTracker usageTracker,
                                @Value("${helpdesk.simulate-primary-failure:false}") boolean simulatePrimaryFailure) {
        this.helpDeskClient = helpDeskClient;
        this.chatMemory = chatMemory;
        this.usageTracker = usageTracker;
        this.simulatePrimaryFailure = simulatePrimaryFailure;
    }

    public AnswerDto ask(String userId, String message, String sessionId) {
        String conversationId = conversationId(userId, sessionId);
        usageTracker.reset();
        try {
            if (simulatePrimaryFailure) {
                return unavailableAnswer();
            }
            ChatClientResponse response = helpDeskClient.prompt()
                    .user(message)
                    .toolContext(Map.of("userId", userId))                 // 도구가 보는 사용자
                    .advisors(a -> a
                            .param("userId", userId)                       // 감사 로그가 보는 사용자
                            .param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .chatClientResponse();
            String text = response.chatResponse().getResult().getOutput().getText();
            return new AnswerDto(text, sourcesOf(response), usageTracker.wasUsed());
        } catch (Exception e) {
            log.warn("AI 호출 실패 — 안전한 정적 안내 응답으로 전환합니다.", e);
            return unavailableAnswer();
        }
    }

    /** SSE 스트리밍용 — 토큰 조각을 그대로 흘려보낸다. 컨트롤러가 SSE 이벤트 모양으로 감싼다. */
    public Flux<ChatClientResponse> stream(String userId, String message, String sessionId) {
        if (simulatePrimaryFailure) {
            // 컨트롤러의 onErrorResume이 이 에러를 안내 문구 토큰 이벤트로 바꿔 내보낸다 — ask()의 정적 응답과 동일한 안내.
            return Flux.error(new IllegalStateException("primary failure simulated"));
        }
        String conversationId = conversationId(userId, sessionId);
        return helpDeskClient.prompt()
                .user(message)
                .toolContext(Map.of("userId", userId))
                .advisors(a -> a
                        .param("userId", userId)
                        .param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .chatClientResponse();
    }

    /** 단일 OpenAI 공급자 환경의 장애 대응: 다른 모델을 가장하지 않고 안전한 안내만 반환한다. */
    public AnswerDto unavailableAnswer() {
        return new AnswerDto("현재 AI 응답 기능을 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.", List.of(), false);
    }

    public List<ChatMessageView> history(String userId, String sessionId) {
        return chatMemory.get(conversationId(userId, sessionId)).stream()
                .map(m -> new ChatMessageView(m.getMessageType().getValue(), m.getText()))
                .toList();
    }

    /** 대화 ID 규칙 — tenant 없이 userId:session 조합. 호출부는 이 메서드만 쓴다. */
    public String conversationId(String userId, String sessionId) {
        String session = (sessionId == null || sessionId.isBlank()) ? "default" : sessionId;
        return userId + ":" + session;
    }

    @SuppressWarnings("unchecked")
    public List<Source> sourcesOf(ChatClientResponse response) {
        Object raw = response.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
        if (!(raw instanceof List<?> docs)) {
            return List.of();
        }
        return docs.stream()
                .filter(Document.class::isInstance)
                .map(Document.class::cast)
                .map(d -> new Source((String) d.getMetadata().get("source"), (String) d.getMetadata().get("version")))
                .distinct()
                .toList();
    }
}
