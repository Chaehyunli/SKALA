package com.skala.lab0.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.skala.lab0.domain.Snack;

/**
 * 1장 미니 실습 — 데이터에 닿는 유일한 자리.
 *
 * <p>실제 DB 는 쓰지 않는다. 여기서 중요한 것은 "저장소를 뒤지는 일은 여기서만 한다" 는
 * 약속이지, 어디에 담겨 있느냐가 아니다. 나중에 JPA 로 바꿔도 위 계층은 그대로다.
 */
@Repository
public class SnackRepository {

    private static final Map<String, Snack> 기분별 = Map.of(
            "피곤", new Snack("초코바", "당 충전"),
            "더움", new Snack("아이스크림", "체온 낮추기"),
            "배고픔", new Snack("샌드위치", "요기 되는 걸로"),
            "심심", new Snack("젤리", "오래 씹기"));

    public Optional<Snack> findByMood(String mood) {
        return Optional.ofNullable(기분별.get(mood));
    }
}
