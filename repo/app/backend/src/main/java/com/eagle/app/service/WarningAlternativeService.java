package com.eagle.app.service;

import com.eagle.app.model.*;
import com.eagle.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class WarningAlternativeService {
    private final RoomRepository rooms;

    public WarningAlternativeService(RoomRepository rooms) {
        this.rooms = rooms;
    }

    public String buildWarningAndAlternatives(Room impacted) {
        List<Room> alternatives = rooms.findByLocation_IdAndRoomType_IdAndReservableTrueAndIdNot(
                impacted.location.id, impacted.roomType.id, impacted.id);
        if (alternatives.isEmpty()) {
            return "Essential asset is out of service. Room marked non-reservable. No alternatives found.";
        }
        String joined = alternatives.stream().map(r -> r.number).reduce((a,b) -> a + ", " + b).orElse("none");
        return "Essential asset is out of service. Room marked non-reservable. Alternatives: " + joined;
    }
}
