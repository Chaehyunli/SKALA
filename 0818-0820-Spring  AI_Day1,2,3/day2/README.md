# day2

**Day 1 실습(이월) · 주문 요약** + **Day 2 실습 · 문서 Q&A(RAG)**

## 실행

```bash
export OPENAI_API_KEY="sk-..."
./gradlew bootRun          # VS Code 는 F5
```

## 확인 — Day 1 이월: 주문 요약

```bash
curl 'localhost:8080/lab1/orders/12345/summary?userId=user1'   # 200 + 한 문장 요약
curl 'localhost:8080/lab1/orders/99999/summary?userId=user1'   # 404 — 99999는 user2 소유
curl 'localhost:8080/lab1/orders/00000/summary?userId=user1'   # 404 — 없는 주문, 위와 같은 응답
```

## 확인 — Day 2: 문서 Q&A

벡터스토어가 인메모리라 앱을 재시작하면 비워진다. `retrieve`·`ask` 전에 `ingest`를 먼저 호출한다.

```bash
curl -X POST 'localhost:8080/lab2/ingest'

curl -G 'localhost:8080/lab2/retrieve' --data-urlencode 'q=반품 기한' --data-urlencode 'threshold=0.3'

curl -X POST 'localhost:8080/lab2/ask' -H 'Content-Type: application/json' \
  -d '{"question":"단순 변심 반품은 며칠 이내인가요?"}'
```

토큰 사용량까지 보려면 `spring-boot-starter-actuator` 를 추가하고
`management.endpoints.web.exposure.include: metrics` 를 켠 뒤
`/actuator/metrics/ai.tokens` 를 확인한다 (기본 구성에는 빠져 있다).

Swagger UI — <http://localhost:8080/swagger-ui.html>

## 이 폴더에 있는 것

```
config/
  Lab1AiConfig.java          — 요약 전용 ChatClient 빈 (온도 0, maxTokens 120)
  Lab2AiConfig.java          — 근거 답변 전용 ChatClient 빈 (온도 0, 거절·인용 지시)
  Lab2VectorStoreConfig.java — 인메모리 VectorStore 빈
domain/
  Order.java, OrderStatus.java
service/
  OrderRepository.java, OrderNotFoundException.java, OrderSummaryService.java
  Lab2IngestService.java     — 읽기 → 분할 → 메타데이터 → 저장 (재인제스트 시 중복 방지)
  Lab2RetrieveService.java   — 검색 로직 (컨트롤러·Ask 서비스가 공유)
  Lab2AskService.java        — 근거 없으면 모델 호출 스킵, 있으면 구조화 출력으로 답변
web/
  controller/  — OrderSummaryController, Lab2IngestController, Lab2RetrieveController, Lab2AskController
  dto/         — SummaryResponse, ErrorResponse, IngestResult, Chunk, AskRequest, AnswerDto
  exception/   — Lab1ExceptionHandler (예외 → 응답 변환은 한 곳)
resources/
  lab2-docs/   — return-policy.md, shipping-policy.md, membership.md, golden.json
```

교재의 「Day 2 실습」 Step 1~5 장표와 파일이 그대로 대응한다.
