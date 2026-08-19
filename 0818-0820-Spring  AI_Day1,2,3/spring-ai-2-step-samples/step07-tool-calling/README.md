# STEP 07 - Tool Calling

```bash
./gradlew :step07-tool-calling:bootRun
```

```bash
curl "http://localhost:8107/api/tool?message=Spring%20AI와%20Kubernetes%20과정의%20교육시간을%20확인해서%20합계를%20알려줘"
```

Spring AI 2.0의 `ChatClient`는 ToolCallingAdvisor를 자동 등록하여 Tool 실행 루프를 처리합니다.
