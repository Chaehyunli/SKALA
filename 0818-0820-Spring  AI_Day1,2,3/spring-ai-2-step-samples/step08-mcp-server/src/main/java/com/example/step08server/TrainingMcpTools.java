package com.example.step08server;

import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * STEP 08 (Server) - MCP로 외부에 공개할 Tool
 *
 * <p>STEP 07의 {@code @Tool}과 목적은 같지만, 노출 경로가 다르다.
 * <ul>
 *   <li>{@code @Tool}     : 같은 JVM 안의 ChatClient에게 노출 (In-Process)</li>
 *   <li>{@code @McpTool}  : MCP 프로토콜을 통해 <b>다른 프로세스/다른 서버</b>에 노출</li>
 * </ul>
 *
 * <p>이 애노테이션들이 스캔되려면 application.yml에서
 * {@code spring.ai.mcp.server.annotation-scanner.enabled: true}가 켜져 있어야 한다.
 */
@Component
public class TrainingMcpTools {

    /** 데모용 강의실 배정 데이터. 실무에서는 DB나 시설 관리 API를 조회한다. */
    private final Map<String, String> rooms = Map.of(
            "Spring AI", "A-301",
            "Spring Boot", "B-201",
            "Kubernetes", "C-401"
    );

    /**
     * MCP Tool 정의.
     *
     * <p>{@code name} : 프로토콜 상에서 이 도구를 식별하는 고유 이름.
     * 메서드 이름과 별개로 명시할 수 있어, Java 메서드명을 바꿔도
     * 외부 계약(name)은 유지할 수 있다. 관례상 snake_case를 쓴다.
     *
     * <p>{@code description} : 클라이언트 쪽 LLM이 이 도구를 쓸지 판단하는 근거.
     * 서버와 클라이언트가 분리되어 있어도 판단은 여전히 LLM이 하므로,
     * 설명을 명확히 쓰는 원칙은 STEP 07과 완전히 같다.
     *
     * <p>{@code @McpToolParam(required = true)} :
     * 이 인자는 반드시 채워야 한다고 스키마에 표시한다.
     * 모델이 값을 못 찾으면 사용자에게 되물어보게 유도된다.
     */
    @McpTool(
            name = "find_training_room",
            description = "교육 과정 이름으로 강의실을 조회한다.")
    public String findTrainingRoom(
            @McpToolParam(
                    description = "교육 과정 이름",
                    required = true)
            String courseName) {

        // getOrDefault : 없는 과정이어도 예외 대신 안내 문구를 돌려준다.
        // 원격 호출에서 예외를 던지면 클라이언트 쪽에서 오류로 처리되어
        // 대화가 끊기므로, 정상 응답 형태로 사실을 전달하는 편이 낫다.
        return rooms.getOrDefault(
                courseName,
                "해당 과정의 강의실 정보가 없습니다."
        );
    }
}
