package com.eagle.app;

import com.eagle.app.repository.SearchIndexDocumentRepository;
import com.eagle.app.service.NotificationJobService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
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
class SearchQueueIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private NotificationJobService jobs;
    @Autowired
    private SearchIndexDocumentRepository index;

    @Test
    void queuedReindexJobBuildsSearchDocuments() throws Exception {
        String token = login("ops.demo", "ChangeMe!1234");
        mvc.perform(post("/api/v1/operations/search/reindex")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        jobs.processQueue();
        Assertions.assertTrue(index.count() > 0, "Expected reindex queue job to create search documents.");

        mvc.perform(get("/api/v1/operations/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("q", "A101"))
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
