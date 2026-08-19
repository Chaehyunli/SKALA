package com.example.step08client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * STEP 08 (Client) - 원격 MCP 도구를 사용하는 컨트롤러
 *
 * <p>STEP 07의 {@code ToolController}와 비교해 보자.
 * 바뀐 것은 {@code .tools()}에 넘기는 대상뿐이다.
 * <ul>
 *   <li>STEP 07 : {@code .tools(courseTools)}  → 로컬 객체</li>
 *   <li>STEP 08 : {@code .tools(mcpTools)}     → 원격 서버가 제공하는 도구 목록</li>
 * </ul>
 * 도구가 다른 서버에 있어도 호출 코드가 동일하다는 것이 MCP 표준화의 효과다.
 *
 * <p><b>전체 호출 흐름</b>
 * <pre>
 *   사용자 → 클라이언트(8109) → OpenAI (도구 목록 포함해 질의)
 *          ← "find_training_room(courseName=Spring AI) 실행해줘"
 *          → MCP 서버(8108) 호출 → "A-301"
 *          → OpenAI 재호출(결과 전달) → 최종 문장 → 사용자
 * </pre>
 *
 * <p><b>테스트</b> : GET /api/mcp?message=Spring AI 과정 강의실이 어디야?
 */
@RestController
public class McpChatController {

    private final ChatClient chatClient;

    /**
     * MCP 클라이언트 자동 구성이 등록해 주는 Bean.
     * 접속한 MCP 서버들의 도구를 Spring AI의 Tool 형식으로 변환해 담고 있다.
     * 서버를 여러 개 연결하면 그 도구들이 모두 여기에 합쳐진다.
     */
    private final ToolCallbackProvider mcpTools;

    public McpChatController(
            ChatClient.Builder builder,
            ToolCallbackProvider mcpTools) {

        this.chatClient = builder.build();
        this.mcpTools = mcpTools;
    }

    @GetMapping("/api/mcp")
    public String ask(@RequestParam String message) {
        return chatClient
                .prompt()
                .user(message)
                // 원격 도구 목록을 이번 요청에 노출한다.
                // 실제 실행은 Spring AI의 MCP 클라이언트가 HTTP로 8108 서버를 호출해 처리한다.
                .tools(mcpTools)
                .call()
                .content();
    }
}
