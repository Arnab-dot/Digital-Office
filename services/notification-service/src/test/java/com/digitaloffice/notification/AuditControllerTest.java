package com.digitaloffice.notification;

import com.digitaloffice.contracts.MembershipRole;
import com.digitaloffice.contracts.MembershipSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService service;

    @MockBean
    private OrganizationMembershipClient membershipClient;

    @MockBean
    private JwtDecoder jwtDecoder;

    private final UUID orgId = UUID.randomUUID();

    @Test
    void testGetAuditLogs_OwnerOrAdmin_Allowed() throws Exception {
        when(membershipClient.activeMemberships(any(), any()))
                .thenReturn(List.of(new MembershipSummary(orgId, MembershipRole.ADMIN)));

        AuditLog log = new AuditLog(
                orgId, UUID.randomUUID(), "ROLE_CHANGED", "USER",
                UUID.randomUUID(), "organization-service", "SUCCESS", "corr-1", "{}"
        );
        when(service.getOrganizationAuditLogs(eq(orgId), any(), any(), eq(0), eq(20)))
                .thenReturn(new PageImpl<>(List.of(log)));

        mockMvc.perform(get("/api/v1/organizations/{orgId}/audit-logs", orgId)
                        .with(jwt().jwt(j -> j.subject("user-admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("ROLE_CHANGED"));
    }

    @Test
    void testGetAuditLogs_NormalMember_Forbidden() throws Exception {
        when(membershipClient.activeMemberships(any(), any()))
                .thenReturn(List.of(new MembershipSummary(orgId, MembershipRole.MEMBER)));

        mockMvc.perform(get("/api/v1/organizations/{orgId}/audit-logs", orgId)
                        .with(jwt().jwt(j -> j.subject("user-member"))))
                .andExpect(status().isForbidden());
    }
}

