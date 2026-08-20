package com.skala.helpdesk.rag;

import com.skala.helpdesk.web.dto.admin.IngestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** 서버가 뜰 때 규정 문서를 자동으로 인제스트한다 — 매번 수동으로 /api/admin/ingest 를 부를 필요가 없다. */
@Component
public class DocsAutoIngestRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocsAutoIngestRunner.class);

    private final IngestService ingestService;

    public DocsAutoIngestRunner(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<IngestResult> results = ingestService.ingestAll();
        log.info("부팅 시 규정 문서 자동 인제스트 완료: {}", results);
    }
}
