package com.skala.helpdesk.service;

import com.skala.helpdesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** front.md 검증 시나리오(§4) — sessionId는 userId로부터만 유도되고, 본인 것과 다르면 거부된다. */
class UserSessionServiceTest {

    private final UserSessionService service = new UserSessionService(new UserRepository());
    private MockHttpSession session;

    @BeforeEach
    void 로그인() {
        session = new MockHttpSession();
        service.login(session, "user1");
    }

    @Test
    void sessionId를_생략하면_userId로부터_유도된값을_채워준다() {
        assertEquals("user1-session", service.requireOwnSessionId(session, null));
        assertEquals("user1-session", service.requireOwnSessionId(session, ""));
    }

    @Test
    void 본인의_sessionId를_보내면_그대로_통과한다() {
        assertEquals("user1-session", service.requireOwnSessionId(session, "user1-session"));
    }

    @Test
    void 다른_사용자의_sessionId를_보내면_거부된다() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.requireOwnSessionId(session, "user2-session"));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void 임의값을_보내면_거부된다() {
        assertThrows(ResponseStatusException.class,
                () -> service.requireOwnSessionId(session, "아무값"));
    }

    @Test
    void 같은_userId는_항상_같은_sessionId로_결정론적이다() {
        assertEquals(service.deriveSessionId("user1"), service.deriveSessionId("user1"));
    }
}
