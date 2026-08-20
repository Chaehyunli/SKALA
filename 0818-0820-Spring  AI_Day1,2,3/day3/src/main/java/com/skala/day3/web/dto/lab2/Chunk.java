package com.skala.day3.web.dto.lab2;

/**
 * 검색 API(GET /lab2/retrieve) 응답 한 건 — 근거 청크 하나를 눈으로 확인하기 위한 형태.
 *
 * @param source  이 청크가 나온 문서 출처(인제스트 시점에 심어 둔 메타데이터)
 * @param score   질문과의 유사도 점수 — 감으로 판단하지 않도록 항상 노출한다
 * @param snippet 청크 본문 앞부분 일부(미리보기) — 전체 본문 대신 짧게 잘라서 보여준다
 */
public record Chunk(String source, Double score, String snippet) {
}
