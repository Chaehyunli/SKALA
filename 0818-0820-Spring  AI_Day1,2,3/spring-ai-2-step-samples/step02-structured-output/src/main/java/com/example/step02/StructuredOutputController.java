package com.example.step02;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * STEP 02 - Structured Output (구조화 출력)
 *
 * <p><b>학습 목표</b> : LLM의 자유 서술형 답변을 <b>타입 안전한 Java 객체</b>로 받는다.
 *
 * <p><b>왜 필요한가?</b><br>
 * STEP 01처럼 String을 받으면 결과를 DB에 저장하거나 다른 로직에 넘기기 위해
 * 사람이 직접 파싱해야 한다. entity()를 쓰면 이 과정을 Spring AI가 대신 해준다.
 *
 * <p><b>entity() 내부 동작 3단계</b>
 * <ol>
 *   <li>대상 타입({@link CoursePlan})으로부터 JSON Schema를 생성</li>
 *   <li>"이 스키마에 맞는 JSON만 출력하라"는 포맷 지시를 프롬프트 끝에 자동 추가
 *       (OpenAI처럼 지원하는 모델은 response_format=json_schema 로 강제한다)</li>
 *   <li>응답 JSON을 Jackson으로 역직렬화해 객체로 반환</li>
 * </ol>
 */
@RestController
public class StructuredOutputController {

    private final ChatClient chatClient;

    /**
     * 여기서는 defaultSystem 없이 기본 설정 그대로 build() 한다.
     * 출력 형식 지시는 entity()가 자동으로 넣어주므로 직접 쓸 필요가 없다.
     */
    public StructuredOutputController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 예) GET http://localhost:8102/api/course?subject=Kubernetes
     *
     * <p>반환 타입이 {@link CoursePlan}이므로 Spring MVC가 이 객체를
     * 다시 JSON으로 직렬화해 응답 본문에 실어준다.
     *
     * @param subject 교육 주제. 값이 없으면 기본값 "Spring AI"를 사용한다.
     */
    @GetMapping("/api/course")
    public CoursePlan course(
            @RequestParam(defaultValue = "Spring AI") String subject) {

        return chatClient
                .prompt()
                // user(Consumer) 형태 : 프롬프트 템플릿 + 파라미터 바인딩을 쓸 때 사용한다.
                .user(u -> u
                        // {subject}는 자리표시자(placeholder). 문자열 연결(+)이나
                        // String.format 대신 템플릿을 쓰면 프롬프트가 코드에서 분리되어 관리하기 쉽고,
                        // 사용자 입력이 프롬프트 문법과 섞이는 사고를 줄일 수 있다.
                        .text("""
                                {subject}를 주제로 초급 개발자를 위한 교육과정을 만들어 주세요.
                                topics는 핵심 주제 5개로 구성하세요.
                                """)
                        // param() : 템플릿의 {subject} 자리에 실제 값을 채운다.
                        .param("subject", subject))
                .call()
                // content()(문자열) 대신 entity()(객체)를 호출하는 것이 이 STEP의 핵심이다.
                .entity(
                        // 1번째 인자 : 변환할 목표 타입
                        CoursePlan.class,
                        // 2번째 인자 : 변환 옵션(spec).
                        // validateSchema() = 돌려받은 JSON이 스키마를 실제로 만족하는지 검증하고,
                        // 어긋나면 오류 내용을 담아 모델에 재요청한다(자동 재시도).
                        // 검증을 켜면 안정성이 올라가는 대신 실패 시 호출 횟수(비용)가 늘어난다.
                        spec -> spec.validateSchema()
                );
    }
}
