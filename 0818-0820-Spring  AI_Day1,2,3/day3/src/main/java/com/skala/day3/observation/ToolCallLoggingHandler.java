package com.skala.day3.observation;

import com.skala.day3.advisor.AuditAdvisor;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import org.springframework.stereotype.Component;

/**
 * 도구 호출을 하나하나 잡아 로그로 남긴다 — ChatClient Advisor 가 아니라 Spring AI 가
 * 도구 실행마다 감싸 두는 Observation 을 가로챈다({@link ToolCallingObservationContext}).
 *
 * <p>Advisor 는 ChatClient.call() 전체를 한 번만 감싸므로, 모델 내부에서 도구가 몇 번
 * 호출됐는지는 볼 수 없다({@code chain.nextCall()} 안에서 다 끝나고 최종 응답만 나옴).
 * 반면 이 Observation 은 도구가 실제로 실행되는 순간을 감싸므로, 도구가 몇 개든 이
 * 핸들러 하나로 전부 잡힌다 — 도구마다 로그 코드를 넣을 필요가 없다.
 *
 * <p>사용자(userId)는 이 Observation 이 모르는 정보라서, AuditAdvisor 가 턴 시작 시
 * MDC 에 심어 둔 값을 그대로 읽는다.
 */
@Component
public class ToolCallLoggingHandler implements ObservationHandler<ToolCallingObservationContext> {

    private static final Logger log = LoggerFactory.getLogger(ToolCallLoggingHandler.class);

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ToolCallingObservationContext;
    }

    @Override
    public void onStop(ToolCallingObservationContext context) {
        log.info("[{}] {} tool={} args={} result={}",
                traceId(), userId(), context.getToolDefinition().name(),
                context.getToolCallArguments(), context.getToolCallResult());
    }

    @Override
    public void onError(ToolCallingObservationContext context) {
        log.warn("[{}] {} tool={} args={} error={}",
                traceId(), userId(), context.getToolDefinition().name(),
                context.getToolCallArguments(), context.getError().getMessage());
    }

    private String userId() {
        String userId = MDC.get(AuditAdvisor.MDC_USER_ID);
        return userId != null ? userId : "-";
    }

    private String traceId() {
        String traceId = MDC.get(AuditAdvisor.MDC_TRACE_ID);
        return traceId != null ? traceId : "-";
    }
}
