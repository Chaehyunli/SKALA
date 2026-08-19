package com.skala.day2.service;

import com.skala.day2.web.AnswerDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 파이프라인의 3단계 — 근거로 답하기.
 *
 * <p>검색(Step2)에서 근거를 못 찾으면 모델을 아예 부르지 않는다 — 비용도 아끼고,
 * 근거 없이 모델이 지어내는 것도 막는다. 근거가 있을 때만 [근거]를 프롬프트에 넣어 호출한다.
 */
@Service
public class Lab2AskService {

    private static final int TOP_K = 4;
    private static final double THRESHOLD = 0.5;

    private final Lab2RetrieveService retrieveService;
    private final ChatClient askChatClient;

    public Lab2AskService(Lab2RetrieveService retrieveService,
                           @Qualifier("askChatClient") ChatClient askChatClient) {
        this.retrieveService = retrieveService;
        this.askChatClient = askChatClient;
    }

    public AnswerDto ask(String question) {
        List<Document> docs = retrieveService.retrieve(question, TOP_K, THRESHOLD);
        if (docs.isEmpty()) {
            return AnswerDto.unknown();          // 근거가 없으면 모델을 부르지 않는다
        }
        return askChatClient.prompt()
                .user(u -> u.text("[근거]\n{context}\n\n[질문] {question}")
                        .param("context", format(docs))
                        .param("question", question))
                .call()
                .entity(AnswerDto.class);         // 구조화 출력 — 문자열 파싱 금지
    }

    /** 근거 청크를 "[출처] 본문" 형태로 나열해 프롬프트에 넣을 컨텍스트를 만든다. */
    private String format(List<Document> docs) {
        return docs.stream()
                .map(d -> "[%s] %s".formatted(d.getMetadata().get("source"), d.getText()))
                .collect(Collectors.joining("\n\n"));
    }
}
