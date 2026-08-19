package com.skala.day2.web;

/**
 * 인제스트 API(POST /lab2/ingest) 응답 한 건.
 *
 * @param source 문서 출처 식별자(파일명에서 확장자를 뗀 값, 예: return-policy)
 * @param chunks 이 문서에서 만들어져 벡터스토어에 저장된 청크 개수
 */
public record IngestResult(String source, int chunks) {
}
