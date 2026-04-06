package com.eagle.app.dto;

import com.eagle.app.model.Room;

public record RoomBrowserResponse(Long id, String location, String roomNumber, Integer capacity, boolean reservable,
                                  String warning, String roomType, boolean available, String availabilityMessage) {
    public static RoomBrowserResponse from(Room room) {
        return new RoomBrowserResponse(room.id, room.location.name, room.number, room.capacity, room.reservable,
                room.reservationWarning, room.roomType.name, room.reservable,
                room.reservationWarning == null ? "Available for booking." : room.reservationWarning);
    }

    public static RoomBrowserResponse from(Room room, boolean available, String msg) {
        return new RoomBrowserResponse(room.id, room.location.name, room.number, room.capacity, room.reservable,
                room.reservationWarning, room.roomType.name, available, msg);
    }
}
