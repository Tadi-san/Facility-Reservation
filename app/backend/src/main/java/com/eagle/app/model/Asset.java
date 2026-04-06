package com.eagle.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "assets")
public class Asset extends SyncableEntity {
    @ManyToOne(optional = false)
    public Room room;

    @Column(nullable = false, unique = true)
    public String tag;

    @Column(nullable = false)
    public String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AssetType assetType = AssetType.GENERIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AssetOperationalStatus operationalStatus = AssetOperationalStatus.IN_SERVICE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AssetLifecycleState lifecycleState = AssetLifecycleState.NORMAL;

    public boolean isEssential() {
        return assetType == AssetType.PROJECTOR || assetType == AssetType.HVAC;
    }
}
