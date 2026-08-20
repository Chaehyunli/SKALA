package com.skala.lab14;

import java.time.Duration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;

/**
 * 14장 미니 실습 — 타자기 효과(SSE).
 *
 * <p>확인할 때 {@code curl} 에 {@code -N} 을 꼭 붙인다. 빠뜨리면 버퍼링 때문에
 * 한꺼번에 나와서 스트리밍이 안 되는 것처럼 보인다.
 *
 * <pre>
 * curl -N -X POST localhost:8080/lab14/stream \
 *      -H 'Content-Type: application/json' -d '{"question":"자기소개 해줘"}'
 * </pre>
 */
@RestController
@Tag(name = "14장 실습 · SSE와 추적 ID")
public class StreamLab {

    public record Ask(String question) {}

    private final ChatClient chat;

    public StreamLab(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    @PostMapping(value = "/lab14/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "토큰이 오는 대로 흘려 보낸다", description = "event 이름으로 token 과 done 을 구분한다.")
    public Flux<ServerSentEvent<String>> stream(@RequestBody Ask req) {
        return chat.prompt().user(req.question()).stream().content()
                .map(t -> ServerSentEvent.builder(t).event("token").build())
                .concatWith(Flux.just(
                        ServerSentEvent.builder("[DONE]").event("done").build()))
                .timeout(Duration.ofSeconds(60));
    }
}
