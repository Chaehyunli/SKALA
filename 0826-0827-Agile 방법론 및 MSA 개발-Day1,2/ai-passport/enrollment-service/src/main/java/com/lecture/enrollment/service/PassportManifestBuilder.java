package com.lecture.enrollment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lecture.enrollment.entity.AgentInfo;
import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.entity.ExcludedPermissionInfo;
import com.lecture.enrollment.entity.PermissionInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enrollment(발급된 Passport)를 에이전트가 읽는 권한 매니페스트 YAML로 직렬화한다.
 *
 * - allow: 이번 미션에서 실제로 허가된 권한
 * - deny : 요청/검토됐으나 명시적으로 제외된 권한 (이유 포함)
 * - 둘 다 없는 권한 = 미부여 = 거부 (enforcement.default)
 */
final class PassportManifestBuilder {

    private static final YAMLMapper YAML = configure();

    private PassportManifestBuilder() {
    }

    static String toYaml(Enrollment enrollment, Map<String, Object> course) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("apiVersion", "agentpass/v1");
        root.put("kind", "PermissionPassport");

        Map<String, Object> passport = new LinkedHashMap<>();
        passport.put("id", enrollment.getId());
        passport.put("status", enrollment.getStatus());
        passport.put("riskLevel", enrollment.getRiskLevel());
        passport.put("issuedAt",
                enrollment.getApprovedAt() != null ? enrollment.getApprovedAt() : enrollment.getCreatedAt());
        passport.put("expiresAt", enrollment.getExpiredAt());
        passport.put("usagePeriod", course.get("usagePeriod"));
        passport.put("dataSensitivity", course.get("dataSensitivity"));
        root.put("passport", passport);

        root.put("owner", Map.of("userId", enrollment.getUserId()));

        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("courseId", enrollment.getCourseId());
        mission.put("title", course.get("title"));
        mission.put("description", course.get("description"));
        root.put("mission", mission);

        root.put("summary", enrollment.getSummary());

        Map<String, Object> agents = new LinkedHashMap<>();
        for (AgentInfo agent : nullSafe(enrollment.getAgentList())) {
            Map<String, Object> allow = new LinkedHashMap<>();
            for (PermissionInfo p : nullSafe(agent.getPermissions())) {
                allow.put(p.getCode(), permEntry(p.getLabel(), p.getReason(), p.getDescription()));
            }
            Map<String, Object> deny = new LinkedHashMap<>();
            for (ExcludedPermissionInfo p : nullSafe(agent.getExcludedPermissions())) {
                deny.put(p.getCode(), permEntry(p.getLabel(), p.getReason(), p.getDescription()));
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("allow", allow);
            entry.put("deny", deny);
            agents.put(agent.getAgentCode(), entry);
        }
        root.put("agents", agents);

        Map<String, Object> enforcement = new LinkedHashMap<>();
        enforcement.put("default", "deny");        // allow/deny 어디에도 없으면 거부
        enforcement.put("requireStatus", "ACTIVE"); // status != ACTIVE 이면 전체 거부
        enforcement.put("onExpired", "deny-all");   // now > expiresAt 이면 전체 거부
        root.put("enforcement", enforcement);

        try {
            return YAML.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Passport YAML 생성 실패", e);
        }
    }

    private static Map<String, Object> permEntry(String label, String reason, String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("reason", reason);
        if (description != null && !description.isBlank()) {
            m.put("description", description.trim());
        }
        return m;
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : List.of();
    }

    private static YAMLMapper configure() {
        YAMLMapper mapper = new YAMLMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        mapper.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        return mapper;
    }
}
