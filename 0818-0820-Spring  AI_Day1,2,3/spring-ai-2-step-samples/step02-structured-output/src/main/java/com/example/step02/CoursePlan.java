package com.example.step02;

import java.util.List;

/**
 * STEP 02 - 구조화 출력(Structured Output)의 '목표 타입'
 *
 * <p>이 record는 두 가지 역할을 동시에 한다.
 * <ol>
 *   <li><b>LLM에게 주는 출력 규격</b> :
 *       Spring AI가 이 타입을 리플렉션으로 분석해 JSON Schema를 만들고,
 *       "이 스키마대로만 JSON을 출력하라"는 지시를 프롬프트에 자동으로 덧붙인다.</li>
 *   <li><b>역직렬화 대상</b> :
 *       모델이 돌려준 JSON 문자열을 Jackson이 이 타입으로 변환한다.</li>
 * </ol>
 *
 * <p><b>왜 record인가?</b><br>
 * 불변(immutable)이고, 생성자·getter·equals·hashCode·toString이 자동 생성되어
 * DTO로 쓰기에 적합하다. 일반 class(기본 생성자 + getter/setter)로도 동작한다.
 *
 * <p><b>실무 팁</b> : 필드 이름 자체가 LLM에게는 '지시문'이다.
 * {@code lvl} 보다 {@code level}, {@code h} 보다 {@code hours}처럼
 * 의미가 분명한 이름을 쓸수록 채워지는 값의 품질이 올라간다.
 *
 * @param title  교육과정 제목 (예: "Spring AI 입문")
 * @param level  난이도 (예: "초급")
 * @param hours  총 교육 시간. 기본형 int이므로 모델이 값을 빠뜨리면 0이 들어간다.
 * @param topics 핵심 주제 목록. 제네릭 타입(String)까지 스키마에 반영된다.
 */
public record CoursePlan(
        String title,
        String level,
        int hours,
        List<String> topics
) {
}
