package com.eagle.app.service;

import com.eagle.app.model.MaintenanceTicket;
import com.eagle.app.model.MaintenanceTicketStatus;
import com.eagle.app.repository.MaintenanceTicketRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class MaintenanceSlaService {
    private static final int BUSINESS_START_HOUR = 9;
    private static final int BUSINESS_END_HOUR = 17;
    private static final int SLA_BUSINESS_HOURS = 4;

    private final MaintenanceTicketRepository tickets;

    public MaintenanceSlaService(MaintenanceTicketRepository tickets) {
        this.tickets = tickets;
    }

    public Instant calculateDueAt(Instant fromInstant) {
        LocalDateTime cursor = LocalDateTime.ofInstant(fromInstant, ZoneOffset.UTC);
        int remaining = SLA_BUSINESS_HOURS;
        while (remaining > 0) {
            cursor = normalizeToBusinessTime(cursor);
            int availableHours = BUSINESS_END_HOUR - cursor.getHour();
            if (availableHours <= 0) {
                cursor = cursor.plusDays(1).withHour(BUSINESS_START_HOUR).withMinute(0).withSecond(0).withNano(0);
                continue;
            }
            int consume = Math.min(remaining, availableHours);
            cursor = cursor.plusHours(consume);
            remaining -= consume;
        }
        return cursor.toInstant(ZoneOffset.UTC);
    }

    private LocalDateTime normalizeToBusinessTime(LocalDateTime input) {
        LocalDateTime cursor = input.withSecond(0).withNano(0);
        while (cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
            cursor = cursor.plusDays(1).withHour(BUSINESS_START_HOUR).withMinute(0);
        }
        if (cursor.getHour() < BUSINESS_START_HOUR) {
            cursor = cursor.withHour(BUSINESS_START_HOUR).withMinute(0);
        }
        if (cursor.getHour() >= BUSINESS_END_HOUR) {
            cursor = cursor.plusDays(1).withHour(BUSINESS_START_HOUR).withMinute(0);
            while (cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
                cursor = cursor.plusDays(1).withHour(BUSINESS_START_HOUR).withMinute(0);
            }
        }
        return cursor;
    }

    @Scheduled(fixedDelay = 60000L)
    @Transactional
    public void markOverdueTickets() {
        Instant now = Instant.now();
        List<MaintenanceTicket> overdue = tickets.findByStatusInAndSlaDueAtBefore(
                List.of(MaintenanceTicketStatus.OPEN, MaintenanceTicketStatus.IN_PROGRESS), now);
        for (MaintenanceTicket ticket : overdue) {
            ticket.overdue = true;
            ticket.slaBreached = true;
        }
        tickets.saveAll(overdue);
    }
}
