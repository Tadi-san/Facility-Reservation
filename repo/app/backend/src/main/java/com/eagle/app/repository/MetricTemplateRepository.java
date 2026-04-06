package com.eagle.app.repository;

import com.eagle.app.model.MetricTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetricTemplateRepository extends JpaRepository<MetricTemplate, Long> {
    List<MetricTemplate> findByMetricKeyOrderByVersionDesc(String metricKey);
}
