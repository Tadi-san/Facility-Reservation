package com.eagle.app.service;

import com.eagle.app.model.*;
import com.eagle.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
public class AssetService {
    private final AssetRepository assets;
    private final RoomRepository rooms;
    private final WarningAlternativeService warnings;

    public AssetService(AssetRepository assets, RoomRepository rooms, WarningAlternativeService warnings) {
        this.assets = assets;
        this.rooms = rooms;
        this.warnings = warnings;
    }

    @Transactional
    public Asset updateOperationalStatus(Long id, AssetOperationalStatus status) {
        Asset a = assets.findById(id).orElseThrow(() -> new IllegalArgumentException("Asset not found"));
        a.operationalStatus = status;
        Asset saved = assets.save(a);
        applyRoomDependence(saved);
        return saved;
    }

    @Transactional
    public Asset transitionLifecycle(Long id, AssetLifecycleState target) {
        Asset a = assets.findById(id).orElseThrow(() -> new IllegalArgumentException("Asset not found"));
        if (!isValid(a.lifecycleState, target)) {
            throw new IllegalArgumentException("Invalid asset lifecycle transition: " + a.lifecycleState + " -> " + target);
        }
        a.lifecycleState = target;
        Asset saved = assets.save(a);
        applyRoomDependence(saved);
        return saved;
    }

    private boolean isValid(AssetLifecycleState current, AssetLifecycleState target) {
        if (current == target) return true;
        return switch (current) {
            case NORMAL -> target == AssetLifecycleState.UNDER_REPAIR;
            case UNDER_REPAIR -> Set.of(AssetLifecycleState.NORMAL, AssetLifecycleState.RETIRED).contains(target);
            case RETIRED -> false;
        };
    }

    private void applyRoomDependence(Asset asset) {
        if (!asset.isEssential()) return;
        Room room = asset.room;
        boolean problematic = asset.operationalStatus == AssetOperationalStatus.OUT_OF_SERVICE || asset.lifecycleState == AssetLifecycleState.UNDER_REPAIR;
        if (problematic) {
            room.reservable = false;
            room.reservationWarning = warnings.buildWarningAndAlternatives(room);
        } else {
            room.reservable = true;
            room.reservationWarning = null;
        }
        rooms.save(room);
    }
}
