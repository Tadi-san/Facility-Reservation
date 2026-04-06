package com.eagle.app.service;

import com.eagle.app.repository.OfflineOrderRepository;
import com.eagle.app.repository.ReservationRepository;
import com.eagle.app.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class CanaryService {
    private final UserRepository users;
    private final ReservationRepository reservations;
    private final OfflineOrderRepository orders;

    public CanaryService(UserRepository users, ReservationRepository reservations, OfflineOrderRepository orders) {
        this.users = users;
        this.reservations = reservations;
        this.orders = orders;
    }

    public Map<String, Object> run() {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "usersReachable", users.count() >= 0,
                "reservationsReachable", reservations.count() >= 0,
                "ordersReachable", orders.count() >= 0,
                "status", "PASS"
        );
    }
}
