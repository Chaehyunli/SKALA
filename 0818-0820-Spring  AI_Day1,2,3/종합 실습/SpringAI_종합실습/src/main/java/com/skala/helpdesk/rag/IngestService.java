package com.skala.helpdesk.rag;

import com.skala.helpdesk.web.dto.admin.IngestResult;
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
import java.util.List;

/**
 * RAG 파이프라인의 1단계 — 인제스트. 흐름: 읽기(TextReader) → 분할(TokenTextSplitter) →
 * 메타데이터 부여 → 저장(VectorStore). source·version 메타데이터는 여기서 심어 둬야
 * 답변 단계에서 출처로 되꺼낼 수 있다.
 */
@Service
public class IngestService {

    private static final String DOCS_LOCATION = "classpath:regulation-docs/*.md";
    private static final String VERSION = "v1";
    private static final int CHUNK_SIZE = 400;
    private static final int MIN_CHUNK_SIZE_CHARS = 200;

    private final VectorStore vectorStore;

    public IngestService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /** regulation-docs 아래 .md 문서를 전부 (재)인제스트한다. 문서별로 delete-then-add 하므로 중복 저장되지 않는다. */
    public List<IngestResult> ingestAll() {
        try {
            Resource[] docs = new PathMatchingResourcePatternResolver().getResources(DOCS_LOCATION);
            return List.of(docs).stream().map(this::ingest).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private IngestResult ingest(Resource doc) {
        String source = sourceOf(doc);

        var reader = new TextReader(doc);
        reader.getCustomMetadata().put("version", VERSION);

        var splitter = TokenTextSplitter.builder()
                .withChunkSize(CHUNK_SIZE)
                .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
                .build();
        List<Document> chunks = splitter.apply(reader.get());
        // TextReader.get()이 항상 resource 파일명으로 source를 덮어쓰므로, 우리가 원하는 값으로 다시 심는다.
        chunks.forEach(chunk -> chunk.getMetadata().put("source", source));

        vectorStore.delete(new FilterExpressionBuilder().eq("source", source).build());
        vectorStore.add(chunks);
        return new IngestResult(source, chunks.size());
    }

    private String sourceOf(Resource doc) {
        String filename = doc.getFilename();
        return filename.substring(0, filename.lastIndexOf('.'));
    }
}
