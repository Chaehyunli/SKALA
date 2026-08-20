# 08_위키QnA

**8장 실습 · RAG 기본(인제스트·검색·답변)**

## 실행

```bash
export OPENAI_API_KEY="sk-..."
./gradlew bootRun          # VS Code 는 F5
```

## 확인

```bash
curl -X POST localhost:8080/lab8/ingest
```

Swagger UI — <http://localhost:8080/swagger-ui.html>

## 이 폴더에 있는 것

- `VectorStoreConfig.java`
- `WikiRag.java`
- `WikiRagController.java`
- `resources/lab8-docs/`

교재의 「실습 코드」·「실행·테스트」 장표와 파일이 그대로 대응한다.
막히면 `../00_참조예제/` 의 같은 주제 패키지를 열어 비교해 본다.
