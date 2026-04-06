package com.eagle.app;

import com.eagle.app.model.AuditLog;
import com.eagle.app.repository.AuditLogRepository;
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

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditIntegrityTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private AuditLogRepository logs;

    @Test
    void auditRecordsFormAppendOnlyHashChain() throws Exception {
        String token = login("admin.demo", "ChangeMe!1234");
        String payload = mapper.writeValueAsString(Map.of(
                "username", "audit.user",
                "email", "audit.user@eagle.local",
                "password", "Complex#Pass123",
                "role", "REQUESTER",
                "staffContactInfo", "+1-555-0188"
        ));
        mvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        List<AuditLog> all = logs.findAll().stream().sorted(Comparator.comparing(a -> a.id)).toList();
        Assertions.assertTrue(all.size() >= 2);
        for (int i = 1; i < all.size(); i++) {
            Assertions.assertEquals(all.get(i - 1).recordHash, all.get(i).previousHash);
        }
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
