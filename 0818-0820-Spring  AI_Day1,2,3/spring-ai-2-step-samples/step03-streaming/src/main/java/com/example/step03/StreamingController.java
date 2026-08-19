package com.example.step03;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * STEP 03 - Streaming (스트리밍 응답)
 *
 * <p><b>학습 목표</b> : {@code call()} 대신 {@code stream()}을 사용해
 * 토큰이 생성되는 즉시 클라이언트로 흘려보낸다(ChatGPT의 타자기 효과).
 *
 * <p><b>call() vs stream()</b>
 * <table border="1">
 *   <tr><th></th><th>call()</th><th>stream()</th></tr>
 *   <tr><td>반환</td><td>String (완성본)</td><td>Flux&lt;String&gt; (조각의 흐름)</td></tr>
 *   <tr><td>체감 속도</td><td>느림 (전부 기다림)</td><td>빠름 (첫 글자가 바로 보임)</td></tr>
 *   <tr><td>적합한 곳</td><td>배치·후처리·구조화 출력</td><td>채팅 UI</td></tr>
 * </table>
 *
 * <p><b>전송 방식 : SSE(Server-Sent Events)</b><br>
 * 서버 → 클라이언트 단방향 푸시 전용 HTTP 표준이다.
 * WebSocket과 달리 별도 프로토콜 업그레이드가 없고, 브라우저의 {@code EventSource}로
 * 몇 줄이면 붙는다(static/index.html 참고).
 */
@RestController
public class StreamingController {

    private final ChatClient chatClient;

    public StreamingController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 예) GET http://localhost:8103/api/chat/stream?message=Spring AI 설명해줘
     * <p>브라우저에서는 http://localhost:8103/ 의 데모 페이지로 확인할 수 있다.
     *
     * @param message 사용자 질문
     * @return 생성되는 순서대로 방출되는 텍스트 조각(chunk)의 스트림.
     *         Flux는 '아직 값이 다 정해지지 않은 0..N개의 데이터 흐름'을 나타내는
     *         Reactor(리액티브 스트림) 타입이다. 이 메서드는 즉시 리턴되고,
     *         실제 데이터는 이후 비동기로 흘러나간다.
     */
    @GetMapping(
            value = "/api/chat/stream",
            // produces : Content-Type을 'text/event-stream'으로 지정.
            // 이 헤더가 있어야 브라우저와 프록시가 응답을 '버퍼링하지 않고' 즉시 흘려보낸다.
            // 빠뜨리면 스트리밍이 동작하는 것처럼 보이다가 마지막에 한꺼번에 도착한다.
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> stream(@RequestParam String message) {
        return chatClient
                .prompt()
                .user(message)
                // stream() : 모델을 스트리밍 모드로 호출한다(내부적으로 SSE 기반 API 사용).
                .stream()
                // content() : 각 조각에서 텍스트만 뽑아 Flux<String>으로 만든다.
                // 토큰 사용량 등 메타데이터가 필요하면 chatResponse()로 Flux<ChatResponse>를 받는다.
                .content();
    }
}
