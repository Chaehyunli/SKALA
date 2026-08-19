# day2

**Day 2 실습 · 주문 요약 — ChatClient 빈 · Service · Controller · 예외 처리**

## 실행

```bash
export OPENAI_API_KEY="sk-..."
./gradlew bootRun          # VS Code 는 F5
```

## 확인

```bash
curl 'localhost:8080/lab1/orders/12345/summary?userId=user1'   # 200 + 한 문장 요약
curl 'localhost:8080/lab1/orders/99999/summary?userId=user1'   # 404 — 99999는 user2 소유
curl 'localhost:8080/lab1/orders/00000/summary?userId=user1'   # 404 — 없는 주문, 위와 같은 응답
```

토큰 사용량까지 보려면 `spring-boot-starter-actuator` 를 추가하고
`management.endpoints.web.exposure.include: metrics` 를 켠 뒤
`/actuator/metrics/ai.tokens` 를 확인한다 (기본 구성에는 빠져 있다).

Swagger UI — <http://localhost:8080/swagger-ui.html>

## 이 폴더에 있는 것

- `config/Lab1AiConfig.java` — 요약 전용 ChatClient 빈 (온도 0, maxTokens 120)
- `domain/Order.java`, `domain/OrderStatus.java`
- `service/OrderRepository.java`, `service/OrderNotFoundException.java`
- `service/OrderSummaryService.java` — 업무 흐름
- `web/OrderSummaryController.java`, `web/SummaryResponse.java` — 컨트롤러는 AI 를 모른다
- `web/Lab1ExceptionHandler.java`, `web/ErrorResponse.java` — 예외 → 응답 변환은 한 곳

교재의 「Day 2 실습」 Step 1~5 장표와 파일이 그대로 대응한다.
