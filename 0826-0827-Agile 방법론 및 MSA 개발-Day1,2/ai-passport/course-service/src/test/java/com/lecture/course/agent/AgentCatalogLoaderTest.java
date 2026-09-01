package com.lecture.course.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentCatalogLoaderTest {

    @Test
    void loadsTheFourBundledAgentPolicies() {
        AgentCatalogLoader loader = new AgentCatalogLoader();
        loader.loadCatalog();

        assertThat(loader.getCatalog().selectableAgentCodes())
                .containsExactlyInAnyOrder(
                        "COMMON_AGENT",
                        "CUSTOMER_SUPPORT",
                        "MEETING_COORDINATOR",
                        "REVENUE_ANALYST");
        assertThat(loader.getCatalog().requireAgent("COMMON_AGENT").permissionsByCode())
                .containsKeys("DOCUMENT_READ", "MAIL_DRAFT_CREATE");
    }
}
