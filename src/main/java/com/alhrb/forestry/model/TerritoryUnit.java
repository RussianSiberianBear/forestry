package com.alhrb.forestry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "territory_units")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TerritoryUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TerritoryUnit parent;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TerritoryType type;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TerritoryUnit> children = new ArrayList<>();

    @Column(columnDefinition = "geometry")
    private Geometry geometry;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "code")
    private String code;

    @Column(name = "is_main")
    private Boolean isMain = false;

    @Column(name = "area_ha")
    private Double areaHa;

    @Column(name = "number")
    private String number; // для кварталов

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    public String getFullPath() {
        StringBuilder sb = new StringBuilder();
        TerritoryUnit current = this;
        List<String> names = new ArrayList<>();

        while (current != null) {
            names.add(0, current.getName());
            current = current.getParent();
        }

        return String.join(" / ", names);
    }

    public TerritoryUnit getRoot() {
        TerritoryUnit current = this;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    public List<TerritoryUnit> getPathToRoot() {
        List<TerritoryUnit> path = new ArrayList<>();
        TerritoryUnit current = this;
        while (current != null) {
            path.add(0, current);
            current = current.getParent();
        }
        return path;
    }

    // ===== ПРОВЕРКИ ТИПА =====
    public boolean isFederalDistrict() {
        return type == TerritoryType.FEDERAL_DISTRICT;
    }

    public boolean isRegion() {
        return type == TerritoryType.REGION;
    }

    public boolean isMunicipalDistrict() {
        return type == TerritoryType.MUNICIPAL_DISTRICT;
    }

    public boolean isForestry() {
        return type == TerritoryType.FORESTRY;
    }

    public boolean isDistrictForestry() {
        return type == TerritoryType.DISTRICT_FORESTRY;
    }

    public boolean isTechnicalUnit() {
        return type == TerritoryType.TECHNICAL_UNIT;
    }

    public boolean isQuarter() {
        return type == TerritoryType.QUARTER;
    }
}
