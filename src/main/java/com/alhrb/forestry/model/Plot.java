package com.alhrb.forestry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Polygon;

import java.time.LocalDateTime;

@Entity
@Table(name = "forest_plot")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== НОВАЯ СВЯЗЬ (вместо всех старых) =====
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_unit_id", nullable = false)
    private TerritoryUnit territoryUnit;

    // ===== ОСТАЛЬНЫЕ ПОЛЯ ОСТАЮТСЯ =====
    @Column(name = "number_in_quarter", nullable = false, length = 50)
    private String numberInQuarter;

    @Column(name = "full_number", length = 300, unique = true)
    private String fullNumber;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "geometry", columnDefinition = "geometry(Polygon,4326)")
    private Polygon geometry;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "area_m2")
    private Double areaM2;

    @Column(name = "year_of_cut")
    private Integer yearOfCut;

    @Column(name = "cut_type", length = 50)
    private String cutType;

    @Column(name = "plots", length = 200)
    private String plots;

    @Column(name = "area_ha")
    private Double areaHa;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (fullNumber == null && territoryUnit != null) {
            fullNumber = territoryUnit.getFullPath() + " / Дел." + numberInQuarter;
        }
    }

    @Override
    public String toString() {
        return fullNumber != null ? fullNumber : (numberInQuarter != null ? "Дел." + numberInQuarter : "Новая деляна");
    }
}
