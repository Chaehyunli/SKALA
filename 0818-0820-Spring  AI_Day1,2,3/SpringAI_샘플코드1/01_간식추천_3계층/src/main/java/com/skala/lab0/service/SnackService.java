package com.skala.lab0.service;

import org.springframework.stereotype.Service;

import com.skala.lab0.domain.Snack;
import com.skala.lab0.web.SnackResponse;

/**
 * 1장 미니 실습 — '무엇을 하는가' 는 여기에만 적는다.
 *
 * <p>AI 는 한 줄도 없다. 계층을 먼저 몸에 익히는 것이 이 실습의 전부다.
 * 뒤 장에서 AI 호출이 들어올 자리도 정확히 이 자리다.
 */
@Service
public class SnackService {

    private final SnackRepository repo;

    public SnackService(SnackRepository repo) {
        this.repo = repo;
    }

    public SnackResponse recommend(String mood) {
        Snack s = repo.findByMood(mood)                     // 데이터는 저장소에서만
                .orElse(new Snack("아메리카노", "무난하게"));
        return new SnackResponse(s.name(), s.reason());     // 나갈 때는 DTO 로
    }
}
