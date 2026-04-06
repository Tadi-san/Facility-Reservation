package com.eagle.app.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class JwtService {
    private final SecretKey key;

    public JwtService(@Value("${eagle.jwt-secret}") String secret) {
        String use = secret.length() < 32 ? (secret + "00000000000000000000000000000000") : secret;
        this.key = Keys.hmacShaKeyFor(use.substring(0, 32).getBytes(StandardCharsets.UTF_8));
    }

    public String token(String username, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + 8 * 60 * 60 * 1000L);
        return Jwts.builder()
                .subject(username)
                .claims(Map.of("roles", List.of("ROLE_" + role)))
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
