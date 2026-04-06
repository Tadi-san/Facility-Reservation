package com.eagle.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PersonaWorkflowAuthorizationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void roleSpecificWorkspaceEndpointsAreAccessible() throws Exception {
        String requester = login("requester.demo", "ChangeMe!1234");
        mvc.perform(get("/api/v1/reservations/mine").header(HttpHeaders.AUTHORIZATION, "Bearer " + requester))
                .andExpect(status().isOk());

        String agent = login("agent.demo", "ChangeMe!1234");
        mvc.perform(get("/api/v1/frontdesk/schedule-board").header(HttpHeaders.AUTHORIZATION, "Bearer " + agent))
                .andExpect(status().isOk());

        String tech = login("tech.demo", "ChangeMe!1234");
        mvc.perform(get("/api/v1/maintenance/tickets").header(HttpHeaders.AUTHORIZATION, "Bearer " + tech))
                .andExpect(status().isOk());

        String ops = login("ops.demo", "ChangeMe!1234");
        mvc.perform(get("/api/v1/operations/promotions").header(HttpHeaders.AUTHORIZATION, "Bearer " + ops))
                .andExpect(status().isOk());

        String admin = login("admin.demo", "ChangeMe!1234");
        mvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk());
    }

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = mapper.readTree(body);
        return node.get("token").asText();
    }
}
