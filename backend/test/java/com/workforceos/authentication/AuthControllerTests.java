package com.workforceos.authentication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.workforceos.bootstrap.WorkforceOsApplication;

@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
class AuthControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginReturnsBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void invalidCredentialsAndProtectedRoutesAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedCredentialsAreRejectedWithoutServerError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":null,\"password\":\"admin\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registersTenantAndReturnsAdminToken() throws Exception {
        String request = """
                {
                  "organizationName": "Acme Global",
                  "timezone": "UTC",
                  "adminUsername": "acme_lead",
                  "adminPassword": "SecretPassword123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationName").value("Acme Global"))
                .andExpect(jsonPath("$.username").value("acme_lead"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void rejectsDuplicateUsernameOnTenantRegistration() throws Exception {
        String request = """
                {
                  "organizationName": "Conflict Org",
                  "timezone": "UTC",
                  "adminUsername": "admin",
                  "adminPassword": "SecretPassword123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsMissingFieldsOnTenantRegistration() throws Exception {
        String request = """
                {
                  "organizationName": "",
                  "timezone": "UTC",
                  "adminUsername": "valid_user",
                  "adminPassword": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register-tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
