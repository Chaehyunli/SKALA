package com.skala.day2.service;

import com.skala.day2.web.dto.IngestResult;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * RAG 파이프라인의 1단계 — 인제스트.
 *
 * <p>흐름: 읽기(TextReader) → 분할(TokenTextSplitter) → 메타데이터 부여 → 저장(VectorStore).
 * 출처(source)·버전(version) 메타데이터는 여기서 청크에 심어 둬야 하고, 검색·답변 단계에서는
 * 다시 넣을 방법이 없다 — 그래서 이 단계가 "메타데이터가 절반"이다.
 */
@Service
public class Lab2IngestService {

    // classpath 아래 lab2-docs 폴더의 .md 문서 전부를 대상으로 한다.
    private static final String DOCS_LOCATION = "classpath:lab2-docs/*.md";
    // 문서 버전 태그 — 지금은 고정값이지만, 문서가 갱신되면 v2로 올려 신·구 버전을 구분할 수 있다.
    private static final String VERSION = "v1";

    private final VectorStore vectorStore;

    public Lab2IngestService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    private static final int DEFAULT_CHUNK_SIZE = 400;
    private static final int DEFAULT_MIN_CHUNK_SIZE_CHARS = 200;
    // TokenTextSplitter 에는 겹침 옵션이 없어 분할 후 직접 이어붙인다 — 슬라이드의 "겹침 20%"에 맞춘 비율.
    private static final double OVERLAP_RATIO = 0.2;

    /** 기본 청크 크기(400토큰)·겹침 없음으로 lab2-docs 아래 .md 문서를 전부 (재)인제스트한다. */
    public List<IngestResult> ingestAll() {
        return ingestAll(DEFAULT_CHUNK_SIZE, DEFAULT_MIN_CHUNK_SIZE_CHARS, false);
    }

    /**
     * lab2-docs 아래 .md 문서를 전부 (재)인제스트한다.
     *
     * <p>재인제스트 시 문서별로 delete-then-add 를 하므로 여러 번 호출해도
     * 벡터스토어에 같은 문서의 청크가 중복으로 쌓이지 않는다.
     *
     * @param chunkSize          청크당 목표 토큰 수 (Step5 실험 B/C 에서 조정)
     * @param minChunkSizeChars  너무 짧은 청크(문장 조각)를 만들지 않는 최소 글자 수
     * @param overlap            true 면 각 청크 앞에 이전 청크 끝부분을 겹쳐 붙인다 (Step5 실험 F)
     */
    public List<IngestResult> ingestAll(int chunkSize, int minChunkSizeChars, boolean overlap) {
        try {
            // classpath:lab2-docs/*.md 패턴에 매칭되는 리소스를 전부 찾는다.
            Resource[] docs = new PathMatchingResourcePatternResolver().getResources(DOCS_LOCATION);
            return List.of(docs).stream()
                    // 파일명(확장자 제외)을 source 로 사용해 문서 하나씩 인제스트한다.
                    .map(doc -> ingest(doc, sourceOf(doc), VERSION, chunkSize, minChunkSizeChars, overlap))
                    .toList();
        } catch (IOException e) {
            // 부팅 시점이 아닌 API 호출 시점의 오류이므로 unchecked 로 감싸 그대로 올려보낸다.
            throw new UncheckedIOException(e);
        }
    }

    /** 문서 하나를 읽어서 분할하고, 같은 source 의 기존 청크를 지운 뒤 새로 저장한다. */
    private IngestResult ingest(Resource doc, String source, String version,
                                 int chunkSize, int minChunkSizeChars, boolean overlap) {
        var reader = new TextReader(doc);                          // .md 파일 → Document 객체로 변환
        // version 은 커스텀 메타데이터로 넣으면 그대로 유지된다.
        reader.getCustomMetadata().put("version", version);

        // 토큰 개수 기준으로 문서를 청크 단위로 쪼갠다.
        var splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)                            // 청크당 목표 토큰 수 — 문서 성격에 맞춘다
                .withMinChunkSizeChars(minChunkSizeChars)            // 너무 짧은 청크(문장 조각)는 만들지 않는다
                .build();
        List<Document> chunks = splitter.apply(reader.get());       // 실제 분할 실행 → 청크(Document) 목록
        if (overlap) {
            chunks = withOverlap(chunks);
        }

        // 주의: TextReader.get() 은 customMetadata 에 우리가 넣어 둔 "source" 값이 있어도
        // 항상 resource 파일명(예: return-policy.md)으로 덮어써 버린다.
        // 확장자가 붙은 값이 필터 키로 쓰이면 아래 delete 필터(source=return-policy)와
        // 어긋나 재색인이 항상 실패하므로, 분할이 끝난 청크에 우리가 원하는 값으로 다시 심는다.
        chunks.forEach(chunk -> chunk.getMetadata().put("source", source));

        // 재인제스트 대비: 같은 source 의 기존 청크를 먼저 지운다.
        // 이걸 안 하면 매번 호출할 때마다 같은 문서의 청크가 중복으로 쌓여
        // "인제스트할수록 답이 나빠지는" 현상이 생긴다.
        vectorStore.delete(new FilterExpressionBuilder()             // 재색인 —
                .eq("source", source).build());                      //   같은 출처를 지우고
        vectorStore.add(chunks);                                     //   새 청크를 다시 넣는다(임베딩은 add 내부에서 계산)
        return new IngestResult(source, chunks.size());
    }

    /** 각 청크(2번째부터) 앞에 바로 앞 청크의 끝부분(OVERLAP_RATIO 비율)을 이어붙인 새 청크 목록을 만든다. */
    private List<Document> withOverlap(List<Document> chunks) {
        List<Document> result = new ArrayList<>();
        String prevText = null;
        for (Document chunk : chunks) {
            String text = chunk.getText();
            if (prevText != null) {
                int overlapLen = (int) (prevText.length() * OVERLAP_RATIO);
                text = prevText.substring(prevText.length() - overlapLen) + text;
            }
            prevText = chunk.getText();                  // 겹침이 누적되지 않게 원본 기준으로 다음 겹침을 계산한다
            result.add(chunk.mutate().text(text).build());
        }
        return result;
    }

    /** 리소스 파일명에서 확장자를 뗀 값을 source(출처) 식별자로 쓴다. 예: return-policy.md → return-policy */
    private String sourceOf(Resource doc) {
        String filename = doc.getFilename();
        return filename.substring(0, filename.lastIndexOf('.'));
    }
}
