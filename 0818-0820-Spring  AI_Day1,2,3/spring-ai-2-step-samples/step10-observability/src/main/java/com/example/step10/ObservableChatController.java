package com.example.step10;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * STEP 10 - Observability (관측 가능성)
 *
 * <p><b>왜 필요한가?</b><br>
 * AI 기능은 운영에 올린 뒤가 진짜 시작이다. 확인해야 할 것들이 있다.
 * <ul>
 *   <li><b>비용</b> : 토큰을 얼마나 쓰고 있나? (요청당 과금이므로 급증 감지가 중요)</li>
 *   <li><b>지연</b> : LLM 호출은 수 초가 걸린다. p95 응답 시간은?</li>
 *   <li><b>실패</b> : 레이트리밋(429)·타임아웃은 얼마나 발생하나?</li>
 * </ul>
 *
 * <p><b>Spring AI가 자동으로 제공하는 것</b><br>
 * Micrometer 기반 관측이 내장되어 있어 별도 코드 없이
 * {@code gen_ai.client.operation}, {@code spring.ai.chat.client} 같은 지표가 수집된다.
 * (호출 횟수, 소요 시간, 모델명·토큰 사용량 등)
 * 확인은 {@code /actuator/metrics} 또는 {@code /actuator/prometheus}에서 한다.
 *
 * <p>즉 이 컨트롤러 코드에 관측 관련 코드가 하나도 없는 것이 정상이다.
 * 스타터 의존성과 설정만으로 계측이 붙는다.
 */
@RestController
public class ObservableChatController {

    private final ChatClient chatClient;

    public ObservableChatController(ChatClient.Builder builder) {
        this.chatClient = builder
                // 지표(metrics)는 '얼마나/얼마나 오래'를 알려주고,
                // 로그는 '무엇을 주고받았는지'를 알려준다. 둘은 상호 보완적이다.
                //
                // 단, 프롬프트 본문에는 사용자 개인정보가 들어갈 수 있다.
                // application.yml의 observations.log-prompt / log-completion을
                // false로 둔 이유가 그것이다(기본값도 false).
                // 문제 재현이 필요할 때만 한시적으로 켠다.
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * 예) GET http://localhost:8110/api/chat?message=안녕
     *
     * <p>몇 번 호출한 뒤 아래를 확인해 보자.
     * <pre>
     *   curl "http://localhost:8110/actuator/metrics/gen_ai.client.operation"
     *   curl -s http://localhost:8110/actuator/prometheus | grep gen_ai
     * </pre>
     */
    @GetMapping("/api/chat")
    public String chat(@RequestParam String message) {
        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }
}
