package com.skala.day3.web.controller;

import com.skala.day3.service.AdminAuthException;
import com.skala.day3.service.Lab3AdminService;
import com.skala.day3.service.Lab3ChatService;
import com.skala.day3.web.dto.lab3.TicketView;
import com.skala.day3.web.dto.lab3.request.ChatRequest;
import com.skala.day3.web.dto.lab3.response.ChatMessageView;
import com.skala.day3.web.dto.lab3.response.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Day3 실습 · 상담 에이전트 — RAG(규정) + Tool(주문 조회·환불 접수) + Advisor(기억·안전·관찰) 조합.
 *
 * <p>확인: {@code curl -X POST localhost:8080/lab3/chat -H 'Content-Type: application/json'
 * -d '{"userId":"user1","message":"단순 변심 반품은 며칠 이내인가요?"}'}
 * sessionId 를 생략하면 이 userId 의 기본 대화로 자동으로 이어진다 — 매번 넣지 않아도 기억한다.
 * 별도의 대화를 동시에 열고 싶을 때만 원하는 값을 sessionId 로 직접 지어서 보낸다.
 *
 * <p>Lab2 문서로 규정을 검색하므로, 이 API 를 쓰기 전에 {@code POST /lab2/ingest} 를 먼저 호출해야 한다.
 */
@RestController
@RequestMapping("/lab3")
@Tag(name = "Day3 실습 · 상담 에이전트")
public class Lab3Controller {

    private final Lab3ChatService chatService;
    private final Lab3AdminService adminService;
    private final String adminKey;

    public Lab3Controller(Lab3ChatService chatService,
                           Lab3AdminService adminService,
                           @Value("${app.admin-key}") String adminKey) {
        this.chatService = chatService;
        this.adminService = adminService;
        this.adminKey = adminKey;
    }

    @PostMapping("/chat")
    @Operation(summary = "상담 에이전트와 대화",
            description = """
                    규정 질문(예: 반품 기한)은 RAG 근거 안에서만 답하고, 주문 조회·환불 접수는 도구로 처리한다.
                    본인(userId) 소유가 아닌 주문은 조회·환불 둘 다 거부된다.
                    sessionId 를 생략하면 이 userId 의 기본 대화로 자동 연결된다(매번 넣지 않아도 기억함).
                    별도의 대화를 동시에 유지하고 싶을 때만 원하는 값을 sessionId 로 직접 지어서 보낸다 —
                    그 값으로 이전 맥락 없는 새 대화가 시작된다.
                    RAG 근거는 Lab2 가 인제스트한 문서를 그대로 쓰므로, 먼저 POST /lab2/ingest 를 호출해 둬야 한다.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(
                            name = "주문 조회",
                            value = """
                                    {"userId":"user1","message":"제 주문 12345는 지금 어디예요?"}
                                    """))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "응답 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {"sessionId":"7e85850f-2c47-48ea-a3cf-c5fb3bc76b61",
                             "answer":"주문번호 12345의 무선 이어폰은 현재 배송 중이며, 예상 도착일은 7월 30일입니다."}
                            """)))})
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @GetMapping("/chat/history")
    @Operation(summary = "대화 이력 조회", description = "sessionId 하나의 전체 대화를 시간순으로 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            [{"role":"user","content":"제 주문 12345는 지금 어디예요?"},
                             {"role":"assistant","content":"주문번호 12345의 무선 이어폰은 현재 배송 중이며, 예상 도착일은 7월 30일입니다."}]
                            """)))})
    public List<ChatMessageView> history(
            @Parameter(description = "대화 세션 ID — /lab3/chat 응답의 sessionId. 별도로 지정한 적 없다면 "
                    + "\"{userId}:default\" 형태다.", example = "user1:default")
            @RequestParam String sessionId) {
        return chatService.history(sessionId);
    }

    @GetMapping("/admin/tickets/pending")
    @Operation(summary = "환불 대기 티켓 목록",
            description = "PENDING 상태인 환불 티켓을 전부 반환한다. 모델은 이 API 를 호출할 경로가 없다 — 담당자가 직접 확인한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            [{"no":"df4d989d","orderId":"12345","status":"PENDING","reason":"단순 변심"}]
                            """))),
            @ApiResponse(responseCode = "403", description = "관리자 키가 없거나 틀림")})
    public List<TicketView> pending(
            @Parameter(description = "관리자 키(application.yml 의 app.admin-key)", example = "changeme")
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return adminService.pending();
    }

    @PostMapping("/admin/tickets/{no}/approve")
    @Operation(summary = "환불 승인",
            description = "PENDING 티켓을 APPROVED 로 바꾼다. 모델은 이 API 를 호출할 경로가 없다 — "
                    + "실제 처리 버튼은 사람이 직접 눌러야 한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "승인 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {"no":"df4d989d","orderId":"12345","status":"APPROVED","reason":"단순 변심"}
                            """))),
            @ApiResponse(responseCode = "403", description = "관리자 키가 없거나 틀림"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 티켓 번호")})
    public TicketView approve(
            @Parameter(description = "환불 티켓 번호", example = "df4d989d") @PathVariable String no,
            @Parameter(description = "관리자 키(application.yml 의 app.admin-key)", example = "changeme")
            @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return adminService.approve(no);
    }

    private void requireAdmin(@Schema(hidden = true) String key) {
        if (!adminKey.equals(key)) {           // key == null 이어도(헤더 자체가 없어도) 그냥 불일치로 처리한다
            throw new AdminAuthException();
        }
    }
}
