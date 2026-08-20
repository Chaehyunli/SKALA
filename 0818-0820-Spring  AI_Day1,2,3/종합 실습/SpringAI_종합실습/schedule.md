# SKALA HelpDesk AI — 종합 실습 설계·진행 기록

day1~3에서 배운 RAG·Tool·Memory·Advisor(안전/관찰)를 하나로 조립하는 종합 실습.
원본 PDF 시나리오(이커머스 상담)는 예시일 뿐이고, 실제로는 **해외주식 모의투자 HelpDesk**로 도메인을 확장했다.

---

## 1. 시나리오

**SKALA HelpDesk AI — 해외주식 모의투자 상담 에이전트**

- 사용자는 로그인 화면에서 기본 사용자 `user1`·`user2`를 선택하거나 실습용 사용자를 추가한다. 각 사용자는 최초 접속 시 원화 1,000만 원 + 임의 초기 보유 종목(AAPL 10주, TSLA 5주)을 받는다.
- 채팅으로 규정을 묻고, 실시간 시세·환율로 매수/매도하고, 포트폴리오를 조회한다.
- DB 없이 서버 메모리에만 상태를 두어, **재시작하면 전원 초기 상태로 리셋**된다 (데모 편의).
- 매수 후 전체 해외주식의 현재 평가액 합계가 신고 기준액(USD 10,000) 이상이면 컴플라이언스 신고 티켓이 자동 생성되고, 담당자 승인 절차(+Gmail 알림)를 거친다.

## 2. 도메인 선정 과정 (왜 이렇게 됐는지)

1. 처음엔 "해외 출장 경비 정산 + 환율(Frankfurter)" 조합을 제안했다 — 무료·키 불필요 API라 제출 직전 API 이슈 리스크가 없다는 이유.
2. 사용자가 실시간 주가 API 추가를 요청 → **환율 + 주가**를 함께 쓰는 "해외주식 신고 컴플라이언스" 도메인으로 확장. 실제 회사에 흔한 "해외주식 보유·거래 신고 규정"(신고 기준액 USD 10,000)과 결합하면 RAG+Tool 2개를 한 질문 안에서 자연스럽게 조합할 수 있음.
3. 사용자가 SKALA Hub(스칼라 허브) 게시글을 근거 문서로 쓰고 싶어함 → 실사 결과 `/api/posts`가 Slack OAuth 로그인 토큰이 있어야만 열리는 비공개 API(403)로 확인, 개인 토큰을 프로젝트에 박아넣는 건 위험해서 **제외**하기로 결정.
4. 사용자가 "채팅으로 매수/매도까지 되냐"고 질문 → 이게 최종 도메인이 됨: 모의투자 시뮬레이션(매수/매도/포트폴리오 조회), DB 없이 서버 재시작 시 초기화되는 방식으로 확정.
5. 신고 알림은 `spring-boot-starter-mail` + `JavaMailSender`로 구현했다. 별도 Slack 봇 연동은 제출 범위에서 제외해, 실제로 검증 가능한 Gmail 알림에 집중한다.

## 3. 요구사항 ↔ Phase ↔ 검증 매트릭스

| 요구사항 | 어디서 채우나 | 검증 방법 |
|---|---|---|
| 문서 근거+출처 | RAG(신고·거래 규정) | 신고 기준액 질문 → 규정 문서 인용 |
| 주문·티켓 실시간 조회/생성 | 매수/매도 즉시 체결, 전체 평가액 기준 신고 티켓 자동 생성 | 포트폴리오 조회, 매수 후 티켓 생성 확인 |
| 3턴 이상 맥락 유지 | JDBC 기반 ChatMemory, `userId:sessionId` 대화 ID | "그거 얼마에 샀지?" 같은 대명사 질문 |
| P95 5초 / 토큰 사용량 관찰 | TokenMeterAdvisor(Micrometer) | `/actuator/metrics/ai.tokens`, `ai.latency`의 0.95 분위수 |
| 인젝션·민감어 차단 + 전체 감사 | SafeGuardAdvisor + AuditAdvisor | 레드팀 프롬프트, 감사 로그 |
| AI 모델 장애 시 안전한 안내 응답 지속 | 정적 장애 안내 + `helpdesk.simulate-primary-failure` 토글 | 토글 켜고 안내 응답 확인 |

