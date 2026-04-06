package com.eagle.app.controller;

import com.eagle.app.service.CanaryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final CanaryService canaryService;

    public HealthController(CanaryService canaryService) {
        this.canaryService = canaryService;
    }

    @GetMapping
    public Map<String, Object> health() {
        return Map.of("status", "UP", "timestamp", Instant.now().toString());
    }

    @GetMapping("/canary")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> canary() {
        return canaryService.run();
    }
}
