# STEP 05 - Chat Memory

```bash
./gradlew :step05-memory:bootRun
```

1차 질문:

```bash
curl "http://localhost:8105/api/chat?conversationId=user1&message=내%20이름은%20홍길동이야"
```

2차 질문:

```bash
curl "http://localhost:8105/api/chat?conversationId=user1&message=내%20이름이%20뭐야?"
```

`conversationId`가 달라지면 별도의 대화로 처리됩니다.
