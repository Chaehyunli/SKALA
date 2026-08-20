package com.skala.lab10;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 10장 미니 실습 — 도구를 붙여서 물어본다.
 *
 * <p>질문 세 개로 도구가 언제 불리고 언제 안 불리는지 본다.
 * <ul>
 *   <li>"안녕하세요" → 도구 호출 없음</li>
 *   <li>"점심 뭐 먹지" → 점심추천 1개</li>
 *   <li>"더운데 점심 뭐 먹지" → 날씨 → 점심추천</li>
 * </ul>
 */
@RestController
@Tag(name = "10장 실습 · 점심 추천 도구")
public class LunchController {

    private final ChatClient chat;
    private final LunchTools lunchTools;

    public LunchController(ChatClient.Builder builder, LunchTools lunchTools) {
        this.chat = builder.build();
        this.lunchTools = lunchTools;
    }

    @GetMapping("/lab10/ask")
    @Operation(summary = "도구를 쥐여 주고 물어본다", description = "도구를 부를지 말지는 모델이 정한다. 실행은 우리 코드가 한다.")
    public String ask(@RequestParam String q) {
        return chat.prompt().user(q).tools(lunchTools).call().content();
    }
}
