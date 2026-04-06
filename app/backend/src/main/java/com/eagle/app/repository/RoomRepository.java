package com.eagle.app.repository;

import com.eagle.app.model.Room;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByLocation_IdAndNumber(Long locationId, String number);
    List<Room> findByLocation_IdAndRoomType_IdAndReservableTrueAndIdNot(Long locationId, Long roomTypeId, Long excludedRoomId);
}