## 4. 아키텍처 요약

```
사용자 → Thymeleaf 사용자 선택/추가 → 웹 UI(SSE) → ChatController(API userId와 서버 세션 대조) → HelpDeskChatService
       → ChatClient(helpDeskClient) → Advisor 체인(Audit→SafeGuard→Memory→RAG→TokenMeter)
       → Tool(StockQuoteTools·ExchangeRateTools·PortfolioTools) → PriceService(Finnhub·Frankfurter)
       → PortfolioRepository / ComplianceTicketRepository(인메모리)
       → 신고 기준 초과 시 ComplianceMailService(Spring Mail)
```

Advisor order 규칙(day3 원칙 그대로): `AuditAdvisor(0)` → `SafeGuardAdvisor(100)` → `MessageChatMemoryAdvisor(200)` → `RetrievalAugmentationAdvisor(300)` → `TokenMeterAdvisor(900)`. 차단(100)이 저장(200)보다 반드시 앞에 있어야 한다.

## 5. 패키지 구조

```
com.skala.helpdesk/
├─ HelpDeskApplication.java
├─ config/         AiConfig(ChatClient·Advisor 조립), HelpDeskProperties
├─ domain/         Portfolio, Holding, ComplianceTicket, TicketStatus  (불변 record)
├─ repository/     PortfolioRepository, ComplianceTicketRepository    (인메모리 ConcurrentHashMap)
├─ market/         PriceService(Finnhub·Frankfurter 캡슐화), StockQuote, ExchangeRate
├─ tool/           StockQuoteTools, ExchangeRateTools, PortfolioTools (@Tool)
├─ rag/            IngestService, DocsAutoIngestRunner(부팅 시 자동 인제스트)
├─ service/        HelpDeskChatService, ComplianceAdminService, ComplianceMailService, ToolUsageTracker
├─ advisor/        AuditAdvisor, TokenMeterAdvisor                    (day3 포팅)
└─ web/            controller(Chat·Admin·UserPage), dto, exception, templates(login·chat)
```

## 6. 신규 도입 기술 (day1~3에는 없던 것)

- **pgvector**(day3는 인메모리 SimpleVectorStore) — `docker-compose.yml`로 로컬 실행
- **JDBC 기반 ChatMemory** — 재시작해도 대화 이력 유지(`spring.ai.chat.memory.repository.jdbc.initialize-schema=always`)
- **spring-dotenv**(`me.paulschwarz:spring-dotenv`) — `.env` 파일을 Spring Environment로 읽어들임. Gradle 플러그인이 아니라 일반 `implementation` 의존성으로 추가해야 함(Gradle Plugin Portal엔 없음).

## 7. 실행 전 준비 — .env 발급 가이드

`cp .env.example .env` 로 복사한 뒤 아래 값을 채운다. `DB_URL` 등은 이미 `docker-compose.yml` 기본값(5434 포트)과 맞춰뒀으니 안 건드려도 된다.

