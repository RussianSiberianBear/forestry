package com.alhrb.forestry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "region")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", unique = true, length = 20)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @OneToMany(mappedBy = "region")
    private List<MunicipalDistrict> municipalDistricts;

    // ===== СИСТЕМА КООРДИНАТ =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinate_system_id")
    private CoordinateSystem coordinateSystem;

    // ===== КООРДИНАТЫ ЦЕНТРА =====
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
