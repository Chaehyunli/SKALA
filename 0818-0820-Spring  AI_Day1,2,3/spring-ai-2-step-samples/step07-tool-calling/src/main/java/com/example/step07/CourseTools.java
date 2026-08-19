package com.example.step07;

import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * STEP 07 - Tool(함수) 정의
 *
 * <p><b>Tool Calling이란?</b><br>
 * LLM이 스스로 코드를 실행하는 것이 <b>아니다</b>. 실제 흐름은 이렇다.
 * <ol>
 *   <li>Spring AI가 이 클래스의 메서드 시그니처와 description을
 *       '사용 가능한 함수 목록'으로 만들어 모델에 함께 보낸다.</li>
 *   <li>모델이 "getCourseHours(courseName=\"java\")를 호출해줘"라는
 *       <b>요청(JSON)</b>을 응답으로 돌려준다.</li>
 *   <li>Spring AI가 실제로 이 Java 메서드를 실행한다.</li>
 *   <li>실행 결과를 모델에게 다시 보내고, 모델이 최종 문장을 만든다.</li>
 * </ol>
 * 즉 <b>모델은 판단만 하고, 실행 주체는 우리 애플리케이션</b>이다.
 * (그래서 LLM 호출이 최소 2회 발생하고, 그만큼 비용·지연이 늘어난다)
 *
 * <p><b>어디에 쓰나</b> : 실시간 데이터 조회(재고, 날씨, 사내 DB),
 * 계산, 외부 시스템 액션 실행 등 모델이 알 수 없거나 정확해야 하는 작업.
 *
 * <p><b>보안 주의</b> : Tool은 LLM이 호출 여부와 인자를 결정한다.
 * 삭제·결제·메일 발송처럼 되돌릴 수 없는 작업을 Tool로 열어 두면
 * 프롬프트 인젝션으로 오작동할 수 있다. 조회 위주로 시작하고,
 * 변경 작업은 별도 승인 절차를 둔다.
 */
@Component
public class CourseTools {

    /** 데모용 인메모리 데이터. 실무라면 이 자리에 Repository/외부 API 호출이 들어간다. */
    private final Map<String, Integer> courseHours = Map.of(
            "java", 40,
            "spring boot", 32,
            "spring ai", 24,
            "kubernetes", 24
    );

    /**
     * {@code @Tool} : 이 메서드를 LLM에게 노출할 함수로 등록한다.
     *
     * <p><b>description이 가장 중요하다.</b> 모델은 이 설명만 보고
     * "지금 이 함수를 써야 하는가"를 판단한다. 설명이 모호하면
     * 호출해야 할 때 호출하지 않거나, 엉뚱할 때 호출한다.
     * 사람이 아니라 <b>모델에게 주는 API 문서</b>라고 생각하고 작성한다.
     *
     * <p>{@code @ToolParam} : 파라미터의 의미를 설명한다.
     * 이 설명을 보고 모델이 사용자 문장에서 어떤 값을 뽑아 넣을지 정한다.
     *
     * @param courseName 모델이 사용자 질문에서 추출해 넣어주는 과정 이름
     * @return 모델에게 되돌려줄 결과 문자열. 이 문장이 최종 답변의 근거가 된다.
     */
    @Tool(description = "교육 과정 이름으로 권장 교육 시간을 조회한다.")
    public String getCourseHours(
            @ToolParam(description = "교육 과정 이름") String courseName) {

        // 모델이 "Java", "JAVA", "java" 중 무엇을 넣을지 보장할 수 없으므로
        // 소문자로 정규화해 조회한다. LLM 입력은 항상 방어적으로 다룬다.
        Integer hours = courseHours.get(courseName.toLowerCase());

        if (hours == null) {
            // 예외를 던지는 대신 '모른다'는 사실을 문장으로 돌려준다.
            // 모델이 이 결과를 읽고 사용자에게 자연스럽게 설명할 수 있다.
            return "등록되지 않은 과정입니다.";
        }

        return courseName + " 과정의 권장 교육 시간은 " + hours + "시간입니다.";
    }

    /**
     * 두 번째 Tool. 한 클래스에 여러 Tool을 둘 수 있고,
     * 모델은 필요하면 <b>여러 Tool을 연달아</b> 호출한다.
     *
     * <p>예: "Java와 Spring Boot 과정 시간을 합치면?" 이라고 물으면
     * getCourseHours를 두 번 호출해 40, 32를 얻은 뒤
     * sumCourseHours(40, 32)를 호출하는 식으로 스스로 연결한다.
     *
     * <p>LLM은 산술 계산에서 실수할 수 있으므로,
     * 이런 단순 계산도 Tool로 위임하면 정확도가 올라간다.
     */
    @Tool(description = "두 교육 과정의 교육 시간을 합산한다.")
    public int sumCourseHours(
            @ToolParam(description = "첫 번째 과정의 교육 시간") int first,
            @ToolParam(description = "두 번째 과정의 교육 시간") int second) {

        return first + second;
    }
}
