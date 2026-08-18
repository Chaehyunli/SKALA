# 04_말투바꾸기

**4장 실습 · 용도별 ChatClient 빈 두 개**

## 실행

```bash
export OPENAI_API_KEY="sk-..."
./gradlew bootRun          # VS Code 는 F5
```

## 확인

```bash
curl 'localhost:8080/lab4/tone?q=오늘 회의 30분 늦어요'
```

Swagger UI — <http://localhost:8080/swagger-ui.html>

## 이 폴더에 있는 것

- `ToneConfig.java`
- `ToneLab.java`

교재의 「실습 코드」·「실행·테스트」 장표와 파일이 그대로 대응한다.
막히면 `../00_참조예제/` 의 같은 주제 패키지를 열어 비교해 본다.
