# SessionEchoServer 구현 가이드

## Step 1: SimpleEchoServer 이해하기

### 핵심 개념
- **ServerSocket**: 클라이언트 연결 요청을 받는 서버 소켓 (포트 대기)
- **accept()**: 클라이언트 연결 요청 대기 (블로킹) → Socket 객체 반환
- **Socket**: 클라이언트와의 TCP 연결 (한 번의 통신만 처리 후 종료)

### SimpleEchoServer 흐름
```
1. ServerSocket(port) 생성
2. while(true) {
     - accept() : 클라이언트 대기
     - readLine() : 메시지 읽기 (블로킹)
     - println() : 응답 전송
     - close() : 연결 종료
   }
```

**특징**: 한 클라이언트 처리 후 종료 → 다음 클라이언트 대기

---

## Step 2: SessionEchoServer 구현하기

### 문제점: SimpleEchoServer는 한 번의 메시지만 처리
- 여러 메시지를 주고받으려면?
- 여러 클라이언트를 동시에 처리하려면?

### 해결책: Thread 활용

#### 단계 1: ClientHandler 클래스 정의
```java
static class ClientHandler implements Runnable {
    private Socket clientSocket;
    
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }
    
    @Override
    public void run() {
        // TODO: 클라이언트와 통신하는 로직 작성
    }
}
```

#### 단계 2: run() 메서드 구현
1. **스트림 생성**
   - `clientSocket.getInputStream()` → BufferedReader로 래핑
   - `clientSocket.getOutputStream()` → PrintWriter로 래핑

2. **메시지 루프** (SimpleEchoServer와의 차이)
   ```java
   while ((line = reader.readLine()) != null) {
       // 메시지 처리
       // "quit" 확인
       // 응답 전송
   }
   ```

3. **리소스 정리**
   - try-with-resources 사용
   - finally에서 소켓 close()

#### 단계 3: main() 메서드 수정
```java
// 각 클라이언트마다 새로운 스레드 생성
Thread clientHandler = new Thread(new ClientHandler(clientSocket));
clientHandler.start();  // 블로킹 없이 즉시 복귀
```

---

## Step 3: 핵심 개념 정리

### ServerSocket vs Socket
| 항목 | ServerSocket | Socket |
|------|-------------|--------|
| 역할 | 포트 대기 | 클라이언트와 통신 |
| 생성 | `new ServerSocket(port)` | `accept()` 반환 |
| 사용처 | 메인 스레드 | 워커 스레드 |

### Thread의 역할
- **Main Thread**: `accept()` → 계속 새 클라이언트 대기
- **Worker Thread**: 각 클라이언트와 메시지 송수신
- **이점**: 여러 클라이언트를 동시에 처리 가능

### start() vs run()
```java
clientHandler.start();  // ✅ 새 스레드 생성 (비동기)
clientHandler.run();    // ❌ 현재 스레드에서 실행 (블로킹)
```

---

## Step 4: 구현 체크리스트

- [ ] ClientHandler 클래스 정의 (implements Runnable)
- [ ] 생성자: Socket 객체 저장
- [ ] run() 메서드: try-with-resources로 스트림 생성
- [ ] while 루프: readLine() != null 조건
- [ ] "quit" 명령 처리
- [ ] echo 응답 전송
- [ ] main(): Thread 생성 및 start() 호출
- [ ] 테스트: 여러 클라이언트 동시 접속

---

## Step 5: 테스트 방법

```bash
# 터미널 1: 서버 실행
java SessionEchoServer

# 터미널 2, 3, 4: 클라이언트 접속 (동시 실행)
telnet localhost 8080
# 또는
nc localhost 8080
```

각 터미널에서 메시지 전송 → 모두 독립적으로 처리됨을 확인

---

---

# 실습: SimpleEcho → HttpEcho 변환 가이드

## 1. 현재 SimpleEcho의 메시지 구조

### SimpleEchoClient가 보내는 것
```
Hello Server!\n
```
단순 텍스트 한 줄입니다. 형식 규약이 없습니다.

### SimpleEchoServer가 응답하는 것
```
Echo: Hello Server!\n
```
받은 내용 앞에 "Echo: " 를 붙여 그대로 돌려줍니다.

---

## 2. HTTP 프로토콜로 바꾸면 메시지가 어떻게 달라지나

### HttpEchoClient가 보내는 HTTP Request 메시지
```
GET /users HTTP/1.1\r\n
Host: localhost\r\n
Connection: close\r\n
\r\n
```

| 구성 요소 | 예시 | 설명 |
|-----------|------|------|
| 요청 라인 | `GET /users HTTP/1.1` | 메서드 + 경로 + HTTP 버전 |
| 헤더 | `Host: localhost` | 접속 대상 서버 |
| 헤더 | `Connection: close` | 응답 후 연결 종료 요청 |
| 빈 줄 | `\r\n` | 헤더 끝을 나타내는 구분선 (필수) |

> 핵심: 줄바꿈이 반드시 `\r\n` (CR+LF) 이어야 합니다. HTTP/1.1 표준(RFC 2616)이 이를 강제합니다.

### HttpEchoServer가 돌려주는 HTTP Response 메시지
```
HTTP/1.1 200 OK\r\n
Content-Type: application/json; charset=UTF-8\r\n
Content-Length: 83\r\n
Connection: close\r\n
\r\n
{"users":[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"},{"id":3,"name":"Charlie"}]}
```

