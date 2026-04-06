package com.eagle.app.service;

import com.eagle.app.config.SensitiveDataCrypto;
import com.eagle.app.repository.OfflineOrderRepository;
import com.eagle.app.repository.ReservationRepository;
import com.eagle.app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Service
public class EncryptedBackupService {
    private final UserRepository users;
    private final ReservationRepository reservations;
    private final OfflineOrderRepository orders;
    private final SensitiveDataCrypto crypto;
    private final ObjectMapper mapper;

    public EncryptedBackupService(UserRepository users,
                                  ReservationRepository reservations,
                                  OfflineOrderRepository orders,
                                  SensitiveDataCrypto crypto,
                                  ObjectMapper mapper) {
        this.users = users;
        this.reservations = reservations;
        this.orders = orders;
        this.crypto = crypto;
        this.mapper = mapper;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void writeEncryptedNightlySnapshot() {
        try {
            Map<String, Object> snapshot = Map.of(
                    "generatedAt", Instant.now().toString(),
                    "users", users.count(),
                    "reservations", reservations.count(),
                    "offlineOrders", orders.count()
            );
            String json = mapper.writeValueAsString(snapshot);
            String encrypted = crypto.encrypt(json);
            Path dir = Path.of("backups");
            Files.createDirectories(dir);
            Path file = dir.resolve("eagle-" + LocalDate.now() + ".enc");
            Files.writeString(file, encrypted);
        } catch (Exception ex) {
            // keep backup failures isolated from request path
        }
    }
}
