package com.eagle.app.service;

import com.eagle.app.model.Asset;
import com.eagle.app.model.AssetLifecycleState;
import com.eagle.app.model.AssetType;
import com.eagle.app.model.Inspection;
import com.eagle.app.repository.AssetRepository;
import com.eagle.app.repository.InspectionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class InspectionCadenceService {
    private final AssetRepository assets;
    private final InspectionRepository inspections;

    public InspectionCadenceService(AssetRepository assets, InspectionRepository inspections) {
        this.assets = assets;
        this.inspections = inspections;
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void generateCriticalAssetInspections() {
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Asset> critical = assets.findByAssetTypeInAndLifecycleStateNot(
                List.of(AssetType.PROJECTOR, AssetType.HVAC),
                AssetLifecycleState.RETIRED
        );
        for (Asset asset : critical) {
            boolean existsRecent = inspections.existsByRoomNumberAndInspectionTimeAfter(asset.room.number, since);
            if (existsRecent) continue;
            Inspection inspection = new Inspection();
            inspection.roomNumber = asset.room.number;
            inspection.inspectionTime = Instant.now();
            inspection.outcome = "SCHEDULED: 30-day critical asset inspection for " + asset.tag;
            inspections.save(inspection);
        }
    }
}
