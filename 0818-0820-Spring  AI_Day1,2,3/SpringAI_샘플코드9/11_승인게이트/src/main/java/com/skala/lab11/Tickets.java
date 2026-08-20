package com.skala.lab11;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

/**
 * 11장 미니 실습 — 승인 대기 티켓 보관소.
 *
 * <p>접수와 승인을 분리하는 것이 이 실습의 핵심이다. 접수는 도구가 할 수 있지만
 * 승인은 사람만 할 수 있다.
 */
@Component
public class Tickets {

    public enum 상태 { PENDING, APPROVED }

    public record Ticket(String no, String userId, String 품목, 상태 status) {}

    private final List<Ticket> 목록 = new ArrayList<>();
    private final AtomicInteger seq = new AtomicInteger(7);

    public synchronized Ticket create(String userId, String 품목) {
        Ticket t = new Ticket("T-%04d".formatted(seq.getAndIncrement()), userId, 품목, 상태.PENDING);
        목록.add(t);
        return t;
    }

    public synchronized List<Ticket> pending() {
        return 목록.stream().filter(t -> t.status() == 상태.PENDING).toList();
    }

    public synchronized Optional<Ticket> approve(String no) {
        for (int i = 0; i < 목록.size(); i++) {
            Ticket t = 목록.get(i);
            if (t.no().equals(no)) {
                Ticket 승인 = new Ticket(t.no(), t.userId(), t.품목(), 상태.APPROVED);
                목록.set(i, 승인);
                return Optional.of(승인);
            }
        }
        return Optional.empty();
    }
}
