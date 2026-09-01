package com.lecture.enrollment.service;

import com.lecture.enrollment.entity.AgentInfo;
import com.lecture.enrollment.entity.Enrollment;
import com.lecture.enrollment.entity.ExcludedPermissionInfo;
import com.lecture.enrollment.entity.PermissionInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PassportManifestBuilderTest {

    @Test
    void splitsGrantedAndExcludedPermissionsPerAgent() {
        Enrollment enrollment = Enrollment.builder()
                .id(1042L)
                .userId(3L)
                .courseId(25L)
                .status(Enrollment.Status.ACTIVE)
                .riskLevel(Enrollment.RiskLevel.MEDIUM)
                .summary("최소 권한만 부여")
                .expiredAt(LocalDateTime.of(2026, 8, 29, 14, 30))
                .agentList(List.of(AgentInfo.builder()
                        .agentCode("REVENUE_ANALYST")
                        .permissions(List.of(PermissionInfo.builder()
                                .code("CUSTOMER_READ").label("CRM 고객정보 조회")
                                .reason("이탈 위험 고객 식별").build()))
                        .excludedPermissions(List.of(ExcludedPermissionInfo.builder()
                                .code("CUSTOMER_DELETE").label("CRM 고객정보 삭제")
                                .reason("업무에 불필요").build()))
                        .build()))
                .build();

        String yaml = PassportManifestBuilder.toYaml(enrollment,
                Map.of("title", "리텐션 캠페인", "description", "이탈 고객 분석",
                        "usagePeriod", "HOURS_24", "dataSensitivity", "사내 제한"));

        assertThat(yaml).contains("kind: PermissionPassport");
        assertThat(yaml).contains("REVENUE_ANALYST:");
        // 허가된 권한은 allow, 제외된 권한은 deny 아래에 위치한다
        int allowIdx = yaml.indexOf("allow:");
        int customerRead = yaml.indexOf("CUSTOMER_READ:");
        int denyIdx = yaml.indexOf("deny:");
        int customerDelete = yaml.indexOf("CUSTOMER_DELETE:");
        assertThat(allowIdx).isLessThan(customerRead);
        assertThat(customerRead).isLessThan(denyIdx);
        assertThat(denyIdx).isLessThan(customerDelete);
        assertThat(yaml).contains("default: deny");
    }
}
