package com.eagle.app.repository;

import com.eagle.app.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Optional<AuditLog> findTopByOrderByIdDesc();
}
