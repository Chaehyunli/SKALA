package com.example.step07;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * STEP 07 - Tool Calling 컨트롤러
 *
 * <p><b>테스트 예시</b>
 * <pre>
 *   GET /api/tool?message=Spring AI 과정은 몇 시간이야?
 *        → getCourseHours 호출 후 "24시간" 이라고 답한다.
 *
 *   GET /api/tool?message=Java와 Kubernetes 과정 시간을 합치면?
 *        → getCourseHours 2회 + sumCourseHours 1회를 스스로 연쇄 호출한다.
 *
 *   GET /api/tool?message=안녕?
 *        → Tool이 필요 없다고 판단해 그냥 대화한다.
 * </pre>
 * 마지막 예시가 중요하다. Tool을 등록했다고 항상 호출되는 게 아니라,
 * <b>모델이 필요하다고 판단할 때만</b> 호출된다.
 */
@RestController
public class ToolController {

    private final ChatClient chatClient;

    /** Tool을 담고 있는 Bean. Spring이 @Component로 등록한 인스턴스를 주입해 준다. */
    private final CourseTools courseTools;

    public ToolController(
            ChatClient.Builder builder,
            CourseTools courseTools) {

        this.chatClient = builder.build();
        this.courseTools = courseTools;
    }

    @GetMapping("/api/tool")
    public String ask(@RequestParam String message) {
        return chatClient
                .prompt()
                .user(message)
                // tools() : 이번 요청에서 모델이 사용할 수 있는 Tool을 등록한다.
                // 객체를 넘기면 그 안의 @Tool 메서드를 스캔해 함수 목록으로 변환한다.
                // 모든 요청에 항상 붙이려면 Builder의 defaultTools(...)를 쓴다.
                //
                // Tool 개수가 많아질수록 프롬프트에 실리는 함수 정의도 늘어
                // 토큰 비용이 증가하고 모델의 선택 정확도가 떨어진다.
                // 요청 성격에 맞는 Tool만 골라 넘기는 편이 좋다.
                .tools(courseTools)
                // call() 한 번처럼 보이지만, 내부에서
                // [모델 호출 → Tool 실행 → 결과 재전달 → 모델 재호출]
                // 루프가 자동으로 돌고, 최종 문장만 반환된다.
                .call()
                .content();
    }
}
