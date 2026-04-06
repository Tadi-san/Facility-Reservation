package com.eagle.app.auth;

import com.eagle.app.config.JwtService;
import com.eagle.app.dto.AuthResponse;
import com.eagle.app.dto.LoginRequest;
import com.eagle.app.model.User;
import com.eagle.app.repository.UserRepository;
import com.eagle.app.service.AuditLogService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {
    private static final int LOCKOUT_ATTEMPTS = 5;
    private static final Duration LOCKOUT_WINDOW = Duration.ofMinutes(15);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, AuditLogService auditLogService) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.auditLogService = auditLogService;
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByUsername(req.username()).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (user.lockUntil != null && user.lockUntil.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Account temporarily locked due to repeated failed logins. Try again later.");
        }
        if (!encoder.matches(req.password(), user.passwordHash)) {
            user.failedAttempts++;
            if (user.failedAttempts >= LOCKOUT_ATTEMPTS) {
                user.lockUntil = Instant.now().plus(LOCKOUT_WINDOW);
                auditLogService.log(user, "AUTH_LOCKOUT_TRIGGERED", "User", String.valueOf(user.id), "Failed attempts reached lockout threshold");
            }
            users.save(user);
            throw new IllegalArgumentException("Invalid credentials");
        }
        user.failedAttempts = 0;
        user.lockUntil = null;
        users.save(user);
        auditLogService.log(user, "AUTH_LOGIN_SUCCESS", "User", String.valueOf(user.id), "User logged in successfully");
        return new AuthResponse(jwt.token(user.username, user.roleName.name()));
    }
}
