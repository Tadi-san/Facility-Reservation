package com.eagle.app.controller;

import com.eagle.app.dto.RoomBrowserResponse;
import com.eagle.app.model.Room;
import com.eagle.app.repository.ReservationRepository;
import com.eagle.app.repository.RoomRepository;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/catalog")
@PreAuthorize("isAuthenticated()")
public class CatalogController {
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public CatalogController(RoomRepository rooms, ReservationRepository reservations) {
        this.rooms = rooms;
        this.reservations = reservations;
    }

    @GetMapping("/rooms")
    public Page<RoomBrowserResponse> rooms(@RequestParam(required = false) String location,
                                           @RequestParam(required = false) String roomType,
                                           @RequestParam(required = false) Integer minCapacity,
                                           @RequestParam(required = false) Instant startTime,
                                           @RequestParam(required = false) Instant endTime,
                                           Pageable pageable) {
        List<RoomBrowserResponse> all = rooms.findAll().stream()
                .filter(r -> location == null || location.isBlank() || r.location.name.toLowerCase().contains(location.toLowerCase()))
                .filter(r -> roomType == null || roomType.isBlank() || r.roomType.name.toLowerCase().contains(roomType.toLowerCase()))
                .filter(r -> minCapacity == null || r.capacity >= minCapacity)
                .sorted(Comparator.comparing(r -> r.number))
                .map(r -> withAvailability(r, startTime, endTime))
                .toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<RoomBrowserResponse> content = start >= all.size() ? List.of() : all.subList(start, end);
        return new PageImpl<>(content, pageable, all.size());
    }

    @GetMapping("/locations")
    public List<String> locations() {
        return rooms.findAll().stream().map(r -> r.location.name).distinct().sorted().toList();
    }

    @GetMapping("/room-types")
    public List<String> roomTypes() {
        return rooms.findAll().stream().map(r -> r.roomType.name).distinct().sorted().toList();
    }

    private RoomBrowserResponse withAvailability(Room room, Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null) return RoomBrowserResponse.from(room);
        if (!room.reservable) return RoomBrowserResponse.from(room, false, room.reservationWarning == null ? "Room is non-reservable." : room.reservationWarning);
        boolean conflict = reservations.countConflicts(room.id, startTime, endTime) > 0;
        if (conflict) return RoomBrowserResponse.from(room, false, "Conflicts with an existing reservation in the selected minute range.");
        return RoomBrowserResponse.from(room, true, "Available for the selected date and time.");
    }
}
