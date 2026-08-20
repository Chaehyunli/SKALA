# 11_승인게이트

**11장 실습 · 승인 게이트와 감사 로깅**

## 실행

```bash
export OPENAI_API_KEY="sk-..."
./gradlew bootRun          # VS Code 는 F5
```

## 확인

```bash
curl 'localhost:8080/lab11/ask?q=초코바 3개 주문해줘'
```

Swagger UI — <http://localhost:8080/swagger-ui.html>

## 이 폴더에 있는 것

- `SnackOrderController.java`
- `SnackTools.java`
- `Tickets.java`
- `ToolAudit.java`

교재의 「실습 코드」·「실행·테스트」 장표와 파일이 그대로 대응한다.
막히면 `../00_참조예제/` 의 같은 주제 패키지를 열어 비교해 본다.
