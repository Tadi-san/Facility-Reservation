package com.eagle.app.repository;

import com.eagle.app.model.MaintenanceTicket;
import com.eagle.app.model.MaintenanceTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {
    List<MaintenanceTicket> findByStatusInAndSlaDueAtBefore(List<MaintenanceTicketStatus> statuses, Instant dueBefore);
}
