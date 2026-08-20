package com.skala.helpdesk.web.dto.admin;

import com.skala.helpdesk.domain.ComplianceTicket;
import io.swagger.v3.oas.annotations.media.Schema;

/** 신고 티켓 생성 결과 겸 관리자 승인 API 응답 — 두 곳 다 같은 모양이면 충분하다. */
public record ComplianceTicketView(
        @Schema(description = "신고 티켓 번호", example = "df4d989d") String no,
        @Schema(description = "사용자 ID", example = "user1") String userId,
        @Schema(description = "신고 대상. 포트폴리오 전체 평가액 기준이면 PORTFOLIO", example = "PORTFOLIO") String symbol,
        @Schema(description = "신고 시점 평가액(달러)", example = "12500.0") double valuationUsd,
        @Schema(description = "PENDING(승인 대기) 또는 APPROVED(승인 완료)", example = "PENDING") String status) {

    public static ComplianceTicketView from(ComplianceTicket ticket) {
        return new ComplianceTicketView(ticket.no(), ticket.userId(), ticket.symbol(), ticket.valuationUsd(),
                ticket.status().name());
    }
}
