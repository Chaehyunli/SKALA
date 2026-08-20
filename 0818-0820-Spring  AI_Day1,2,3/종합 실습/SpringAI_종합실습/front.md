# 타임리프 테스트 프런트 설계

목적: Swagger 없이도 브라우저에서 바로 눌러볼 수 있는 테스트용 대시보드. 로그인 → 채팅 → (우측 상단에 현재 user·sessionId 표시)까지, 사용자와 세션이 1:1로 고정되어 있어 "누구 대화인지 헷갈릴 일"이 없게 만드는 게 핵심이다.

## 1. 핵심 결정 — sessionId는 userId로부터 결정론적으로 계산한다

실제 인증에 쓰는 `JSESSIONID`(서블릿 세션 쿠키)는 건드리지 않는다 — 그건 Tomcat이 관리하는 랜덤 토큰이어야 세션 하이재킹을 막을 수 있다(예측 가능한 값으로 바꾸면 로그인 없이 `Cookie: JSESSIONID=user1`만 붙여도 위장 로그인이 가능해진다).

대신 앱에서 쓰는 `sessionId`(대화 메모리 구분용 문자열, `AskRequest.sessionId`)를 **userId의 순수 함수**로 정의한다.

```
sessionId = userId + "-session"
```

- **결정론적** — 같은 userId는 로그아웃 후 다시 로그인해도 항상 같은 sessionId를 받는다. 그래서 "user1의 대화는 모두 같은 세션"이 자동으로 성립하고, 대화 이력도 로그인 세션이 바뀌어도 계속 이어진다.
- **1:1** — userId 하나당 sessionId 하나뿐이다. 별도 저장소나 발급 로직이 필요 없다(그냥 문자열 조합 함수 하나).
- **서버가 강제** — 클라이언트가 다른 sessionId를 보내면(다른 사람 것이든, 아무 값이든) 요청을 거부한다. 이미 있는 `requireSameUser`(userId 검증)와 완전히 같은 패턴으로 `sessionId` 검증을 하나 더 추가하면 된다.

## 2. 백엔드 변경 사항

### 2-1. `UserSessionService` — sessionId 파생·검증 추가

```java
public String deriveSessionId(String userId) {
    return userId + "-session";
}

/** 요청의 sessionId가 없으면 채워주고, 있으면 로그인한 사용자 것과 일치하는지 검증한다. */
public String requireOwnSessionId(HttpSession session, String requestedSessionId) {
    String userId = currentUserId(session);
    String expected = deriveSessionId(userId);
    if (requestedSessionId != null && !requestedSessionId.isBlank() && !requestedSessionId.equals(expected)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 세션이 아닙니다.");
    }
    return expected;
}
```

`requireSameUser`(userId 검증)와 나란히 두는 게 자연스럽다 — 이미 있는 "본인 계정만" 원칙을 sessionId까지 확장하는 것뿐이다.

### 2-2. `ChatController` — 세 메서드 모두 sessionId를 검증된 값으로 교체

```java
@PostMapping
public AnswerDto ask(@Valid @RequestBody AskRequest request, HttpSession session) {
    String userId = userSessionService.requireSameUser(session, request.userId());
    String sessionId = userSessionService.requireOwnSessionId(session, request.sessionId());
    return chatService.ask(userId, request.message(), sessionId);
}
```

`stream()`, `history()`도 동일하게 `requireOwnSessionId`를 거치도록 바꾼다. `HelpDeskChatService.conversationId()`는 손댈 필요 없음 — 이미 검증된 값만 받는다.

### 2-3. `AskRequest.sessionId` — 필수 아님, 오히려 생략을 권장

지금은 클라이언트가 자유 문자열을 넣게 돼 있는데, 이제는:
- 비워두면 서버가 `deriveSessionId(userId)`로 자동 채움 (프런트는 아예 이 필드를 안 보내도 됨)
- 값을 넣으면 본인 것과 일치하는지만 검증 (Swagger 테스트 시 실수로 남의 세션 흉내를 못 내게)

## 3. 화면 설계

### 3-1. 로그인 페이지 (`login.html`) — 기존 구조 재사용

- `user1`, `user2` 기본 목록 + 사용자 추가 폼은 이미 있는 `UserPageController`/`UserRepository` 그대로 쓴다. 변경 없음.

### 3-2. 채팅 페이지 (`chat.html`) — 우측 상단에 user·sessionId 표시

```html
<header>
  <div>
    <h2>SKALA HelpDesk</h2>
    <p>사용자: <strong th:text="${#session.getAttribute('helpdesk.userId')}">user1</strong>
       · 세션: <strong th:text="${#session.getAttribute('helpdesk.userId')} + '-session'">user1-session</strong></p>
  </div>
  <form method="post" action="/logout"><button>사용자 변경</button></form>
</header>
```

`sessionId`를 서버 세션 attribute로 별도 저장할 필요 없이, 화면에서도 `userId + '-session'`을 그 자리에서 계산하면 된다 — 저장소를 하나 더 두지 않는 게 원칙에 맞다(userId 하나만 진실의 원천).

### 3-3. SSE 요청 시 sessionId 생략

`chat.html`의 fetch body에서 `sessionId` 필드를 아예 뺀다 — 서버가 자동으로 채워준다.

```js
body: JSON.stringify({ userId, message })  // sessionId 없음 — 서버가 채운다
```

## 4. 검증 시나리오

| 시도 | 기대 결과 |
|---|---|
| user1 로그인 → sessionId 생략하고 `/api/chat` 호출 | 200, 내부적으로 `user1-session`으로 처리 |
| user1 로그인 → `sessionId: "user1-session"`으로 호출 | 200 (본인 것이므로 통과) |
| user1 로그인 → `sessionId: "user2-session"`으로 호출 | 403 (남의 세션) |
| user1 로그인 → `sessionId: "아무값"`으로 호출 | 403 (본인 것과 불일치) |
| user1 로그아웃 → 다시 로그인 → 이전 대화 이어서 확인 | 이전 대화 그대로 유지 (sessionId가 항상 동일하므로) |

## 5. 기존 Notion 보고서에 미치는 영향 — 반영 필요

이 설계를 실제로 적용하면, 지금 보고서 4-1(`sessionId: "user1Session"`)·4-6(`sessionId: "redteam1"`)처럼 **한 사용자가 여러 sessionId를 쓰던 시나리오가 전부 막힌다.** 앞으로는:
- 모든 캡처에서 `sessionId` 필드를 아예 빼거나 `user1-session`으로 통일
- 레드팀 프롬프트(4-6)도 이제 메인 대화와 같은 세션에 섞여 들어감(기능상 문제는 없음 — `SafeGuardAdvisor`는 세션이 아니라 메시지 내용으로 차단하므로)

→ front.md 구현이 끝나면 Notion 4-1/4-3/4-6의 요청 예시들을 이 정책에 맞게 다시 다듬어야 한다.

## 6. 구현 체크리스트

- [ ] `UserSessionService`에 `deriveSessionId`, `requireOwnSessionId` 추가
- [ ] `ChatController.ask/stream/history` 세 곳 모두 `requireOwnSessionId`로 교체
- [ ] `chat.html` 헤더에 세션 표시 줄 추가, fetch body에서 `sessionId` 제거
- [ ] `AskRequest.sessionId`의 `@Schema` 설명을 "생략 시 로그인한 사용자의 세션으로 자동 지정됨"으로 갱신
- [ ] Notion 보고서 4-1/4-3/4-6의 `sessionId` 값 정리
