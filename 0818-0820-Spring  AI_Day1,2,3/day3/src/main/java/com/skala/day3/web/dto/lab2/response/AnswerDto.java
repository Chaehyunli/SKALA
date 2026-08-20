package com.skala.day3.web.dto.lab2.response;

import java.util.List;

/**
 * 답변 API(POST /lab2/ask) 응답.
 *
 * @param answer   근거를 바탕으로 한 답변. 근거가 없으면 "확인되지 않습니다."
 * @param sources  답변에 실제로 사용한 근거 출처 목록
 * @param grounded 근거를 찾아 답했는지 여부 — false 면 answer 는 unknown() 고정 문구다
 */
public record AnswerDto(String answer, List<String> sources, boolean grounded) {

    public static AnswerDto unknown() {
        return new AnswerDto("확인되지 않습니다.", List.of(), false);
    }
}
