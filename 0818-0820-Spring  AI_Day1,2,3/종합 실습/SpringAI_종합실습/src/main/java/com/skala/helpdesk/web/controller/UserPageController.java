package com.skala.helpdesk.web.controller;

import com.skala.helpdesk.repository.UserRepository;
import com.skala.helpdesk.service.UserSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 실습 데모용 사용자 선택·추가 화면. 선택된 ID는 세션에 저장되어 API 요청과 대조된다. */
@Controller
public class UserPageController {

    private final UserRepository users;
    private final UserSessionService userSessionService;

    public UserPageController(UserRepository users, UserSessionService userSessionService) {
        this.users = users;
        this.userSessionService = userSessionService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        try {
            userSessionService.currentUserId(session);
            return "chat";
        } catch (Exception ignored) {
            return "redirect:/login";
        }
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("users", users.findAll());
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String userId, HttpSession session) {
        userSessionService.login(session, userId);
        return "redirect:/";
    }

    @PostMapping("/users")
    public String addUser(@RequestParam String userId, HttpSession session, RedirectAttributes redirectAttributes) {
        String normalized = userId == null ? "" : userId.trim();
        if (!normalized.matches("[a-zA-Z0-9_-]{3,20}")) {
            redirectAttributes.addFlashAttribute("error", "사용자 ID는 영문·숫자·_·- 조합의 3~20자여야 합니다.");
            return "redirect:/login";
        }
        users.add(normalized);
        userSessionService.login(session, normalized);
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
