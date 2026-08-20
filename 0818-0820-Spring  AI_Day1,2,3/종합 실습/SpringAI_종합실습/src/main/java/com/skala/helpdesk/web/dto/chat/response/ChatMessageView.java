package com.skala.helpdesk.web.dto.chat.response;

/** 대화 이력 한 줄 — role은 "user" 또는 "assistant". */
public record ChatMessageView(String role, String content) {
}
