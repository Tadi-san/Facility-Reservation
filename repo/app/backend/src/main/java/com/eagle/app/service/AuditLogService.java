package com.eagle.app.service;

import com.eagle.app.model.AuditLog;
import com.eagle.app.model.User;
import com.eagle.app.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class AuditLogService {
    private final AuditLogRepository logs;

    public AuditLogService(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Transactional
    public void log(User actor, String action, String resourceType, String resourceId, String details) {
        String previousHash = logs.findTopByOrderByIdDesc().map(v -> v.recordHash).orElse("GENESIS");
        AuditLog log = new AuditLog();
        log.eventAt = Instant.now();
        log.actorUsername = actor == null ? "system" : actor.username;
        log.action = action;
        log.resourceType = resourceType;
        log.resourceId = resourceId;
        log.details = details;
        log.previousHash = previousHash;
        log.recordHash = hash(previousHash + "|" + log.eventAt + "|" + log.actorUsername + "|" + action + "|" + resourceType + "|" + resourceId + "|" + details);
        logs.save(log);
    }

    private String hash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash audit record", ex);
        }
    }
}
