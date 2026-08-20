package com.skala.helpdesk.service;

import com.skala.helpdesk.domain.ComplianceTicket;
import com.skala.helpdesk.repository.ComplianceTicketRepository;
import com.skala.helpdesk.web.dto.admin.ComplianceTicketView;
import org.springframework.stereotype.Service;

import java.util.List;

/** 신고 티켓 승인 — 실제 처리 버튼은 사람이 누른다. 모델은 이 서비스를 호출할 경로가 없다. */
@Service
public class ComplianceAdminService {

    private final ComplianceTicketRepository tickets;

    public ComplianceAdminService(ComplianceTicketRepository tickets) {
        this.tickets = tickets;
    }

    public List<ComplianceTicketView> pending() {
        return tickets.findPending().stream().map(ComplianceTicketView::from).toList();
    }

    public ComplianceTicketView approve(String no) {
        ComplianceTicket approved = tickets.approve(no).orElseThrow(() -> new ComplianceTicketNotFoundException(no));
        return ComplianceTicketView.from(approved);
    }
}
