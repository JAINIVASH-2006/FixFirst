package com.fixfirst;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "campus_assets", uniqueConstraints = @UniqueConstraint(name = "uk_asset_identifier", columnNames = "assetIdentifier"))
public class CampusAsset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 80) public String assetIdentifier;
    @Column(nullable = false, length = 120) public String location;
    @Column(nullable = false, length = 80) public String category;
    @Column(precision = 12, scale = 2) public BigDecimal estimatedReplacementCost;
    protected CampusAsset() {}
    public CampusAsset(String assetIdentifier, String location, String category, BigDecimal cost) {
        this.assetIdentifier = assetIdentifier; this.location = location; this.category = category;
        this.estimatedReplacementCost = cost;
    }
}