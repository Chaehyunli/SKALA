package com.example.step05;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * STEP 05 - Chat Memory (대화 기억) 설정
 *
 * <p><b>왜 필요한가?</b><br>
 * LLM API는 <b>Stateless</b>다. 이전 요청을 전혀 기억하지 못한다.
 * "내 이름은 홍길동이야" → "내 이름이 뭐라고 했지?" 라고 물으면 모른다고 답한다.
 * 대화가 이어지는 것처럼 보이려면 <b>이전 대화 내용을 매번 다시 보내야</b> 한다.
 * 그 반복 작업을 자동화해 주는 것이 ChatMemory + MessageChatMemoryAdvisor다.
 */
@Configuration
public class MemoryConfig {

    /**
     * 대화 이력 저장소.
     *
     * <p><b>MessageWindowChatMemory</b> : 최근 N개 메시지만 유지하는 슬라이딩 윈도우 방식.
     * 오래된 메시지는 앞에서부터 버린다.
     *
     * <p><b>왜 개수를 제한하나?</b>
     * <ul>
     *   <li>비용 : 대화가 길어질수록 매 요청에 보내는 토큰이 누적되어 요금이 증가한다.</li>
     *   <li>한계 : 모델의 컨텍스트 윈도우를 넘으면 요청 자체가 실패한다.</li>
     * </ul>
     *
     * <p><b>저장 위치</b> : 기본 구현은 애플리케이션 메모리(InMemory)다.
     * 재시작하면 사라지고, 서버를 여러 대로 늘리면 인스턴스마다 기억이 달라진다.
     * 운영에서는 JDBC / Redis 기반 {@code ChatMemoryRepository}로 교체한다.
     */
    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                // 최근 20개(user/assistant 합산) 메시지 = 대략 10턴 분량을 유지한다.
                .maxMessages(20)
                .build();
    }

    /**
     * 위에서 만든 ChatMemory를 Advisor로 감싸 ChatClient에 부착한다.
     *
     * <p><b>MessageChatMemoryAdvisor의 동작</b>
     * <ol>
     *   <li>요청 전 : conversationId로 이전 대화를 조회해 프롬프트 앞에 끼워 넣는다.</li>
     *   <li>응답 후 : 이번 질문과 답변을 다시 저장소에 기록한다.</li>
     * </ol>
     * 덕분에 컨트롤러는 "대화 ID만 알려주면" 되고, 이력 관리 코드를 짤 필요가 없다.
     *
     * <p>참고: 프롬프트 문자열에 이력을 직접 이어 붙이는
     * {@code PromptChatMemoryAdvisor}도 있다. MessageChatMemoryAdvisor는
     * 이력을 <b>메시지 목록 형태 그대로</b> 전달하므로 역할(role) 구분이 유지되어
     * 일반적인 멀티턴 채팅에는 이쪽이 더 자연스럽다.
     */
    @Bean
    ChatClient chatClient(
            ChatClient.Builder builder,
            ChatMemory chatMemory) {

        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor
                                .builder(chatMemory)
                                .build()
                )
                .build();
    }
}
