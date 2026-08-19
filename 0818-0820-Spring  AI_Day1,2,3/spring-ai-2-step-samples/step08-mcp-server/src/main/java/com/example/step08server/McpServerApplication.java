package com.example.step08server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * STEP 08 (Server) - MCP 서버 애플리케이션 (포트 8108)
 *
 * <p><b>MCP(Model Context Protocol)란?</b><br>
 * AI 애플리케이션이 외부 도구/데이터에 접속하기 위한 <b>표준 프로토콜</b>이다.
 * "AI 세계의 USB-C"에 비유된다.
 *
 * <p>STEP 07의 Tool Calling은 Tool이 <b>같은 애플리케이션 안</b>에 있었다.
 * MCP는 그 Tool을 <b>독립된 서버로 분리</b>해 여러 AI 클라이언트가 공유하게 한다.
 *
 * <pre>
 *   STEP 07 : [ 앱 + Tool ]  ← Tool이 앱에 종속
 *   STEP 08 : [ AI 앱(클라이언트) ] ←MCP프로토콜→ [ MCP 서버 + Tool ]
 * </pre>
 *
 * <p><b>장점</b> : 도구를 한 번 만들어 두면 우리 앱, Claude Desktop, 다른 팀 서비스 등
 * MCP를 지원하는 모든 클라이언트가 그대로 쓸 수 있다.
 *
 * <p>이 모듈에는 OpenAI 의존성이 없다는 점에 주목하자.
 * MCP 서버는 <b>도구를 제공만</b> 하고 LLM을 직접 호출하지 않는다.
 *
 * <p><b>실행 순서</b> : 반드시 이 서버(8108)를 먼저 띄운 뒤 클라이언트(8109)를 실행한다.
 */
@SpringBootApplication
public class McpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
