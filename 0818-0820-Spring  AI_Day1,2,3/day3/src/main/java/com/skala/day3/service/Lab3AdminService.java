package com.skala.day3.service;

import com.skala.day3.domain.Ticket;
import com.skala.day3.repository.TicketRepository;
import com.skala.day3.web.dto.lab3.TicketView;
import org.springframework.stereotype.Service;

import java.util.List;

/** 환불 티켓 승인 — 실제 처리 버튼은 사람이 누른다. 모델은 이 서비스를 호출할 경로가 없다. */
@Service
public class Lab3AdminService {

    private final TicketRepository tickets;

    public Lab3AdminService(TicketRepository tickets) {
        this.tickets = tickets;
    }

    public List<TicketView> pending() {
        return tickets.findPending().stream().map(TicketView::from).toList();
    }

    public TicketView approve(String no) {
        Ticket approved = tickets.approve(no).orElseThrow(() -> new TicketNotFoundException(no));
        return TicketView.from(approved);
    }
}
