package com.eagle.app;

import com.eagle.app.model.Reservation;
import com.eagle.app.model.ReservationStatus;
import com.eagle.app.model.User;
import com.eagle.app.repository.ReservationRepository;
import com.eagle.app.repository.RoomRepository;
import com.eagle.app.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OfflineOrderOwnershipTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private UserRepository users;
    @Autowired
    private ReservationRepository reservations;
    @Autowired
    private RoomRepository rooms;
    @Autowired
    private PasswordEncoder encoder;

    @Test
    void requesterCannotAttachOrderToOtherUsersReservation() throws Exception {
        User other = new User();
        other.username = "other.requester";
        other.email = "other.requester@eagle.local";
        other.passwordHash = encoder.encode("ChangeMe!1234");
        other.roleName = com.eagle.app.model.RoleName.REQUESTER;
        users.save(other);

        Reservation reservation = new Reservation();
        reservation.requester = other;
        reservation.room = rooms.findAll().get(0);
        reservation.startTime = Instant.now().plusSeconds(3600);
        reservation.endTime = Instant.now().plusSeconds(7200);
        reservation.status = ReservationStatus.APPROVED;
        reservations.save(reservation);

        String token = login("requester.demo", "ChangeMe!1234");
        String payload = mapper.writeValueAsString(Map.of(
                "amount", 50.00,
                "idempotencyKey", UUID.randomUUID().toString(),
                "reservationId", reservation.id
        ));

        mvc.perform(post("/api/v1/finance/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
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
