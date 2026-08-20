package com.skala.lab12;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

/**
 * 12장 미니 실습 — 요청을 바꿔서 넘기는 가장 단순한 Advisor.
 *
 * <p>답 끝에 이모지가 붙으면 Advisor 가 걸린 것이다.
 * {@code getOrder()} 숫자가 곧 순서이고, 순서가 곧 정책이다.
 */
@Component
public class 이모지Advisor implements BaseAdvisor {

    @Override
    public ChatClientRequest before(ChatClientRequest req, AdvisorChain chain) {
        Prompt 바뀐프롬프트 = req.prompt().augmentSystemMessage(
                s -> new SystemMessage(s.getText() + "\n답변 마지막에 어울리는 이모지 하나를 붙인다."));
        return req.mutate().prompt(바뀐프롬프트).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse res, AdvisorChain chain) {
        return res;                                        // 응답은 그대로 통과
    }

    @Override
    public String getName() {
        return "emoji";
    }

    @Override
    public int getOrder() {
        return 250;                                        // 숫자가 곧 순서
    }
}
