package com.alhrb.forestry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forestry_units")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForestryUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private ForestryUnit parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ForestryUnit> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_units_id")
    @JsonIgnore
    private TerritoryUnit territoryUnit;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ForestryUnitType type;

    private String number;                // внутренний номер

    private Geometry geometry;

    private LocalDateTime createdAt;

    private Long coordinateSystemId;

    private Double centerLat;

    private Double centerLng;

    private Integer zoom;

    private String accountNumber;         // учетный номер по НПА

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    public String getFullPath() {
        ForestryUnit current = this;
        List<String> names = new ArrayList<>();

        while (current != null) {
            names.add(0, current.getName());
            current = current.getParent();
        }

        return String.join(" / ", names);
    }

    public void setAccountNumber(String accountNumber) {
    }

    public ForestryUnit getRoot() {
        ForestryUnit current = this;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    public List<ForestryUnit> getPathToRoot() {
        List<ForestryUnit> path = new ArrayList<>();
        ForestryUnit current = this;
        while (current != null) {
            path.add(0, current);
            current = current.getParent();
        }
        return path;
    }

    // ===== ПРОВЕРКИ ТИПА =====
    public boolean isForestry() {
        return type == ForestryUnitType.FORESTRY;
    }

    public boolean isSubForestry() {
        return type == ForestryUnitType.SUB_FORESTRY;
    }

    public boolean isTechnicalUnit() {
        return type == ForestryUnitType.TECHNICAL_UNIT;
    }

    public boolean isQuarter() {
        return type == ForestryUnitType.FOREST_QUARTER;
    }

    public boolean isForestStand() {
        return type == ForestryUnitType.FOREST_STAND;
    }

    public boolean isForestPlot() {
        return type == ForestryUnitType.FOREST_PLOT;
    }

    public boolean isCuttingArea() {
        return type == ForestryUnitType.CUTTING_AREA;
    }

    @Override
    public String toString() {
        return name;
    }
}