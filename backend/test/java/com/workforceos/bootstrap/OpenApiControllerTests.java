package com.workforceos.bootstrap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = WorkforceOsApplication.class)
@AutoConfigureMockMvc
class OpenApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesOpenApiYamlContract() throws Exception {
        mockMvc.perform(get("/api/v1/openapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/yaml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("WorkforceOS API")));
    }
}
