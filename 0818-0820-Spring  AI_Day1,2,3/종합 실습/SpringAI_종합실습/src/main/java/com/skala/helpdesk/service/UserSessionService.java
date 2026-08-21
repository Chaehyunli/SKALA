package com.skala.helpdesk.service;

import com.skala.helpdesk.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 로그인 화면에서 고른 사용자를 HTTP 세션에 고정한다. API body의 userId는 이 값과 일치해야 한다. */
@Service
public class UserSessionService {

    private static final String USER_ID_ATTRIBUTE = "helpdesk.userId";

    private final UserRepository users;

    public UserSessionService(UserRepository users) {
        this.users = users;
    }

    public void login(HttpSession session, String userId) {
        if (!users.exists(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "등록되지 않은 사용자입니다.");
        }
        session.setAttribute(USER_ID_ATTRIBUTE, userId);
    }

    public String currentUserId(HttpSession session) {
        Object userId = session.getAttribute(USER_ID_ATTRIBUTE);
        if (!(userId instanceof String id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "먼저 사용자를 선택해 주세요.");
        }
        return id;
    }

    public String requireSameUser(HttpSession session, String requestedUserId) {
        String current = currentUserId(session);
        if (!current.equals(requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "로그인한 사용자와 요청 userId가 일치하지 않습니다.");
        }
        return current;
    }

    public String deriveSessionId(String userId) {
        return userId + "-session";
    }

    /** 요청의 sessionId가 없으면 채워주고, 있으면 로그인한 사용자 것과 일치하는지 검증한다. */
    public String requireOwnSessionId(HttpSession session, String requestedSessionId) {
        String expected = deriveSessionId(currentUserId(session));
        if (requestedSessionId != null && !requestedSessionId.isBlank() && !requestedSessionId.equals(expected)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 세션이 아닙니다.");
        }
        return expected;
    }
}
