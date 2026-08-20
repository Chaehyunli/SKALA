package com.skala.helpdesk.integration;

import com.skala.helpdesk.domain.ComplianceTicket;
import com.skala.helpdesk.domain.TicketStatus;
import com.skala.helpdesk.market.ExchangeRate;
import com.skala.helpdesk.market.PriceService;
import com.skala.helpdesk.market.StockQuote;
import com.skala.helpdesk.service.ComplianceMailService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Finnhub(시세)·Frankfurter(환율)·Gmail SMTP(메일)에 실제로 접속해보는 연동 확인용 테스트.
 * Spring 컨텍스트(=pgvector 등)를 띄우지 않고 .env를 직접 읽어 각 클래스를 단독으로 생성해 호출한다 —
 * docker-compose 없이도, 세 외부 연동만 빠르게 확인할 수 있게 하기 위함이다.
 *
 * <p>실제 네트워크 호출(및 메일 발송)이 발생하므로 기본 {@code ./gradlew test}에서는 제외돼 있다.
 * 확인하려면 {@code ./gradlew liveTest} 로 직접 실행한다. .env에 값이 없는 항목은 자동으로 건너뛴다.
 */
class ExternalApiConnectivityTest {

    // application.yml의 market.finnhub.base-url / market.exchange.base-url 기본값과 일치해야 한다.
    private static final String FINNHUB_BASE_URL = "https://finnhub.io/api/v1";
    private static final String EXCHANGE_BASE_URL = "https://api.frankfurter.dev/v1";

    @Test
    @Tag("live")
    void 주식_시세_API_연동_확인() {
        String finnhubKey = env().get("FINNHUB_API_KEY");
        assumeTrue(isSet(finnhubKey), "FINNHUB_API_KEY가 .env에 없어 건너뜁니다.");

        PriceService priceService = new PriceService(FINNHUB_BASE_URL, finnhubKey, EXCHANGE_BASE_URL);
        StockQuote quote = priceService.getQuote("AAPL");

        assertTrue(quote.priceUsd() > 0, "AAPL 현재가는 0보다 커야 한다");
        System.out.printf("[Finnhub] AAPL 현재가 = $%.2f%n", quote.priceUsd());
    }

    @Test
    @Tag("live")
    void 환율_API_연동_확인() {
        // Frankfurter는 API 키가 필요 없으므로 시세 키 설정 여부와 무관하게 항상 시도한다.
        PriceService priceService = new PriceService(FINNHUB_BASE_URL, env().getOrDefault("FINNHUB_API_KEY", ""), EXCHANGE_BASE_URL);
        ExchangeRate rate = priceService.getRate("USD", "KRW");

        assertTrue(rate.rate() > 0, "USD -> KRW 환율은 0보다 커야 한다");
        System.out.printf("[Frankfurter] USD -> KRW = %.2f%n", rate.rate());
    }

    @Test
    @Tag("live")
    void 메일_발송_API_연동_확인() {
        Map<String, String> env = env();
        String username = env.get("MAIL_USERNAME");
        String password = env.get("MAIL_PASSWORD");
        String to = env.get("COMPLIANCE_MAIL_TO");
        assumeTrue(isSet(username) && isSet(password) && isSet(to),
                "MAIL_USERNAME/MAIL_PASSWORD/COMPLIANCE_MAIL_TO가 .env에 없어 건너뜁니다.");

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        Properties mailProps = mailSender.getJavaMailProperties();
        mailProps.put("mail.smtp.auth", "true");
        mailProps.put("mail.smtp.starttls.enable", "true");

        // SMTP 핸드셰이크만으로 자격 증명(앱 비밀번호)이 유효한지 먼저 확인 — 실패하면 메일을 보내지 않고 바로 실패한다.
        assertDoesNotThrow(mailSender::testConnection,
                "Gmail SMTP 인증에 실패했습니다 — MAIL_USERNAME/MAIL_PASSWORD(앱 비밀번호)를 확인하세요.");

        // 실제 서비스 코드(ComplianceMailService)를 그대로 태워 신고 티켓 알림 메일 한 통을 실제로 발송한다.
        ComplianceMailService mailService = new ComplianceMailService(mailSender, username, to);
        ComplianceTicket testTicket = new ComplianceTicket(
                "TEST0001", "connectivity-test", "PORTFOLIO", 12_345.67, TicketStatus.PENDING);
        mailService.notify(testTicket);

        System.out.println("[Mail] " + to + " 로 테스트 메일을 발송했습니다 — 받은편지함에서 확인하세요.");
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    /** .env 파일을 직접 파싱한다 — Spring 컨텍스트 없이도 spring-dotenv가 읽는 값과 동일한 값을 얻기 위함. */
    private static Map<String, String> env() {
        Path envPath = Path.of(".env");
        Map<String, String> values = new HashMap<>();
        if (!Files.exists(envPath)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(envPath)) {
                String trimmed = line.trim();
                int eq = trimmed.indexOf('=');
                if (trimmed.isEmpty() || trimmed.startsWith("#") || eq < 0) {
                    continue;
                }
                values.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return values;
    }
}
