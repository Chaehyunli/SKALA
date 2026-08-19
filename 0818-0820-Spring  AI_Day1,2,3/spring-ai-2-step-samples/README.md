# Spring AI 2.0 단계별 샘플 코드

실습용으로 Spring AI 기능을 단계별로 분리한 Gradle 멀티 프로젝트입니다.

## 기준 환경

- Java 21
- Spring Boot 4.0.7
- MVC 모듈은 Boot 4 권장 `spring-boot-starter-webmvc` 사용
- Spring AI 2.0.0
- OpenAI
- RAG: PostgreSQL + pgvector
- MCP: Streamable HTTP

## API Key

```bash
export OPENAI_API_KEY="YOUR_API_KEY"
```

API Key는 코드나 `application.yml`에 직접 넣지 않고 환경변수로 전달합니다.


## Gradle dependency management

모든 subproject에는 `org.springframework.boot`와 함께 `io.spring.dependency-management`를 적용합니다.
Spring Boot 플러그인이 해당 플러그인을 감지하면 사용 중인 Spring Boot 버전의 `spring-boot-dependencies` BOM을 자동 import하므로, `spring-boot-starter-*` 의존성에 버전을 직접 적지 않아도 됩니다.

```groovy
subprojects {
    apply plugin: 'java'
    apply plugin: 'org.springframework.boot'
    apply plugin: 'io.spring.dependency-management'
}
```

## 실행 방식

루트 디렉터리에서 원하는 모듈만 실행합니다.

```bash
gradle :step01-basic-chat:bootRun
```

IDE에서는 루트 폴더를 Gradle 프로젝트로 열고 각 `*Application` 클래스를 실행해도 됩니다.

## 학습 순서

| 단계 | 모듈 | 핵심 |
|---|---|---|
| 01 | step01-basic-chat | ChatClient 기본 호출 |
| 02 | step02-structured-output | entity() + Java record + Schema Validation |
| 03 | step03-streaming | Flux + SSE |
| 04 | step04-advisor | SimpleLoggerAdvisor |
| 05 | step05-memory | MessageWindowChatMemory |
| 06 | step06-rag-pgvector | Embedding + PGVector + QuestionAnswerAdvisor |
| 07 | step07-tool-calling | @Tool + @ToolParam |
| 08-1 | step08-mcp-server | @McpTool 기반 MCP Server |
| 08-2 | step08-mcp-client | MCP Tool을 ChatClient에서 호출 |
| 09 | step09-evaluation | RetrievalAugmentationAdvisor + RelevancyEvaluator |
| 10 | step10-observability | Actuator + Micrometer + AI Observability |

## 포트

- 8101: Basic Chat
- 8102: Structured Output
- 8103: Streaming
- 8104: Advisor
- 8105: Memory
- 8106: RAG
- 8107: Tool Calling
- 8108: MCP Server
- 8109: MCP Client
- 8110: Observability

## Spring AI 2.0 모듈 분리 주의

Spring AI 2.0에서는 기능별 모듈 분리가 중요합니다.

```text
PGVector 구현
  -> spring-ai-starter-vector-store-pgvector

QuestionAnswerAdvisor
  -> spring-ai-vector-store-advisor

RetrievalAugmentationAdvisor
  -> spring-ai-rag
```

따라서 VectorStore Starter만 추가했다고 RAG Advisor 클래스까지 자동으로 들어오는 것은 아닙니다.

## 공식 문서

- https://docs.spring.io/spring-ai/reference/getting-started.html
- https://docs.spring.io/spring-ai/reference/api/chatclient.html
- https://docs.spring.io/spring-ai/reference/api/chat-memory.html
- https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html
- https://docs.spring.io/spring-ai/reference/api/tools.html
- https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
- https://docs.spring.io/spring-ai/reference/api/testing.html
- https://docs.spring.io/spring-ai/reference/observability/index.html


```bash
./gradlew clean
./gradlew :step01-basic-chat:bootRun
```
