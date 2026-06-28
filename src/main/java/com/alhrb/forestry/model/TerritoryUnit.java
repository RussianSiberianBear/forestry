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
    @JsonIgnore
    private TerritoryUnit parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<TerritoryUnit> children = new ArrayList<>();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TerritoryType type;  // FEDERAL_DISTRICT, REGION, MUNICIPAL_DISTRICT

    @Column(name = "code")
    private String code;

    @Column(name = "okato")
    private String okato;

    @Column(name = "oktmo")
    private String oktmo;

    @Column(name = "number")
    private String number;

    @Column(name = "is_main")
    private Boolean isMain;

    @Column(name = "area_ha")
    private Double areaHa;

    @Column(columnDefinition = "geometry")
    private Geometry geometry;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

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

    public boolean isFederalDistrict() {
        return type == TerritoryType.FEDERAL_DISTRICT;
    }

    public boolean isRegion() {
        return type == TerritoryType.REGION;
    }

    public boolean isMunicipalDistrict() {
        return type == TerritoryType.MUNICIPAL_DISTRICT;
    }

    @Override
    public String toString() {
        return name;
    }

}