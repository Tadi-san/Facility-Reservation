package com.eagle.app;

import com.eagle.app.model.RoleName;
import com.eagle.app.model.User;
import com.eagle.app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserRepository users;
    @Autowired
    private PasswordEncoder encoder;

    @Test
    void reservationsMine_requiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/reservations/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lockout_afterFiveFailedLogins() throws Exception {
        String username = "lockout.demo";
        if (users.findByUsername(username).isEmpty()) {
            User user = new User();
            user.username = username;
            user.email = username + "@eagle.local";
            user.passwordHash = encoder.encode("ChangeMe!1234");
            user.roleName = RoleName.REQUESTER;
            users.save(user);
        }

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("username", username, "password", "WrongPassword!1"))))
                    .andExpect(status().isBadRequest());
        }

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", username, "password", "ChangeMe!1234"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Account temporarily locked due to repeated failed logins. Try again later."));
    }
}
