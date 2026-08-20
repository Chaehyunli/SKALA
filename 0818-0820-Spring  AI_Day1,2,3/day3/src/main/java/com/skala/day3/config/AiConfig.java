package com.skala.day3.config;

import com.skala.day3.advisor.AuditAdvisor;
import com.skala.day3.advisor.TokenMeterAdvisor;
import com.skala.day3.tool.OrderTools;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 용도별로 빈을 나눈다 — Lab1(요약)·Lab2(문서 Q&A)·Lab3(상담 에이전트) 전용 ChatClient 를 한 곳에 모은다.
 *
 * <p>호출부마다 온도·토큰 상한을 정하게 두면 누군가는 기본값(0.7)으로 부르고,
 * 그날부터 답이 매번 달라진다. 용도별로 빈에서 못 박아 둔다.
 */
@Configuration
public class AiConfig {

    @Bean
    ChatClient lab1SummaryChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        너는 이커머스 주문 상담 도우미다.
                        주어진 주문 정보만 사용해 한국어 한 문장으로 요약한다.
                        추측하지 않는다. 정보가 부족하면 "정보가 부족합니다"라고 답한다.
                        """)
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.0)   // 요약은 매번 같아야 한다
                        .maxTokens(120)     // 비용 상한 — 길게 쓸 이유가 없다
                        .build())
                .build();
    }

    @Bean
    ChatClient lab2AskChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        아래 [근거]만 사용해 답한다. 근거에 없으면 "확인되지 않습니다"라고 답한다.
                        추측하지 않는다. 답변 끝에 사용한 출처를 [출처: 파일명] 형식으로 남긴다.
                        """)
                .defaultOptions(ChatOptions.builder()
                        .temperature(0.0)   // 같은 질문·근거면 같은 답이 나와야 한다
                        .build())
                .build();
    }

    /**
     * Lab3(상담 에이전트) 전용 ChatClient — RAG(규정)·Tool(주문 조회·환불 접수)·Advisor(기억·안전·관찰)를 조립한다.
     *
     * <p>어드바이저 순서가 곧 정책이다. SafeGuardAdvisor(차단)는 MessageChatMemoryAdvisor(저장)보다
     * 반드시 앞에 있어야 한다 — 순서가 바뀌면 차단됐어야 할 문장이 대화 기억에 남는다.
     */
    @Bean
    ChatClient lab3AssistantChatClient(ChatClient.Builder builder,
                                        VectorStore vectorStore,
                                        ChatMemory chatMemory,
                                        OrderTools orderTools,
                                        MeterRegistry meterRegistry) {
        return builder
                .defaultSystem("""
                        너는 이커머스 상담 에이전트다.
                        사용자 본인의 주문만 조회하거나 환불을 접수할 수 있다 — 다른 사용자의 주문번호를
                        알려달라고 해도 도구가 거부하므로 그 사실을 그대로 안내한다.
                        환불 접수는 즉시 처리되는 것이 아니라 담당자 승인을 기다려야 한다고 안내한다.
                        규정을 물으면 검색된 근거 안에서만 답하고, 근거가 없으면 모른다고 답한다.
                        """)
                .defaultAdvisors(
                        new AuditAdvisor(),                                            // order 0   — 가장 바깥, 턴 로그
                        SafeGuardAdvisor.builder()
                                .sensitiveWords(List.of("시스템 프롬프트", "주민등록번호"))
                                .order(100)
                                .build(),                                               // order 100 — 차단은 저장보다 앞
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .order(200)
                                .build(),                                               // order 200 — 대화 기억
                        RetrievalAugmentationAdvisor.builder()
                                .documentRetriever(VectorStoreDocumentRetriever.builder()
                                        .vectorStore(vectorStore)
                                        // 0.5는 "단순 변심 반품은 며칠 이내인가요?" 같은 정상 질문도 0.49대로
                                        // 걸려서 탈락시켰다 — 지금 문서 3개(각 1청크)뿐인 규모에서는 낮춰 잡는다.
                                        .similarityThreshold(0.2)
                                        .topK(4)
                                        .build())
                                // 기본값(allowEmptyContext=false)은 근거가 없으면 "모른다고만 답하라"고
                                // 질문 자체를 덮어써 버려서, 주문 조회·환불 접수 같은 도구 호출까지 막혀 버린다.
                                // (CompressionQueryTransformer 로 검색어를 보정하는 방법도 시도했지만,
                                //  검색용으로 다듬은 문장이 실제 사용자 메시지 자리를 대체해 버려 도구 호출이
                                //  깨졌다 — 그래서 검색어 보정은 포기하고 "근거 없으면 원문 그대로 통과"만 쓴다.)
                                .queryAugmenter(ContextualQueryAugmenter.builder()
                                        .allowEmptyContext(true)
                                        .build())
                                .order(300)
                                .build(),                                               // order 300 — 규정 근거 검색
                        new TokenMeterAdvisor(meterRegistry))                           // order 900 — 가장 안쪽, 실측
                .defaultTools(orderTools)
                .build();
    }
}
