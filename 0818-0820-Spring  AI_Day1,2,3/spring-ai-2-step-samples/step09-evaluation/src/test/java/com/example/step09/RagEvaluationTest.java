package com.example.step09;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * STEP 09 - AI 응답 품질 평가(Evaluation)
 *
 * <p><b>왜 필요한가?</b><br>
 * LLM은 같은 입력에도 매번 다른 문장을 내놓는다(비결정적).
 * 그래서 {@code assertEquals("정답 문자열", answer)} 같은 전통적 단위 테스트가 통하지 않는다.
 * 대신 "답변이 <b>충분히 적절한가</b>"를 판정해야 하는데,
 * 그 판정을 다시 LLM에게 맡기는 기법을 <b>LLM-as-a-Judge</b>라고 한다.
 *
 * <p><b>RelevancyEvaluator</b>가 판정하는 것은 다음과 같다.
 * "검색된 Context에 비추어 이 답변이 질문에 적절한가?" → PASS / FAIL
 * (질문·Context·답변 세 가지를 심판 모델에게 주고 물어본다)
 *
 * <p><b>비용/시간 주의</b> : 이 테스트 1회에 LLM 호출이 여러 번 발생한다
 * (임베딩 + RAG 답변 생성 + 평가). 매 커밋마다 돌리기보다는
 * 프롬프트나 검색 설정을 바꿨을 때 회귀를 확인하는 용도로 쓰는 것이 현실적이다.
 *
 * <p>{@code @SpringBootTest} : 실제 Spring 컨텍스트를 전부 띄운다.
 * 따라서 OPENAI_API_KEY 환경변수와 pgvector 컨테이너가 모두 준비되어 있어야 한다.
 */
@SpringBootTest
class RagEvaluationTest {

    /**
     * 저수준 모델 인터페이스. ChatClient가 아니라 ChatModel을 주입받는 이유는
     * 이 테스트에서 용도가 다른 ChatClient를 <b>두 개</b>(답변용/평가용) 만들기 때문이다.
     */
    @Autowired
    ChatModel chatModel;

    /** pgvector 기반 벡터 저장소. 아래 seed()에서 평가용 지식을 넣는다. */
    @Autowired
    VectorStore vectorStore;

    /**
     * 각 테스트 실행 전에 평가 기준이 될 문서를 적재한다.
     *
     * <p>여기 있는 내용만이 '정답의 근거'다. 이 Context로 답할 수 없는 질문을 던지면
     * 평가가 FAIL 나는 것이 정상이다.
     *
     * <p>실무에서는 (질문, 기대 근거) 쌍을 모아 둔 <b>골든 데이터셋</b>을 만들고
     * 여러 케이스를 반복 평가해 통과율을 지표로 관리한다.
     */
    @BeforeEach
    void seed() {
        vectorStore.add(List.of(
                new Document("Spring AI의 Advisor는 AI 요청과 응답 처리 파이프라인에 개입한다."),
                new Document("QuestionAnswerAdvisor는 VectorStore에서 관련 문서를 검색하여 Prompt Context에 추가한다.")
        ));
    }

    /**
     * RAG 답변이 검색된 Context에 비추어 적절한지 검증한다.
     */
    @Test
    void ragAnswerShouldBeRelevantToContext() {

        String question = "QuestionAnswerAdvisor는 어떤 역할을 하나요?";

        // ── 1) RAG 파이프라인 구성 ────────────────────────────────
        // RetrievalAugmentationAdvisor는 STEP 06의 QuestionAnswerAdvisor보다
        // 한 단계 유연한 '모듈형 RAG' Advisor다.
        // 질의 변환(query transformation), 검색(retrieval), 재순위(rerank), 결합(augmentation)
        // 각 단계를 부품처럼 교체할 수 있다.
        // 평가에 필요한 '검색된 문서 목록'을 응답 메타데이터로 남겨준다는 점도 중요하다.
        RetrievalAugmentationAdvisor ragAdvisor =
                RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(
                                // 어디서 문서를 가져올지 지정. 여기서는 pgvector.
                                // topK, similarityThreshold, 메타데이터 필터도 여기서 설정한다.
                                VectorStoreDocumentRetriever.builder()
                                        .vectorStore(vectorStore)
                                        .build()
                        )
                        .build();

        // ── 2) 평가 대상이 되는 실제 답변 생성 ─────────────────────
        ChatResponse response =
                // ChatClient.builder(chatModel) : 자동 구성 Builder 대신
                // 모델로부터 직접 만든다. 테스트에서 설정을 명시적으로 통제하기 위함이다.
                ChatClient.builder(chatModel)
                        .build()
                        // prompt(String) : user() 없이 바로 사용자 메시지를 넘기는 축약형
                        .prompt(question)
                        // 이번 호출에만 RAG Advisor를 적용한다
                        .advisors(ragAdvisor)
                        .call()
                        // content()(문자열)가 아니라 chatResponse()를 받는 이유:
                        // 답변 본문뿐 아니라 '검색에 사용된 문서(메타데이터)'까지 필요하기 때문이다.
                        .chatResponse();

        // ── 3) 평가 요청 조립 ─────────────────────────────────────
        // 심판 모델에게 넘길 3종 세트: 질문 / 근거 Context / 실제 답변
        EvaluationRequest evaluationRequest =
                new EvaluationRequest(
                        // (1) 사용자 질문
                        question,
                        // (2) 근거 Context — RAG Advisor가 검색해 메타데이터에 넣어 둔 문서 목록.
                        //     DOCUMENT_CONTEXT는 그 값을 꺼내기 위한 약속된 키 상수다.
                        response.getMetadata().get(
                                RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT
                        ),
                        // (3) 실제 생성된 답변 텍스트
                        response.getResult()
                                .getOutput()
                                .getText()
                );

        // ── 4) 심판(Judge) 모델 준비 ──────────────────────────────
        // 평가자도 결국 LLM이다. 여기서는 답변 생성과 같은 모델을 쓰지만,
        // 자기 답변에 후한 점수를 주는 편향을 줄이려면
        // 실무에서는 더 상위 모델이나 다른 공급자의 모델을 심판으로 쓰기도 한다.
        RelevancyEvaluator evaluator =
                new RelevancyEvaluator(
                        ChatClient.builder(chatModel)
                );

        // ── 5) 평가 실행 ──────────────────────────────────────────
        // 내부적으로 "이 답변이 Context에 근거해 질문에 적절한가? YES/NO"를 묻는
        // 평가 전용 프롬프트가 심판 모델에게 전송된다.
        EvaluationResponse evaluation =
                evaluator.evaluate(evaluationRequest);

        // ── 6) 검증 ───────────────────────────────────────────────
        // isPass()가 false라면 원인은 보통 셋 중 하나다.
        //   ① 검색이 엉뚱한 문서를 가져옴 (retrieval 문제)
        //   ② 문서는 맞는데 답변이 빗나감 (generation 문제)
        //   ③ 애초에 Context에 답이 없음 (데이터 문제)
        assertThat(evaluation.isPass()).isTrue();
    }
}
