package com.skala.helpdesk.web.controller;

import com.skala.helpdesk.service.UserSessionService;
import com.skala.helpdesk.web.dto.auth.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Swagger 등 API 클라이언트에서 세션 쿠키로 로그인하기 위한 엔드포인트. /login 화면과 동일하게 HttpSession에 사용자를 고정한다. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "인증")
public class AuthController {

    private final UserSessionService userSessionService;

    public AuthController(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "등록된 userId로 세션을 발급한다. Swagger의 Execute로 호출하면 이후 요청에 세션 쿠키가 자동으로 포함된다.")
    public void login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        userSessionService.login(session, request.userId());
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public void logout(HttpSession session) {
        session.invalidate();
    }
}
