# STEP 01 - Basic Chat

```bash
./gradlew :step01-basic-chat:bootRun
curl "http://localhost:8101/api/chat?message=Spring%20AI란?"
```

핵심 코드:

```java
chatClient.prompt()
    .user(message)
    .call()
    .content();
```
