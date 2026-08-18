# 01_간식추천_3계층

**1장 실습 · 계층 분리 (AI 없이 · 키 불필요)**

## 실행

```bash
./gradlew bootRun          # VS Code 는 F5
```

## 확인

```bash
curl 'localhost:8080/lab0/snack?mood=피곤'
```

Swagger UI — <http://localhost:8080/swagger-ui.html>

## 이 폴더에 있는 것

- `domain/Snack.java`
- `service/SnackRepository.java`
- `service/SnackService.java`
- `web/SnackController.java`
- `web/SnackResponse.java`

교재의 「실습 코드」·「실행·테스트」 장표와 파일이 그대로 대응한다.
막히면 `../00_참조예제/` 의 같은 주제 패키지를 열어 비교해 본다.
