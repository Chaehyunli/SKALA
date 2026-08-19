package com.example.step01;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * STEP 01 - ChatClient 기본 호출
 *
 * <p><b>학습 목표</b> : Spring AI의 가장 기본 흐름인
 * {@code prompt() -> user() -> call() -> content()} 체인을 이해한다.
 *
 * <p><b>ChatClient란?</b><br>
 * LLM(여기서는 OpenAI)과 대화하기 위한 Fluent API(메서드 체이닝 방식 API)다.
 * RestClient가 HTTP 호출을 추상화하듯, ChatClient는 AI 모델 호출을 추상화한다.
 * 모델 공급자(OpenAI / Anthropic / Ollama 등)가 바뀌어도 이 코드는 그대로 사용할 수 있다.
 *
 * <p><b>메시지 역할(Role) 개념</b>
 * <ul>
 *   <li>system : AI의 정체성·말투·규칙을 정하는 지시문. 대화 전체에 적용된다.</li>
 *   <li>user   : 사용자가 실제로 던지는 질문</li>
 *   <li>assistant : AI가 생성한 답변</li>
 * </ul>
 */
@RestController
public class ChatController {

    /**
     * 요청마다 새로 만들지 않고 필드로 보관한다.
     * ChatClient는 Thread-safe하므로 싱글턴 Bean처럼 재사용해도 안전하다.
     */
    private final ChatClient chatClient;

    /**
     * 생성자 주입(Constructor Injection).
     *
     * <p>주입받는 타입이 {@code ChatClient}가 아니라 {@code ChatClient.Builder}인 점에 주목하자.
     * Spring AI는 완성된 ChatClient가 아니라 <b>Builder</b>를 Bean으로 등록한다.
     * 컨트롤러마다 서로 다른 기본 설정(system 프롬프트, Advisor, 모델 옵션)을 갖도록
     * 각자 build() 하도록 유도하기 위해서다.
     *
     * @param builder 자동 구성으로 등록된 ChatClient 빌더 (OpenAI 연결 정보가 이미 세팅되어 있음)
     */
    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder
                // defaultSystem : 이 ChatClient로 보내는 '모든' 요청 앞에 자동으로 붙는 시스템 메시지.
                // 매 요청마다 .system(...)을 반복해서 쓰지 않아도 되고, 캐릭터를 일관되게 유지한다.
                // Java 15+ 텍스트 블록(""" ... """)을 쓰면 여러 줄 프롬프트를 읽기 좋게 작성할 수 있다.
                .defaultSystem("""
                        당신은 Java와 Spring을 친절하게 설명하는 기술 강사입니다.
                        초보자가 이해할 수 있도록 핵심부터 설명하세요.
                        """)
                // build() 시점에 위 설정이 고정된 불변(immutable) ChatClient가 만들어진다.
                .build();
    }

    /**
     * 예) GET http://localhost:8101/api/chat?message=Spring AI가 뭐야?
     *
     * @param message 사용자 질문. {@code @RequestParam}이므로 쿼리스트링에 반드시 있어야 한다.
     * @return LLM이 생성한 답변 문자열 (동기 방식이라 답변이 완성될 때까지 스레드가 대기한다)
     */
    @GetMapping("/api/chat")
    public String chat(@RequestParam String message) {
        return chatClient
                // 1) prompt() : 새 요청(Prompt)을 조립하기 시작한다. 여기서 빌더가 열린다.
                .prompt()
                // 2) user() : 사용자 메시지를 추가한다. (defaultSystem은 이미 앞에 붙어 있다)
                .user(message)
                // 3) call() : 모델을 '동기(blocking)'로 호출한다. 응답 전체가 올 때까지 기다린다.
                //    스트리밍이 필요하면 call() 대신 stream()을 쓴다 → STEP 03
                .call()
                // 4) content() : 응답에서 순수 텍스트만 꺼낸다.
                //    토큰 사용량·종료 이유 등 메타데이터까지 필요하면 chatResponse()를 쓴다.
                .content();
    }
}
