package com.skala.helpdesk.repository;

import com.skala.helpdesk.domain.ComplianceTicket;
import com.skala.helpdesk.domain.TicketStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 신고 티켓 저장소. PortfolioRepository 와 달리 런타임에 상태가 바뀌므로(PENDING → APPROVED) ConcurrentHashMap을 쓴다. */
@Repository
public class ComplianceTicketRepository {

    private final Map<String, ComplianceTicket> tickets = new ConcurrentHashMap<>();

    public ComplianceTicket create(String userId, String symbol, double valuationUsd) {
        String no = UUID.randomUUID().toString().substring(0, 8);
        ComplianceTicket ticket = new ComplianceTicket(no, userId, symbol, valuationUsd, TicketStatus.PENDING);
        tickets.put(no, ticket);
        return ticket;
    }

    public List<ComplianceTicket> findPending() {
        return tickets.values().stream()
                .filter(t -> t.status() == TicketStatus.PENDING)
                .toList();
    }

    /** 동일 사용자는 신고 대기 티켓 하나로 관리해, 추가 매수 때 티켓이 중복 생성되지 않게 한다. */
    public Optional<ComplianceTicket> findPendingByUserId(String userId) {
        return tickets.values().stream()
                .filter(t -> t.userId().equals(userId))
                .filter(t -> t.status() == TicketStatus.PENDING)
                .findFirst();
    }

    /** 승인 처리 — 실제 처리 버튼은 사람이 누른다. 모델은 이 메서드를 호출할 경로가 없다. */
    public Optional<ComplianceTicket> approve(String no) {
        ComplianceTicket existing = tickets.get(no);
        if (existing == null) {
            return Optional.empty();
        }
        ComplianceTicket approved = existing.approve();
        tickets.put(no, approved);
        return Optional.of(approved);
    }
}
