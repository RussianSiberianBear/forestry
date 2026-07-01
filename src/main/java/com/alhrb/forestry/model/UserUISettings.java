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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "territory_unit_id")
    private Long territoryUnitId;

    @Column(name = "territory_type")
    private String territoryType;

    @Column(name = "forestry_unit_id")
    private Long forestryUnitId;

    @Column(name = "forestry_type")
    private String ForestryType;

    @Column(name = "center_lat")
    private Double centerLat;

    @Column(name = "center_lng")
    private Double centerLng;

    @Column(name = "zoom")
    private Integer zoom;

    @Column(name = "cut_type")
    private String cutType;

    @Column(name = "year_of_cut")
    private Integer yearOfCut;

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