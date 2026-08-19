# 06_별점카드

**6장 실습 · 구조화 출력(entity)**

## 실행

```bash
export OPENAI_API_KEY="sk-..."
./gradlew bootRun          # VS Code 는 F5
```

## 확인

```bash
curl 'localhost:8080/lab6/review?text=연출은 좋은데 결말이 아쉬웠다'
```

Swagger UI — <http://localhost:8080/swagger-ui.html>

## 이 폴더에 있는 것

- `ReviewLab.java`

교재의 「실습 코드」·「실행·테스트」 장표와 파일이 그대로 대응한다.
막히면 `../00_참조예제/` 의 같은 주제 패키지를 열어 비교해 본다.
