package com.workforceos.organization;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workforceos.bootstrap.WorkforceOsApplication;

@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OrganizationAdminTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanCreateAndListOrganizations() throws Exception {
        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", bearer(login("admin", "admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme\",\"timezone\":\"UTC\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme"))
                .andExpect(jsonPath("$.timezone").value("UTC"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/v1/organizations")
                        .header("Authorization", bearer(login("admin", "admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void nonAdminCannotManageOrganizations() throws Exception {
        String managerToken = login("manager", "manager");

        mockMvc.perform(get("/api/v1/organizations")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme\",\"timezone\":\"UTC\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void provisionedAccountResolvesToItsOwnTenant() throws Exception {
        String adminToken = login("admin", "admin");
        String location = mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Beta Org\",\"timezone\":\"Europe/London\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        String organizationId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(post("/api/v1/admin/accounts")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"operator","password":"secret","organizationId":"%s","roles":["MANAGER"]}
                                """.formatted(organizationId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("operator"))
                .andExpect(jsonPath("$.roles[0]").value("MANAGER"));

        mockMvc.perform(get("/api/v1/organizations/current")
                        .header("Authorization", bearer(login("operator", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(organizationId));
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}