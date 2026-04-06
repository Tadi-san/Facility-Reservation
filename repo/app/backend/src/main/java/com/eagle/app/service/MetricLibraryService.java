package com.eagle.app.service;

import com.eagle.app.model.MetricTemplate;
import com.eagle.app.repository.MetricTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class MetricLibraryService {
    private final MetricTemplateRepository metrics;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final ObjectMapper objectMapper;

    public MetricLibraryService(MetricTemplateRepository metrics, ObjectMapper objectMapper) {
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    public MetricTemplate create(String key, String from, String to, String definition, Map<String, Integer> weights) {
        int sum = weights == null ? 0 : weights.values().stream().mapToInt(Integer::intValue).sum();
        if (sum != 100) throw new IllegalArgumentException("Weights must sum to 100");
        if (from == null || from.isBlank()) throw new IllegalArgumentException("effectiveFrom is required");

        LocalDate effectiveFrom = LocalDate.parse(from.trim(), fmt);
        LocalDate effectiveTo = (to == null || to.isBlank()) ? null : LocalDate.parse(to.trim(), fmt);
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveTo must be on or after effectiveFrom");
        }

        List<MetricTemplate> existing = metrics.findByMetricKeyOrderByVersionDesc(key);
        int version = existing.isEmpty() ? 1 : existing.get(0).version + 1;

        MetricTemplate m = new MetricTemplate();
        m.metricKey = key;
        m.version = version;
        m.definition = definition;
        m.effectiveFrom = effectiveFrom;
        m.effectiveTo = effectiveTo;
        try {
            m.weightedDimensionsJson = objectMapper.writeValueAsString(weights);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not serialize weighted dimensions");
        }
        return metrics.save(m);
    }
}
