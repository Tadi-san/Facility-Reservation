package com.eagle.app.repository;

import com.eagle.app.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    boolean existsByTag(String tag);
    List<Asset> findByAssetTypeInAndLifecycleStateNot(Collection<AssetType> assetTypes, AssetLifecycleState excludedState);
}
