package com.alhrb.forestry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_ui_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUISettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    // ===== ИЕРАРХИЯ =====
    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "municipal_district_id")
    private Long municipalDistrictId;

    @Column(name = "forestry_id")
    private Long forestryId;

    @Column(name = "district_forestry_id")
    private Long districtForestryId;

    @Column(name = "technical_unit_id")
    private Long technicalUnitId;

    @Column(name = "quarter_id")
    private Long quarterId;

    // ===== КООРДИНАТЫ КАРТЫ =====
    @Column(name = "center_lat")
    private Double centerLat;

    @Column(name = "center_lng")
    private Double centerLng;

    @Column(name = "zoom")
    private Integer zoom;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
