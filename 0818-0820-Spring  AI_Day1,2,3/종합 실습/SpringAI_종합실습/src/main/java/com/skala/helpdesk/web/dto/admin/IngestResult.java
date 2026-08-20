package com.skala.helpdesk.web.dto.admin;

/**
 * 인제스트 결과 한 건.
 *
 * @param source 문서 출처 식별자(파일명에서 확장자를 뗀 값, 예: 해외주식-신고-규정)
 * @param chunks 이 문서에서 만들어져 벡터스토어에 저장된 청크 개수
 */
public record IngestResult(String source, int chunks) {
}
