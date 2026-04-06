package com.eagle.app;

import com.eagle.app.model.User;
import com.eagle.app.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FrontDeskTemplateHandoffIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserRepository users;

    @Test
    void bannerTemplateAndShiftHandoffEndpointsWork() throws Exception {
        String token = login("agent.demo", "ChangeMe!1234");
        mvc.perform(post("/api/v1/frontdesk/banner-templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "templateKey", "ARRIVAL_05M",
                                "minutesBefore", 5,
                                "message", "Arrival in 5 minutes",
                                "active", true
                        ))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/frontdesk/banner-templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        User from = users.findByUsername("agent.demo").orElseThrow();
        User to = users.findByUsername("agent.demo").orElseThrow();
        mvc.perform(post("/api/v1/frontdesk/shift-handoffs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "fromUserId", from.id,
                                "toUserId", to.id,
                                "handoffTime", Instant.now().toString(),
                                "summary", "Night shift handoff",
                                "pendingTasks", "Inspect late check-ins"
                        ))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/frontdesk/shift-handoffs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
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
