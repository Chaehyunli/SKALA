package com.skala.helpdesk.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.helpdesk.service.HelpDeskChatService;
import com.skala.helpdesk.service.UserSessionService;
import com.skala.helpdesk.web.dto.chat.request.AskRequest;
import com.skala.helpdesk.web.dto.chat.response.AnswerDto;
import com.skala.helpdesk.web.dto.chat.response.ChatMessageView;
import org.springframework.ai.chat.client.ChatClientResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpSession;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** HelpDesk 채팅 API — 동기 응답과 SSE 스트리밍 두 가지를 제공한다. */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "HelpDesk 채팅")
public class ChatController {

    private final HelpDeskChatService chatService;
    private final UserSessionService userSessionService;
    private final ObjectMapper objectMapper;

    public ChatController(HelpDeskChatService chatService, UserSessionService userSessionService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.userSessionService = userSessionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @Operation(summary = "동기 채팅", description = "규정 질문·시세 조회·매수/매도를 한 번에 처리하고 구조화된 응답을 반환한다.")
    public AnswerDto ask(@Valid @RequestBody AskRequest request, HttpSession session) {
        String userId = userId(session, request.userId());
        String sessionId = userSessionService.requireOwnSessionId(session, request.sessionId());
        return chatService.ask(userId, request.message(), sessionId);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE 스트리밍 채팅", description = "토큰을 event:token 으로 흘리고, 마지막에 event:sources 로 출처를 보낸다.")
    public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody AskRequest request, HttpSession session) {
        AtomicReference<List<com.skala.helpdesk.web.dto.chat.response.Source>> lastSources =
                new AtomicReference<>(List.of());

        // 청크에 따라 getResult()/getOutput()/getText()가 null일 수 있어 map 대신 handle로 null은 그냥 건너뛴다
        // (Reactor의 map은 null 반환을 허용하지 않아 그 상태로 두면 NullPointerException으로 스트림이 죽는다).
        String userId = userId(session, request.userId());
        String sessionId = userSessionService.requireOwnSessionId(session, request.sessionId());
        Flux<ServerSentEvent<String>> tokens = chatService.stream(userId, request.message(), sessionId)
                .doOnNext(chunk -> {
                    var sources = chatService.sourcesOf(chunk);
                    if (!sources.isEmpty()) {
                        lastSources.set(sources);
                    }
                })
                .<ServerSentEvent<String>>handle((chunk, sink) -> {
                    String text = textOf(chunk);
                    if (text != null && !text.isEmpty()) {
                        sink.next(ServerSentEvent.builder(text).event("token").build());
                    }
                })
                .onErrorResume(e -> Mono.just(ServerSentEvent.<String>builder(
                        chatService.unavailableAnswer().answer()).event("token").build()));

        Mono<ServerSentEvent<String>> sourcesEvent = Mono.fromCallable(
                        () -> ServerSentEvent.builder(toJson(lastSources.get())).event("sources").build());

        return tokens.concatWith(sourcesEvent).timeout(Duration.ofSeconds(60));
    }

    @GetMapping("/history")
    @Operation(summary = "대화 이력 조회", description = "현재 브라우저 세션(+sessionId) 하나의 전체 대화를 시간순으로 반환한다.")
    public List<ChatMessageView> history(@RequestParam String userId,
                                          HttpSession session,
                                          @RequestParam(required = false) String sessionId) {
        String verifiedUserId = userId(session, userId);
        String verifiedSessionId = userSessionService.requireOwnSessionId(session, sessionId);
        return chatService.history(verifiedUserId, verifiedSessionId);
    }

    private String textOf(ChatClientResponse chunk) {
        if (chunk.chatResponse() == null || chunk.chatResponse().getResult() == null) {
            return null;
        }
        var output = chunk.chatResponse().getResult().getOutput();
        return output != null ? output.getText() : null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /** API의 userId가 로그인 세션과 일치할 때만 모델·도구에 전달한다. */
    private String userId(HttpSession session, String requestedUserId) {
        return userSessionService.requireSameUser(session, requestedUserId);
    }
}
