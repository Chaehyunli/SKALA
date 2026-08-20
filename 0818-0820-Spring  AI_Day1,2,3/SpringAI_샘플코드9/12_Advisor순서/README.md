# 12_Advisor순서

**12장 실습 · Advisor 순서가 곧 정책**

## 실행

```bash
export OPENAI_API_KEY="sk-..."
./gradlew bootRun          # VS Code 는 F5
```

## 확인

```bash
curl 'localhost:8080/lab12/ask?q=회의 언제야&sessionId=s1'
```

Swagger UI — <http://localhost:8080/swagger-ui.html>

## 이 폴더에 있는 것

- `ChatMemoryConfig.java`
- `Lab12Config.java`
- `Lab12Controller.java`
- `VectorStoreConfig.java`
- `이모지Advisor.java`

교재의 「실습 코드」·「실행·테스트」 장표와 파일이 그대로 대응한다.
막히면 `../00_참조예제/` 의 같은 주제 패키지를 열어 비교해 본다.
