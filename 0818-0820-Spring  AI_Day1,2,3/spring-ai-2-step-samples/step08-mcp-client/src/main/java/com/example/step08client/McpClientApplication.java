package com.example.step08client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * STEP 08 (Client) - MCP 클라이언트 애플리케이션 (포트 8109)
 *
 * <p>기동 시 application.yml에 설정된 MCP 서버(http://localhost:8108)에 접속해
 * "어떤 도구를 제공하니?"라고 물어보고(tools/list), 받아온 도구 목록을
 * {@code ToolCallbackProvider} Bean으로 만들어 등록한다.
 *
 * <p>따라서 <b>MCP 서버가 먼저 실행되어 있어야 한다.</b>
 * 서버가 없으면 기동 중 연결 오류가 발생하거나 사용할 도구가 없는 상태가 된다.
 */
@SpringBootApplication
public class McpClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpClientApplication.class, args);
    }
}
