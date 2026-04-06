package com.eagle.app.dto;

import com.eagle.app.model.Asset;
import com.eagle.app.model.*;

public record AssetResponse(Long id, Long roomId, String roomNumber, String tag, String name, AssetType assetType,
                            AssetOperationalStatus operationalStatus, AssetLifecycleState lifecycleState, boolean essential) {
    public static AssetResponse from(Asset a) {
        return new AssetResponse(a.id, a.room.id, a.room.number, a.tag, a.name, a.assetType, a.operationalStatus, a.lifecycleState, a.isEssential());
    }
}
