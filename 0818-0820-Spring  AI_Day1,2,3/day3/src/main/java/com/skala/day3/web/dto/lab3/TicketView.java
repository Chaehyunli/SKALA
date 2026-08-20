package com.skala.day3.web.dto.lab3;

import com.skala.day3.domain.Ticket;
import io.swagger.v3.oas.annotations.media.Schema;

/** requestRefund 도구의 반환값 겸 관리자 승인 API 응답 — 두 곳 다 같은 모양이면 충분하다. */
public record TicketView(
        @Schema(description = "환불 티켓 번호", example = "df4d989d") String no,
        @Schema(description = "환불 대상 주문번호", example = "12345") String orderId,
        @Schema(description = "PENDING(승인 대기) 또는 APPROVED(승인 완료)", example = "PENDING") String status,
        @Schema(description = "환불 사유", example = "단순 변심") String reason) {

    public static TicketView from(Ticket ticket) {
        return new TicketView(ticket.no(), ticket.orderId(), ticket.status().name(), ticket.reason());
    }
}
