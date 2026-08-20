package com.skala.day2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.day2.service.Lab2AskService;
import com.skala.day2.service.Lab2IngestService;
import com.skala.day2.web.dto.lab2.response.AnswerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAG 파이프라인의 4단계 — 골든 세트로 측정.
 *
 * <p>모델을 호출하므로 기본 테스트에서는 제외한다. {@code ./gradlew test -Peval} 로만 실행된다.
 * Step5 실험표의 각 조합(A~F)을 테스트하려면 Lab2IngestService/Lab2AskService 쪽 설정값을
 * 하나 바꾸고 이 테스트를 다시 돌려 통과율을 비교한다.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "eval", matches = "true")
class Lab2GoldenSetTest {

    private static final Logger log = LoggerFactory.getLogger(Lab2GoldenSetTest.class);

    // 실험표 A(기준) 조합 — Step5 에서 이 값들을 바꿔가며 재실행한다. 한 번에 하나만 바꾼다.
    private static final int CHUNK_SIZE = 400;
    private static final int MIN_CHUNK_SIZE_CHARS = 200;
    private static final boolean OVERLAP = true;   // 실험 F — true 로 바꾸면 청크끼리 20% 겹쳐서 이어붙인다
    private static final int TOP_K = 4;
    private static final double THRESHOLD = 0.2;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private Lab2IngestService ingestService;
    @Autowired
    private Lab2AskService askService;

    @BeforeEach
    void ingest() {
        ingestService.ingestAll(CHUNK_SIZE, MIN_CHUNK_SIZE_CHARS, OVERLAP);   // 벡터스토어가 인메모리라 테스트마다 새로 채운다
    }

    @Test
    void 골든_세트_평가() throws Exception {
        List<Golden> golden = mapper.readValue(
                new ClassPathResource("lab2-docs/golden.json").getInputStream(),
                new TypeReference<>() {});

        int pass = 0;
        for (Golden g : golden) {
            AnswerDto a = askService.ask(g.q(), TOP_K, THRESHOLD);
            boolean hit = g.must().stream().allMatch(k -> a.answer().contains(k));
            boolean cite = g.src() == null
                    || a.sources().stream().anyMatch(s -> s.contains(g.src()));
            if (hit && cite) {
                pass++;
            } else {
                // 실패를 두 종류로 나눠 읽는다 — hit=false 면 근거를 못 찾은 것, cite=false 면 찾고도 출처 표기가 안 된 것.
                log.warn("실패(hit={}, cite={}): {}\n  답변: {}\n  출처: {}", hit, cite, g.q(), a.answer(), a.sources());
            }
        }
        log.info("통과 {}/{}", pass, golden.size());
        assertThat(pass).isGreaterThanOrEqualTo(8);   // 기준선을 코드에 박아 둔다
    }

    record Golden(String q, List<String> must, String src) {
    }
}
