package com.skala.helpdesk.web.controller;

import com.skala.helpdesk.rag.IngestService;
import com.skala.helpdesk.service.AdminAuthException;
import com.skala.helpdesk.service.ComplianceAdminService;
import com.skala.helpdesk.web.dto.admin.ComplianceTicketView;
import com.skala.helpdesk.web.dto.admin.IngestResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 문서 인제스트 확인, 신고 티켓 승인 — 전부 X-Admin-Key 헤더로 보호한다(day3 Lab3Controller 원칙 그대로). */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "HelpDesk 관리자")
public class AdminController {

    private final IngestService ingestService;
    private final ComplianceAdminService adminService;
    private final VectorStore vectorStore;
    private final String adminKey;

    public AdminController(IngestService ingestService,
                            ComplianceAdminService adminService,
                            VectorStore vectorStore,
                            @Value("${app.admin-key}") String adminKey) {
        this.ingestService = ingestService;
        this.adminService = adminService;
        this.vectorStore = vectorStore;
        this.adminKey = adminKey;
    }

    @PostMapping("/ingest")
    @Operation(summary = "규정 문서 재인제스트", description = "부팅 시 자동 실행되지만, 문서를 고친 뒤 재시작 없이 반영하고 싶을 때 수동으로도 호출할 수 있다.")
    public List<IngestResult> ingest(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return ingestService.ingestAll();
    }

    @GetMapping("/chunks")
    @Operation(summary = "인제스트 품질 확인", description = "성공 메시지가 아니라 실제로 벡터스토어에 무엇이 들어갔는지 눈으로 본다.")
    public List<Map<String, Object>> inspect(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @Parameter(example = "신고 기준액") @RequestParam String q,
            @RequestParam(defaultValue = "5") int topK) {
        requireAdmin(key);
        List<org.springframework.ai.document.Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder().query(q).topK(topK).build());
        return hits.stream().map(d -> Map.<String, Object>of(
                "source", d.getMetadata().get("source"),
                "version", d.getMetadata().get("version"),
                "score", d.getScore(),
                "preview", d.getText().substring(0, Math.min(160, d.getText().length())))).toList();
    }

    @GetMapping("/tickets/pending")
    @Operation(summary = "신고 대기 티켓 목록", description = "PENDING 상태인 해외주식 신고 티켓을 전부 반환한다.")
    public List<ComplianceTicketView> pending(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return adminService.pending();
    }

    @PostMapping("/tickets/{no}/approve")
    @Operation(summary = "신고 승인", description = "PENDING 티켓을 APPROVED로 바꾼다. 모델은 이 API를 호출할 경로가 없다 — 담당자가 직접 처리한다.")
    public ComplianceTicketView approve(@PathVariable String no,
                                         @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdmin(key);
        return adminService.approve(no);
    }

    private void requireAdmin(String key) {
        if (!adminKey.equals(key)) {           // key == null 이어도(헤더 자체가 없어도) 그냥 불일치로 처리한다
            throw new AdminAuthException();
        }
    }
}
