package com.eagle.app.service;

import com.eagle.app.dto.RoomBrowserResponse;
import com.eagle.app.dto.RoomCreateRequest;
import com.eagle.app.model.*;
import com.eagle.app.repository.*;
import org.springframework.stereotype.Service;

@Service
public class FacilityAdminService {
    private final LocationRepository locations;
    private final RoomTypeRepository roomTypes;
    private final RoomRepository rooms;
    private final AssetRepository assets;

    public FacilityAdminService(LocationRepository locations, RoomTypeRepository roomTypes, RoomRepository rooms, AssetRepository assets) {
        this.locations = locations;
        this.roomTypes = roomTypes;
        this.rooms = rooms;
        this.assets = assets;
    }

    public RoomBrowserResponse createRoom(RoomCreateRequest req) {
        Location location = locations.findByCode(req.locationCode().trim()).orElseGet(() -> {
            Location l = new Location();
            l.code = req.locationCode().trim().toUpperCase();
            l.name = req.locationName().trim();
            l.address = req.locationAddress();
            return locations.save(l);
        });

        RoomType type = roomTypes.findByName(req.roomTypeName().trim()).orElseGet(() -> {
            RoomType t = new RoomType();
            t.name = req.roomTypeName().trim();
            t.description = req.roomTypeName().trim() + " workspace";
            return roomTypes.save(t);
        });

        String number = req.roomNumber().trim().toUpperCase();
        if (rooms.existsByLocation_IdAndNumber(location.id, number)) {
            throw new IllegalArgumentException("A room with that number already exists in the selected location");
        }

        Room room = new Room();
        room.location = location;
        room.roomType = type;
        room.number = number;
        room.floorNumber = req.floorNumber();
        room.capacity = req.capacity();
        room.reservable = true;
        room = rooms.save(room);

        if (req.includeProjector()) createAssetIfMissing(room, AssetType.PROJECTOR);
        if (req.includeHvac()) createAssetIfMissing(room, AssetType.HVAC);

        return RoomBrowserResponse.from(room);
    }

    private void createAssetIfMissing(Room room, AssetType type) {
        String tag = (type == AssetType.PROJECTOR ? "PROJ-" : "HVAC-") + room.number;
        if (assets.existsByTag(tag)) return;
        Asset a = new Asset();
        a.room = room;
        a.tag = tag;
        a.name = type.name() + " " + room.number;
        a.assetType = type;
        assets.save(a);
    }
}
