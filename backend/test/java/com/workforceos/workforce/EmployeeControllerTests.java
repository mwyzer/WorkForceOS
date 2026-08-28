package com.workforceos.workforce;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import com.workforceos.bootstrap.WorkforceOsApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.security.enabled=false")
class EmployeeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsEmployee() throws Exception {
        UUID departmentId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        String request = """
                {
                  "employeeNumber": "EMP-001",
                  "firstName": "Amina",
                  "lastName": "Rahman",
                  "email": "amina@example.com",
                  "departmentId": "%s",
                  "teamId": "%s"
                }
                """.formatted(departmentId, teamId);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/employees/")))
                .andExpect(jsonPath("$.employeeNumber").value("EMP-001"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void deactivatesEmployee() throws Exception {
        UUID employeeId = createEmployee("EMP-002");

        mockMvc.perform(delete("/api/v1/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    private UUID createEmployee(String employeeNumber) throws Exception {
        String request = """
                {
                  "employeeNumber": "%s",
                  "firstName": "Test",
                  "lastName": "Employee",
                  "email": "%s@example.com",
                  "departmentId": "%s",
                  "teamId": "%s"
                }
                """.formatted(employeeNumber, employeeNumber.toLowerCase(), UUID.randomUUID(), UUID.randomUUID());

        String location = mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }
}
