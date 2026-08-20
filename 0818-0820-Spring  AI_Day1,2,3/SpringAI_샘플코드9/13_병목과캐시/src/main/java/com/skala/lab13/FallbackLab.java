package com.skala.lab13;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * 13장 미니 실습 — 주 모델이 죽어도 서비스는 답해야 한다.
 *
 * <p>재시도는 일시적 오류에만 건다. 잘못된 요청을 두 번 보내도 결과는 같다.
 */
@Service
public class FallbackLab {

    private static final Logger log = LoggerFactory.getLogger(FallbackLab.class);

    private final ChatClient 주모델;
    private final ChatClient 예비모델;

    public FallbackLab(ChatModel model) {
        this.주모델 = ChatClient.builder(model).build();
        // 실무에서는 다른 공급자·다른 모델을 예비로 둔다. 실습에서는 같은 모델로 경로만 확인한다.
        this.예비모델 = ChatClient.builder(model).build();
    }

    @Retryable(retryFor = TransientAiException.class, maxAttempts = 2)
    public String 물어보기(String q) {
        return 주모델.prompt().user(q).call().content();
    }

    @Recover
    public String 폴백(Exception e, String q) {
        log.warn("주 모델 실패 — 예비 모델로 넘긴다: {}", e.getMessage());
        return 예비모델.prompt().user(q).call().content();
    }
}
