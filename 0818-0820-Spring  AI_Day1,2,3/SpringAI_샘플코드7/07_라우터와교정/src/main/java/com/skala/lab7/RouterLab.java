package com.skala.lab7;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 7장 미니 실습 — 답변 스타일 라우터 + 스스로 채점.
 *
 * <p>한 번의 호출로 안 되는 일을 쪼갠다. 대신 호출이 2~3배로 는다.
 * 로그의 {@code 모델호출} 횟수가 "쪼갠 값어치" 를 판단하는 근거가 된다.
 */
@RestController
@Tag(name = "7장 실습 · 라우터와 교정")
public class RouterLab {

    private static final Logger log = LoggerFactory.getLogger(RouterLab.class);

    public enum 스타일 { 짧게, 자세히, 농담 }

    public record 점수(int value, String reason) {}

    private final ChatClient 분류기, 작은모델, 큰모델, 평가자;

    public RouterLab(ChatModel model) {
        // 창구마다 builder 를 새로 연다. 분류·채점은 흔들리면 안 되므로 온도 0.
        this.분류기 = 창구(model, 0.0);
        this.평가자 = 창구(model, 0.0);
        this.작은모델 = 창구(model, 0.3);
        this.큰모델 = 창구(model, 0.7);
    }

    private static ChatClient 창구(ChatModel model, double 온도) {
        return ChatClient.builder(model)
                .defaultOptions(ChatOptions.builder().temperature(온도).build())
                .build();
    }

    @GetMapping("/lab7/ask")
    @Operation(summary = "질문 성격에 따라 경로를 가른다", description = "분류 → 답변 → 채점 → 필요하면 한 번만 교정")
    public String ask(@RequestParam String q) {
        스타일 route = 분류기.prompt()                        // 먼저 고른다
                .system("질문에 어울리는 답변 스타일을 하나 고른다.")
                .user(q).call().entity(스타일.class);

        String 답 = switch (route) {                          // 경로별로 다르게 답한다
            case 짧게 -> 작은모델.prompt().system("한 문장으로만 답한다.").user(q)
                    .call().content();
            case 자세히 -> 큰모델.prompt().system("단계별로 자세히 설명한다.").user(q)
                    .call().content();
            case 농담 -> 큰모델.prompt().system("농담을 섞되 사실은 정확히.").user(q)
                    .call().content();
        };

        점수 s = 평가자.prompt()                              // 스스로 채점한다
                .user("질문:%s%n답변:%s%n0~5점과 이유를 매겨라".formatted(q, 답))
                .call().entity(점수.class);

        if (s.value() < 3) {                                  // 낮을 때만, 한 번만 고친다
            답 = 큰모델.prompt().user("아래 답을 더 낫게 고쳐라: " + 답).call().content();
        }

        log.info("route={} score={} 모델호출={}회", route, s.value(), s.value() < 3 ? 4 : 3);
        return 답;
    }
}
