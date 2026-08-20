# 14_SSE와추적ID

**14장 실습 · SSE와 추적 ID**

## 실행

```bash
export OPENAI_API_KEY="sk-..."
./gradlew bootRun          # VS Code 는 F5
```

## 확인

```bash
curl -N -X POST localhost:8080/lab14/stream -H 'Content-Type: application/json' -d '{"question":"자기소개"}'
```

Swagger UI — <http://localhost:8080/swagger-ui.html>

## 이 폴더에 있는 것

- `Lab14ExceptionHandler.java`
- `StreamLab.java`

교재의 「실습 코드」·「실행·테스트」 장표와 파일이 그대로 대응한다.
막히면 `../00_참조예제/` 의 같은 주제 패키지를 열어 비교해 본다.
