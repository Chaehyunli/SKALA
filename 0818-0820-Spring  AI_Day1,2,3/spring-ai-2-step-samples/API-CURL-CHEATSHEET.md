# API 호출 Cheatsheet

```bash
# STEP 01
curl "http://localhost:8101/api/chat?message=Spring%20AI란?"

# STEP 02
curl "http://localhost:8102/api/course?subject=Spring%20AI"

# STEP 03
curl -N "http://localhost:8103/api/chat/stream?message=RAG를%20설명해줘"

# STEP 04
curl "http://localhost:8104/api/advisor?message=Advisor란?"

# STEP 05
curl "http://localhost:8105/api/chat?conversationId=user1&message=내%20이름은%20홍길동이야"
curl "http://localhost:8105/api/chat?conversationId=user1&message=내%20이름이%20뭐야?"

# STEP 06
curl "http://localhost:8106/api/rag?question=Advisor의%20역할은?"

# STEP 07
curl "http://localhost:8107/api/tool?message=Spring%20AI와%20Kubernetes%20교육시간을%20합산해줘"

# STEP 08
curl "http://localhost:8109/api/mcp?message=Spring%20AI%20과정의%20강의실을%20알려줘"

# STEP 10
curl "http://localhost:8110/api/chat?message=Spring%20AI를%20요약해줘"
curl "http://localhost:8110/actuator/prometheus"
```
