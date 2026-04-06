package com.eagle.app.controller;

import com.eagle.app.dto.ReservationRequest;
import com.eagle.app.dto.ReservationResponse;
import com.eagle.app.model.Reservation;
import com.eagle.app.model.ReservationStatus;
import com.eagle.app.model.RoleName;
import com.eagle.app.model.User;
import com.eagle.app.repository.ReservationRepository;
import com.eagle.app.repository.RoomRepository;
import com.eagle.app.repository.UserRepository;
import com.eagle.app.service.AuditLogService;
import com.eagle.app.service.CurrentUserService;
import com.eagle.app.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    private final ReservationRepository reservations;
    private final ReservationService service;
    private final UserRepository users;
    private final RoomRepository rooms;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;

    public ReservationController(ReservationRepository reservations,
                                 ReservationService service,
                                 UserRepository users,
                                 RoomRepository rooms,
                                 CurrentUserService currentUserService,
                                 AuditLogService auditLogService) {
        this.reservations = reservations;
        this.service = service;
        this.users = users;
        this.rooms = rooms;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT','ADMIN','OPS')")
    public Page<ReservationResponse> list(Pageable pageable) {
        return reservations.findAll(pageable).map(ReservationResponse::from);
    }

    @GetMapping("/mine")
    public Page<ReservationResponse> mine(Pageable pageable) {
        User current = currentUserService.requireCurrentUser();
        List<ReservationResponse> all = reservations.findByRequester_UsernameOrderByStartTimeDesc(current.username)
                .stream()
                .map(ReservationResponse::from)
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start >= all.size() ? List.of() : all.subList(start, end), pageable, all.size());
    }

    @PostMapping
    public ReservationResponse create(@Valid @RequestBody ReservationRequest req) {
        User current = currentUserService.requireCurrentUser();
        Reservation r = new Reservation();
        if (req.requesterId() != null) {
            boolean allowed = currentUserService.hasAnyRole(current, RoleName.ADMIN, RoleName.AGENT);
            if (!allowed) {
                throw new IllegalArgumentException("Only AGENT/ADMIN can create reservations for another user");
            }
            r.requester = users.findById(req.requesterId()).orElseThrow(() -> new IllegalArgumentException("Requester not found"));
        } else {
            r.requester = current;
        }
        r.room = rooms.findById(req.roomId()).orElseThrow(() -> new IllegalArgumentException("Room not found"));
        if (req.endTime().isBefore(req.startTime()) || req.endTime().equals(req.startTime())) {
            throw new IllegalArgumentException("Reservation end must be after start");
        }
        r.startTime = req.startTime();
        r.endTime = req.endTime();
        r.status = req.status() == null ? ReservationStatus.PENDING : req.status();
        Reservation saved = service.createReservation(r);
        auditLogService.log(current, "RESERVATION_CREATED", "Reservation", String.valueOf(saved.id), "Reservation created for room " + saved.room.number);
        return ReservationResponse.from(saved);
    }
}
