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

    @Column(name = "number")
    private String number;                // внутренний номер

    @Column(name = "account_number")
    private String accountNumber;         // учетный номер по НПА

    @Column(columnDefinition = "geometry")
    private Geometry geometry;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    public String getFullPath() {
        StringBuilder sb = new StringBuilder();
        ForestryUnit current = this;
        List<String> names = new ArrayList<>();

        while (current != null) {
            names.add(0, current.getName());
            current = current.getParent();
        }

        return String.join(" / ", names);
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

    public boolean isDistrictForestry() {
        return type == ForestryUnitType.DISTRICT_FORESTRY;
    }

    public boolean isTechnicalUnit() {
        return type == ForestryUnitType.TECHNICAL_UNIT;
    }

    public boolean isQuarter() {
        return type == ForestryUnitType.QUARTER;
    }

    @Override
    public String toString() {
        return name;
    }
}