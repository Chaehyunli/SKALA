package com.skala.day3.repository;

import com.skala.day3.domain.Ticket;
import com.skala.day3.domain.TicketStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 환불 티켓 저장소. OrderRepository 와 달리 런타임에 상태가 바뀌므로(PENDING → APPROVED)
 * 불변 Map 이 아니라 ConcurrentHashMap 을 쓴다.
 */
@Repository
public class TicketRepository {

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public Ticket create(String orderId, String userId, String reason) {
        String no = UUID.randomUUID().toString().substring(0, 8);
        Ticket ticket = new Ticket(no, orderId, userId, reason, TicketStatus.PENDING);
        tickets.put(no, ticket);
        return ticket;
    }

    public List<Ticket> findPending() {
        return tickets.values().stream()
                .filter(t -> t.status() == TicketStatus.PENDING)
                .toList();
    }

    /** 승인 처리 — 실제 처리 버튼은 사람이 누른다. 모델은 이 메서드를 호출할 경로가 없다. */
    public Optional<Ticket> approve(String no) {
        Ticket existing = tickets.get(no);
        if (existing == null) {
            return Optional.empty();
        }
        Ticket approved = existing.approve();
        tickets.put(no, approved);
        return Optional.of(approved);
    }
}
