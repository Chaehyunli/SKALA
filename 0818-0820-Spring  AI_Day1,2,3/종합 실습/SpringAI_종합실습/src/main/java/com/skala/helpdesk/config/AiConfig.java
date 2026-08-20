package com.skala.helpdesk.config;

import com.skala.helpdesk.advisor.AuditAdvisor;
import com.skala.helpdesk.advisor.TokenMeterAdvisor;
import com.skala.helpdesk.tool.ExchangeRateTools;
import com.skala.helpdesk.tool.PortfolioTools;
import com.skala.helpdesk.tool.StockQuoteTools;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * ChatClient·ChatMemory 조립. 어드바이저 순서가 곧 정책이다 — SafeGuardAdvisor(차단)는
 * MessageChatMemoryAdvisor(저장)보다 반드시 앞에 있어야 한다(day3 Lab3AssistantChatClient 원칙 그대로).
 */
@Configuration
public class AiConfig {

    @Bean
    ChatMemory chatMemory(ChatMemoryRepository repository, HelpDeskProperties props) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(props.memory().maxMessages())
                .build();
    }

    /** 주 ChatClient — RAG(규정)·Tool(시세·환율·매매)·Advisor(기억·안전·관찰)를 조립한다. */
    @Bean
    ChatClient helpDeskClient(ChatClient.Builder builder,
                              VectorStore vectorStore,
                              ChatMemory chatMemory,
                              HelpDeskProperties props,
                              StockQuoteTools stockQuoteTools,
                              ExchangeRateTools exchangeRateTools,
                              PortfolioTools portfolioTools,
                              MeterRegistry meterRegistry,
                              @Value("classpath:prompts/system.st") Resource systemPrompt) throws IOException {
        return builder
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultAdvisors(
                        new AuditAdvisor(),                                            // order 0   — 가장 바깥, 턴 로그
                        SafeGuardAdvisor.builder()
                                .sensitiveWords(List.of("시스템 프롬프트", "프롬프트를 공개", "이전 지시 무시", "지시를 무시",
                                        "주민등록번호", "카드번호", "관리자 키"))
                                .order(100)
                                .build(),                                               // order 100 — 차단은 저장보다 앞
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .order(200)
                                .build(),                                               // order 200 — 대화 기억
                        RetrievalAugmentationAdvisor.builder()
                                .documentRetriever(VectorStoreDocumentRetriever.builder()
                                        .vectorStore(vectorStore)
                                        .similarityThreshold(props.rag().threshold())
                                        .topK(props.rag().topK())
                                        .build())
                                // 근거가 없어도 질문 자체를 덮어쓰지 않는다 — 그래야 매매·조회 같은
                                // 도구 호출 질문(규정과 무관한 질문)까지 막히지 않는다.
                                .queryAugmenter(ContextualQueryAugmenter.builder()
                                        .allowEmptyContext(true)
                                        .build())
                                .order(300)
                                .build(),                                               // order 300 — 규정 근거 검색
                        new TokenMeterAdvisor(meterRegistry))                           // order 900 — 가장 안쪽, 실측
                .defaultTools(stockQuoteTools, exchangeRateTools, portfolioTools)
                .build();
    }

}
