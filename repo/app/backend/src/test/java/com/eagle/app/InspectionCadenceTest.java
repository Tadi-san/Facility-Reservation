package com.eagle.app;

import com.eagle.app.model.Asset;
import com.eagle.app.model.AssetType;
import com.eagle.app.model.Location;
import com.eagle.app.model.Room;
import com.eagle.app.model.RoomType;
import com.eagle.app.repository.AssetRepository;
import com.eagle.app.repository.InspectionRepository;
import com.eagle.app.repository.LocationRepository;
import com.eagle.app.repository.RoomRepository;
import com.eagle.app.repository.RoomTypeRepository;
import com.eagle.app.service.InspectionCadenceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InspectionCadenceTest {
    @Autowired
    private InspectionCadenceService cadenceService;
    @Autowired
    private InspectionRepository inspections;
    @Autowired
    private AssetRepository assets;
    @Autowired
    private LocationRepository locations;
    @Autowired
    private RoomTypeRepository roomTypes;
    @Autowired
    private RoomRepository rooms;

    @Test
    void generatesDefaultThirtyDayInspectionForCriticalAssets() {
        Location location = new Location();
        location.code = "TEST";
        location.name = "Test Site";
        locations.save(location);

        RoomType roomType = new RoomType();
        roomType.name = "Meeting";
        roomTypes.save(roomType);

        Room room = new Room();
        room.location = location;
        room.roomType = roomType;
        room.number = "T-100";
        room.capacity = 6;
        rooms.save(room);

        Asset asset = new Asset();
        asset.room = room;
        asset.tag = "PROJ-T-100";
        asset.name = "Projector T-100";
        asset.assetType = AssetType.PROJECTOR;
        assets.save(asset);

        long before = inspections.count();
        cadenceService.generateCriticalAssetInspections();
        long after = inspections.count();
        Assertions.assertTrue(after > before);
    }
}
