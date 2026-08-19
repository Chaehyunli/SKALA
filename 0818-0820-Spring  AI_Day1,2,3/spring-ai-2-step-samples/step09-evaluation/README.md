# STEP 09 - RAG Evaluation

STEP 06의 PostgreSQL/PGVector 컨테이너를 먼저 실행합니다.

```bash
cd step06-rag-pgvector
docker compose up -d
cd ..
```

테스트:

```bash
./gradlew :step09-evaluation:test
```

`RelevancyEvaluator`가 질문 + 검색 Context + AI 답변의 관련성을 평가합니다.

주의: Evaluator 역시 LLM 호출을 수행하므로 API 사용량이 발생합니다.
