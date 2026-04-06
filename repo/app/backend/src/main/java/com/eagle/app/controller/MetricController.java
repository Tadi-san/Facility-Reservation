package com.eagle.app.controller;

import com.eagle.app.dto.MetricCreateRequest;
import com.eagle.app.model.MetricTemplate;
import com.eagle.app.repository.MetricTemplateRepository;
import com.eagle.app.service.AuditLogService;
import com.eagle.app.service.CurrentUserService;
import com.eagle.app.service.MetricLibraryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metrics")
@PreAuthorize("hasAnyRole('OPS','ADMIN')")
public class MetricController {
    private final MetricTemplateRepository metrics;
    private final MetricLibraryService service;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public MetricController(MetricTemplateRepository metrics, MetricLibraryService service, CurrentUserService currentUserService, AuditLogService auditLogService) {
        this.metrics = metrics;
        this.service = service;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/scorecards")
    public Page<MetricTemplate> list(Pageable pageable) {
        return metrics.findAll(pageable);
    }

    @PostMapping("/scorecards")
    public MetricTemplate create(@Valid @RequestBody MetricCreateRequest req) {
        MetricTemplate saved = service.create(req.metricKey(), req.effectiveFrom(), req.effectiveTo(), req.definition(), req.weightedDimensions());
        auditLogService.log(currentUserService.requireCurrentUser(), "SCORECARD_CREATED", "MetricTemplate", String.valueOf(saved.id), "Created metric " + saved.metricKey + " v" + saved.version);
        return saved;
    }
}
