package com.anish.auditlogservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:audit_rate_limit_test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "audit.rate-limit.create.max-requests=2",
        "audit.rate-limit.create.window-seconds=60"
})
class CreateEventRateLimitTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsThirdCreateWithinConfiguredWindow() throws Exception {
        String requestBody = """
                {
                  "eventType": "ACCOUNT_VIEWED",
                  "actorId": "rate-limit-test-user",
                  "resourceType": "ACCOUNT",
                  "resourceId": "rate-limit-test-account",
                  "payload": {"action": "view"}
                }
                """;

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/audit/events")
                            .with(httpBasic(
                                    "audit-test-admin",
                                    "test-only-admin-password"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/audit/events")
                        .with(httpBasic(
                                "audit-test-admin",
                                "test-only-admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isTooManyRequests());
    }
}