### 1) FINNHUB_API_KEY (주가 조회, 필수)
[finnhub.io/register](https://finnhub.io/register) 가입 → 로그인 후 대시보드 첫 화면에 API Key가 바로 보인다. 카드 등록 없이 즉시 발급.

### 2) MAIL_USERNAME / MAIL_PASSWORD / COMPLIANCE_MAIL_TO (신고 메일, 선택)
구글 계정 → 보안 → 2단계 인증 켜기(안 켜져 있으면 앱 비밀번호 메뉴 자체가 안 보임) → "앱 비밀번호" 검색 → 이름 아무거나 입력해 생성 → 16자리 값이 `MAIL_PASSWORD`(로그인 비밀번호 아님). `MAIL_USERNAME`은 본인 Gmail 주소, `COMPLIANCE_MAIL_TO`는 받을 주소(본인 메일로 넣어도 됨, 데모용).

### OPENAI_API_KEY
OpenAI Platform에서 프로젝트 API 키를 발급해 `.env`의 `OPENAI_API_KEY=` 뒤에 입력한다. `.env`는 `.gitignore` 대상이므로 제출 ZIP에는 포함하지 않는다.

## 8. 실행 방법

```bash
cd "종합 실습/SpringAI_종합실습"
cp .env.example .env      # 위 가이드대로 값 채우기
docker compose up -d      # pgvector (호스트 5434 포트 — 로컬 기존 Postgres/다른 프로젝트와 충돌 방지)
./gradlew bootRun
```

뜨면 확인할 곳:
- `http://localhost:8080/swagger-ui.html` — 전체 API 목록
- `http://localhost:8080/` — 간단 채팅 데모 페이지(SSE)
- `http://localhost:8080/api/admin/chunks?q=...` — 인제스트 품질 확인 (`X-Admin-Key: changeme` 헤더 필요)

## 9. 실제로 검증한 것 (bootRun + curl로 라이브 테스트 완료)

- 부팅 시 규정 문서 3개 자동 인제스트 (`해외주식-거래-규정`, `해외주식-보유-신고-규정`, `환율-적용-기준`)
- RAG 답변 + 출처: "신고 기준액이 얼마야?" → 정확한 규정 인용 + `sources`에 문서명 포함
- Tool 호출 실패 시 우아한 처리: Finnhub 키 없이 포트폴리오 조회 → 자연스러운 안내 문구로 응답(`toolUsed: true`)
- 3턴 멀티턴 기억: "방금 내가 뭐라고 물어봤지?" 정상 응답 (JDBC 대화기억)
- 동기 API, SSE 스트리밍(토큰 이벤트 + 마지막 출처 이벤트), Swagger, 관리자 인증(403) 전부 확인
- Portfolio 매수/매도/평단가/잔고검증 유닛테스트 4개 통과

과정에서 실제로 잡은 버그 2개:
1. SSE 스트림이 일부 청크에서 null 텍스트를 반환해 죽던 문제 — Reactor `map` → `handle`로 수정
2. 로컬에 이미 떠 있던 다른 Postgres(5432)·다른 프로젝트 컨테이너(5433)와 포트가 겹쳐 pgvector가 엉뚱한 DB로 연결되던 문제 — 5434 포트로 변경

## 10. 아직 라이브로 못 본 것

Gmail 알림은 구현됐지만 실제 Gmail 앱 비밀번호를 넣기 전까지는 발송을 확인할 수 없다. 위 7번 가이드대로 값을 채운 뒤 신고 티켓 생성으로 테스트한다.

## 11. 가산점 후보 (논의만 하고 아직 미착수)

- **동일 질문 캐시**(Caffeine, `spring-boot-starter-cache` + `@Cacheable`) — P95 응답시간·토큰 상한 비기능 요구를 before/after 지표로 정량 증명 가능
- **평가 자동화 게이트**(day3 `Lab2GoldenSetTest` 확장) — golden set 질문·기대 근거로 정답률/근거 일치율을 리포트

## 12. 제출 준비 메모

- 제출 형식: 코드 + 실행/테스트 결과 캡처·설명을 담은 PDF를 zip으로 묶어 `SpringAI_Final_고유번호....zip`
- 캡처해야 할 화면: 사용자 선택 화면(user1/user2) / Swagger 전체 API 목록 / 인제스트 결과(`/api/admin/chunks`) / 규정 질문 응답(출처 포함) / user1·user2 계좌·대화 분리 / 신고 티켓 생성→승인·Gmail 알림 / **3턴 멀티턴 대화(가장 중요)** / SSE 스트리밍 / 레드팀 테스트 결과 / `/actuator/metrics` / 장애 주입 안내 응답
