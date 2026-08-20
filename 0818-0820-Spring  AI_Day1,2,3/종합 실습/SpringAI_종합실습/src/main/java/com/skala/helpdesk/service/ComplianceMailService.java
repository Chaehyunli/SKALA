package com.skala.helpdesk.service;

import com.skala.helpdesk.domain.ComplianceTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 신고 티켓이 생성될 때 담당자에게 확인 메일을 보낸다.
 *
 * <p>메일 발송은 매수 흐름의 부가 기능이다 — SMTP 설정이 비어 있거나 실패해도
 * 매수 자체(주 업무 흐름)를 막으면 안 되므로 예외를 삼키고 로그만 남긴다.
 */
@Service
public class ComplianceMailService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceMailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String to;

    public ComplianceMailService(JavaMailSender mailSender,
                                  @Value("${spring.mail.username:}") String from,
                                  @Value("${compliance.mail-to:}") String to) {
        this.mailSender = mailSender;
        this.from = from;
        this.to = to;
    }

    public void notify(ComplianceTicket ticket) {
        if (to.isBlank() || from.isBlank()) {
            log.info("MAIL_USERNAME/COMPLIANCE_MAIL_TO 미설정 — 신고 메일 발송을 건너뜁니다. ticket={}", ticket.no());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[HelpDesk] 해외주식 신고 대상 발생 — " + ticket.symbol());
        message.setText("""
                신고 티켓 번호: %s
                사용자: %s
                신고 대상: %s
                전체 해외주식 평가액: 약 $%,.2f (신고 기준액 이상)

                관리자 승인 API로 처리해 주세요: POST /api/admin/tickets/%s/approve
                """.formatted(ticket.no(), ticket.userId(), ticket.symbol(), ticket.valuationUsd(), ticket.no()));
        try {
            mailSender.send(message);
            log.info("신고 메일 발송 완료 — ticket={}, to={}", ticket.no(), to);
        } catch (MailException e) {
            log.warn("신고 메일 발송 실패 — ticket={}", ticket.no(), e);
        }
    }
}
