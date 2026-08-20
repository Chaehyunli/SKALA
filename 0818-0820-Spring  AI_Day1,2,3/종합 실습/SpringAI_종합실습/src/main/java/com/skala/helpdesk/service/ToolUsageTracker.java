package com.skala.helpdesk.service;

import org.springframework.stereotype.Component;

/**
 * 이번 턴에서 도구가 실행됐는지 표시하는 스레드 로컬 플래그.
 *
 * <p>Spring AI의 기본(Framework-Controlled) 도구 실행은 {@code .call()} 안에서 같은 스레드로 끝까지
 * 처리되므로, 도구 메서드가 직접 표시(markUsed)하고 호출부(HelpDeskChatService)가 같은 스레드에서
 * reset→call→wasUsed 순서로 읽으면 정확하다. 최종 ChatResponse만 봐서는 중간에 도구가 있었는지
 * 알 수 없어서(마지막 응답엔 이미 도구 호출이 없다) 이 방법을 쓴다.
 */
@Component
public class ToolUsageTracker {

    private final ThreadLocal<Boolean> used = ThreadLocal.withInitial(() -> false);

    public void reset() {
        used.set(false);
    }

    public void markUsed() {
        used.set(true);
    }

    public boolean wasUsed() {
        return used.get();
    }
}
