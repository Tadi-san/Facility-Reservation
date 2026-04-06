package com.eagle.app.repository;

import com.eagle.app.model.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    boolean existsByRoomNumberAndInspectionTimeAfter(String roomNumber, Instant since);
}
