package com.fixfirst;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusAssetRepository extends JpaRepository<CampusAsset, Long> {
    Optional<CampusAsset> findByAssetIdentifier(String assetIdentifier);
}