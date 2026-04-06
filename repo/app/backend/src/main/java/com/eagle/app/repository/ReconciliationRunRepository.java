package com.eagle.app.repository;

import com.eagle.app.model.ReconciliationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, Long> {
    Optional<ReconciliationRun> findByChecksum(String checksum);
}
