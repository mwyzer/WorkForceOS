package com.workforceos.workforce;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.workforceos.bootstrap.WorkforceOsApplication;

@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.security.enabled=false")
class DepartmentTeamControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsDepartments() throws Exception {
        String request = """
                {
                  "organizationId": "%s",
                  "name": "Operations"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/departments/")))
                .andExpect(jsonPath("$.name").value("Operations"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void createsAndListsTeamsForDepartment() throws Exception {
        UUID departmentId = createDepartment("Operations");

        String request = """
                {
                  "departmentId": "%s",
                  "name": "Warehouse"
                }
                """.formatted(departmentId);

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Warehouse"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/v1/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private UUID createDepartment(String name) throws Exception {
        String request = """
                {
                  "organizationId": "%s",
                  "name": "%s"
                }
                """.formatted(UUID.randomUUID(), name);

        String location = mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }
}
