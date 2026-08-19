package com.example.step05;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * STEP 05 - 멀티턴 대화 컨트롤러
 *
 * <p><b>핵심 규칙</b> : 동일한 {@code conversationId}를 사용해야 같은 대화 Context가 유지된다.
 * ID가 달라지면 Spring AI는 완전히 다른 대화로 취급해 이력을 불러오지 않는다.
 *
 * <p><b>동작 확인 시나리오</b>
 * <pre>
 *   1) /api/chat?conversationId=u1&amp;message=내 이름은 홍길동이야
 *   2) /api/chat?conversationId=u1&amp;message=내 이름이 뭐라고 했지?   → "홍길동" (기억함)
 *   3) /api/chat?conversationId=u2&amp;message=내 이름이 뭐라고 했지?   → 모른다고 답함 (다른 대화)
 * </pre>
 *
 * <p><b>실무 주의</b> : conversationId를 클라이언트가 마음대로 보내게 두면
 * 남의 ID를 넣어 타인의 대화 이력을 열람할 수 있다.
 * 운영에서는 로그인 세션/사용자 ID에서 서버가 직접 파생시켜야 한다.
 */
@RestController
public class MemoryController {

    private final ChatClient chatClient;

    /** MemoryConfig에서 MessageChatMemoryAdvisor가 부착된 ChatClient를 주입받는다. */
    public MemoryController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * @param conversationId 대화 구분 키(사용자별/세션별로 유일해야 한다)
     * @param message        이번 턴의 사용자 질문
     */
    @GetMapping("/api/chat")
    public String chat(
            @RequestParam String conversationId,
            @RequestParam String message) {

        return chatClient
                .prompt()
                .user(message)
                // advisors(Consumer) : 이번 호출에 한해 Advisor에게 전달할 파라미터를 지정한다.
                // ChatMemory.CONVERSATION_ID는 "chat_memory_conversation_id" 라는
                // 약속된 키 이름의 상수다. 오타를 막기 위해 문자열 대신 상수를 쓴다.
                // 이 값을 생략하면 모든 사용자가 "default" 라는 하나의 대화를
                // 공유하게 되어 서로의 이력이 뒤섞인다.
                .advisors(a -> a.param(
                        ChatMemory.CONVERSATION_ID,
                        conversationId
                ))
                .call()
                .content();
    }
}
