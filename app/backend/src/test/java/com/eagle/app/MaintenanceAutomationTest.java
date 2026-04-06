package com.eagle.app;

import com.eagle.app.model.MaintenanceTicket;
import com.eagle.app.model.MaintenanceTicketStatus;
import com.eagle.app.repository.MaintenanceTicketRepository;
import com.eagle.app.service.MaintenanceSlaService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

@SpringBootTest
class MaintenanceAutomationTest {
    @Autowired
    private MaintenanceSlaService slaService;
    @Autowired
    private MaintenanceTicketRepository tickets;

    @Test
    void calculatesFourBusinessHourDueDate() {
        Instant from = Instant.parse("2026-04-03T15:00:00Z"); // Friday
        Instant due = slaService.calculateDueAt(from);
        Assertions.assertEquals(Instant.parse("2026-04-06T11:00:00Z"), due);
    }

    @Test
    void overdueFlagIsAutomaticallyApplied() {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.title = "Critical HVAC issue";
        ticket.roomNumber = "A101";
        ticket.description = "No cooling";
        ticket.status = MaintenanceTicketStatus.OPEN;
        ticket.slaDueAt = Instant.now().minusSeconds(300);
        tickets.save(ticket);

        slaService.markOverdueTickets();

        MaintenanceTicket updated = tickets.findById(ticket.id).orElseThrow();
        Assertions.assertTrue(updated.overdue);
        Assertions.assertTrue(updated.slaBreached);
    }
}