| 구성 요소 | 예시 | 설명 |
|-----------|------|------|
| 상태 라인 | `HTTP/1.1 200 OK` | HTTP 버전 + 상태코드 + 메시지 |
| 헤더 | `Content-Type: application/json` | 본문 형식 |
| 헤더 | `Content-Length: 83` | 본문 바이트 수 |
| 헤더 | `Connection: close` | 응답 후 연결 종료 |
| 빈 줄 | `\r\n` | 헤더 끝 구분선 (필수) |
| 본문 | `{"users":[...]}` | JSON 데이터 |

### 경로별 응답 목록

| 요청 경로 | 상태코드 | 응답 JSON |
|-----------|----------|-----------|
| `GET /` | 200 OK | `{"service":"HttpEchoServer","status":"running","port":8080}` |
| `GET /users` | 200 OK | `{"users":[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"},{"id":3,"name":"Charlie"}]}` |
| `GET /time` | 200 OK | `{"time":"2026-06-30T21:17:03"}` |
| `GET /기타` | 404 Not Found | `{"error":"경로를 찾을 수 없습니다","path":"/기타"}` |

---

## 3. SimpleEcho → HttpEcho 변환 실습

### 변환 포인트 비교표

| 항목 | SimpleEcho | HttpEcho |
|------|-----------|----------|
| 클라이언트 전송 | `writer.println("Hello Server!")` | `writer.print("GET /users HTTP/1.1\r\n")` ... |
| 줄바꿈 | `\n` (println 자동) | 반드시 `\r\n` 직접 작성 |
| 서버 수신 | 첫 줄만 읽음 | 빈 줄이 나올 때까지 헤더를 반복 읽기 |
| 서버 응답 | `"Echo: " + line` | 상태 라인 + 헤더 + 빈 줄 + JSON 본문 |
| 본문 끝 감지 | 연결 종료(null) 대기 | `Connection: close` → 서버 소켓 close 시 null |

---

## 4. SimpleEchoServer.java 수정 방법 (3단계)

### Before (원본)
```java
String line = reader.readLine();          // 한 줄만 읽음
writer.println("Echo: " + line);          // 단순 텍스트 응답
```

### Step 1. 요청 라인 읽기
```java
// "GET /users HTTP/1.1" 형태로 첫 줄을 읽는다
String requestLine = reader.readLine();
```

### Step 2. 나머지 헤더를 빈 줄이 나올 때까지 읽기
```java
// HTTP 헤더는 빈 줄(\r\n)이 나올 때까지 계속된다
String headerLine;
while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
    System.out.println("헤더: " + headerLine);
}
// 빈 줄 도착 = 헤더 끝
```

### Step 3. 경로를 파싱하고 JSON으로 응답하기
```java
// "GET /users HTTP/1.1" → 공백 기준 분리 → [0]=GET, [1]=/users
String path = requestLine.split(" ")[1];

String body = switch (path) {
    case "/users" -> "{\"users\":[{\"id\":1,\"name\":\"Alice\"}]}";
    default       -> "{\"error\":\"not found\"}";
};

// 반드시 이 순서: 상태라인 → 헤더 → 빈줄 → 본문
writer.print("HTTP/1.1 200 OK\r\n");
writer.print("Content-Type: application/json\r\n");
writer.print("Content-Length: " + body.getBytes().length + "\r\n");
writer.print("Connection: close\r\n");
writer.print("\r\n");          // 헤더 끝 빈 줄 (필수!)
writer.print(body);
writer.flush();
```

---

## 5. SimpleEchoClient.java 수정 방법 (3단계)

### Before (원본)
```java
writer.println("Hello Server!");          // 단순 텍스트 전송
String response = reader.readLine();      // 한 줄만 읽음
```

### Step 1. GET 요청을 \r\n 으로 작성하여 전송
```java
writer.print("GET /users HTTP/1.1\r\n");
writer.print("Host: localhost\r\n");
writer.print("Connection: close\r\n");
writer.print("\r\n");    // 헤더 끝 빈 줄 (필수!)
writer.flush();
```

### Step 2. 상태 라인 읽기
```java
// "HTTP/1.1 200 OK"
String statusLine = reader.readLine();
System.out.println("상태: " + statusLine);
```

### Step 3. 헤더 읽고 본문 읽기
```java
// 빈 줄이 나올 때까지 헤더 읽기
String headerLine;
while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
    System.out.println("헤더: " + headerLine);
}

// Connection: close 이므로 서버 종료 시 readLine()이 null 반환
StringBuilder body = new StringBuilder();
String line;
while ((line = reader.readLine()) != null) {
    body.append(line);
}
System.out.println("JSON: " + body);
```

---

## 6. HTTP 메시지 흐름 전체 그림

```
[클라이언트]                          [서버]

TCP 연결 수립 ──────────────────────▶ accept()

"GET /users HTTP/1.1\r\n"
"Host: localhost\r\n"
"Connection: close\r\n"
"\r\n"                ──────────────▶ 요청 라인 파싱
                                       헤더 읽기 (빈 줄까지)
                                       경로 "/users" 확인
                                       JSON 생성

                      ◀──────────────  "HTTP/1.1 200 OK\r\n"
                                       "Content-Type: ...\r\n"
                                       "Content-Length: 83\r\n"
                                       "Connection: close\r\n"
                                       "\r\n"
                                       {"users":[...]}

상태코드 파싱
헤더 읽기
JSON 본문 읽기

TCP 연결 종료 ◀─────────────────────  소켓 close()
```

## 7. 실습 완성 확인 방법

```bash
# 터미널 1: 서버 실행
java HttpEchoServer

# 터미널 2: 클라이언트 실행
java HttpEchoClient

# 또는 curl로 실제 HTTP 도구와 호환 확인
curl http://localhost:8080/users
curl http://localhost:8080/time
curl http://localhost:8080/
```
