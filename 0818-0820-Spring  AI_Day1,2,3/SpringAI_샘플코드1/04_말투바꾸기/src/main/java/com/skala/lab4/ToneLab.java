package com.skala.lab4;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 4장 미니 실습 — 같은 질문을 두 창구에 동시에 보낸다.
 *
 * <p>확인: {@code curl 'localhost:8080/lab4/tone?q=오늘 회의 30분 늦어요'}
 */
@RestController
@Tag(name = "4장 실습 · 말투 바꾸기")
public class ToneLab {

    private final ChatClient 사극체, 이모지체;                    // 이름으로 골라 받는다

    public ToneLab(@Qualifier("사극체") ChatClient 사극체,
                   @Qualifier("이모지체") ChatClient 이모지체) {
        this.사극체 = 사극체;
        this.이모지체 = 이모지체;
    }

    @GetMapping("/lab4/tone")
    @Operation(summary = "같은 말을 두 말투로", description = "빈이 둘로 갈려 있는지 눈으로 확인한다.")
    public Map<String, String> tone(@RequestParam String q) {
        return Map.of("사극체", 사극체.prompt().user(q).call().content(),
                      "이모지체", 이모지체.prompt().user(q).call().content());
    }
}
