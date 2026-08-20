package com.skala.helpdesk.repository;

import com.skala.helpdesk.domain.ComplianceTicket;
import com.skala.helpdesk.domain.TicketStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** 신고 티켓 저장소. PortfolioRepository 와 달리 런타임에 상태가 바뀌므로(PENDING → APPROVED) ConcurrentHashMap을 쓴다. */
@Repository
public class ComplianceTicketRepository {

    private final Map<String, ComplianceTicket> tickets = new ConcurrentHashMap<>();
    // userId -> 현재 PENDING 티켓 번호. computeIfAbsent로 사용자별 생성을 원자적으로 만들어
    // 동시 매수 요청이 겹쳐도 같은 사용자의 신고 티켓이 두 번 생성되지 않게 한다.
    private final Map<String, String> pendingTicketNoByUser = new ConcurrentHashMap<>();

    /** 사용자에게 PENDING 티켓이 없으면 새로 만들고, 있으면 기존 것을 반환한다. created()로 신규 생성 여부를 구분해 메일은 신규일 때만 보낸다. */
    public TicketCreation createPendingIfAbsent(String userId, String symbol, double valuationUsd) {
        AtomicBoolean created = new AtomicBoolean(false);
        String no = pendingTicketNoByUser.computeIfAbsent(userId, id -> {
            String newNo = UUID.randomUUID().toString().substring(0, 8);
            tickets.put(newNo, new ComplianceTicket(newNo, id, symbol, valuationUsd, TicketStatus.PENDING));
            created.set(true);
            return newNo;
        });
        return new TicketCreation(tickets.get(no), created.get());
    }

    public List<ComplianceTicket> findPending() {
        return tickets.values().stream()
                .filter(t -> t.status() == TicketStatus.PENDING)
                .toList();
    }

    /** 승인 처리 — 실제 처리 버튼은 사람이 누른다. 모델은 이 메서드를 호출할 경로가 없다. */
    public Optional<ComplianceTicket> approve(String no) {
        ComplianceTicket existing = tickets.get(no);
        if (existing == null) {
            return Optional.empty();
        }
        ComplianceTicket approved = existing.approve();
        tickets.put(no, approved);
        pendingTicketNoByUser.remove(existing.userId(), no); // 승인되면 다음 매수 때 새 신고 티켓을 다시 만들 수 있게 해제
        return Optional.of(approved);
    }

    public record TicketCreation(ComplianceTicket ticket, boolean created) {}
}
