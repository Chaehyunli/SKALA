package com.example.step04;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * STEP 04 - Advisor 적용 확인용 컨트롤러
 *
 * <p>코드를 보면 STEP 01과 거의 같다. <b>바로 그것이 핵심</b>이다.
 * 로깅 기능이 추가되었지만 비즈니스 코드(컨트롤러)는 전혀 바뀌지 않았다.
 * 횡단 관심사(cross-cutting concern)를 Advisor로 분리했기 때문이다.
 */
@RestController
public class AdvisorController {

    private final ChatClient chatClient;

    /**
     * Builder가 아니라 <b>완성된 ChatClient</b>를 주입받는다.
     * {@link AiConfig}에서 SimpleLoggerAdvisor가 이미 부착된 인스턴스다.
     */
    public AdvisorController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 예) GET http://localhost:8104/api/advisor?message=Advisor가 뭐야?
     * <p>호출 후 애플리케이션 콘솔 로그에서 요청/응답 전문을 확인해 보자.
     */
    @GetMapping("/api/advisor")
    public String ask(@RequestParam String message) {
        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }
}
