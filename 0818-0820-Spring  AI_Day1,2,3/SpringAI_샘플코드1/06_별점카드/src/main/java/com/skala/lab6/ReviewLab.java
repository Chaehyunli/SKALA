package com.skala.lab6;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 6장 미니 실습 — 영화 감상문을 별점 카드로.
 *
 * <p>문자열이 아니라 객체로 온다. 이 실습은 그게 전부다.
 * 모델이 {@code 느낌} 에 없는 값을 만들면 그 자리에서 실패한다. 그게 안전하다.
 */
@RestController
@Tag(name = "6장 실습 · 별점 카드")
public class ReviewLab {

    /** 보기를 제한한다. */
    public enum 느낌 { 최고, 좋음, 보통, 별로 }

    /** 받을 모양을 미리 정한다. */
    public record 리뷰카드(String 제목, 느낌 감정, int 별점, List<String> 키워드) {}

    private final ChatClient chat;

    public ReviewLab(ChatClient.Builder b) {
        this.chat = b.build();
    }

    @GetMapping("/lab6/review")
    @Operation(summary = "감상문 → 별점 카드", description = "별점은 1~5 정수. 파싱 코드가 한 줄도 없다.")
    public 리뷰카드 card(@RequestParam String text) {
        return chat.prompt()
                .system("영화 감상문을 카드로 정리한다. 별점은 1~5 정수로 매긴다.")
                .user(text)
                .call()
                .entity(리뷰카드.class);                   // 객체로 받는다(파싱 없음)
    }
}
