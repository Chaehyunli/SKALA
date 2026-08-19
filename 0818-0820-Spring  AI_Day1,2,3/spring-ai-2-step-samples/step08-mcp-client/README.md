# STEP 08-2 - MCP Client

먼저 서버:

```bash
./gradlew :step08-mcp-server:bootRun
```

다른 터미널에서 클라이언트:

```bash
./gradlew :step08-mcp-client:bootRun
```

호출:

```bash
curl "http://localhost:8109/api/mcp?message=Spring%20AI%20과정의%20강의실이%20어디야?"
```
