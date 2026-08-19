# STEP 10 - Observability

```bash
./gradlew :step10-observability:bootRun
```

AI 호출:

```bash
curl "http://localhost:8110/api/chat?message=Spring%20AI를%20한문장으로%20설명해줘"
```

Actuator:

```bash
curl "http://localhost:8110/actuator/health"
curl "http://localhost:8110/actuator/metrics"
curl "http://localhost:8110/actuator/prometheus"
```

Prompt/Completion 원문 로깅은 민감정보 노출 위험 때문에 기본적으로 꺼 둡니다.
