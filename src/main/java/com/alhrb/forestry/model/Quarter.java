package com.alhrb.forestry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quarter")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quarter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "area_ha")
    private Double areaHa;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "geometry", columnDefinition = "geometry(Polygon,4326)")
    private Polygon geometry;

    // ===== ПРИВЯЗКА К ТЕХНИЧЕСКОМУ УЧАСТКУ =====
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technical_unit_id")
    private TechnicalUnit technicalUnit;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_forestry_id")
    private DistrictForestry districtForestry; // ← дублируем для быстрых запросов

    @JsonIgnore
    @OneToMany(mappedBy = "quarter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Plot> plots = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (geometry != null) {
            double areaM2 = geometry.getArea() * 111319.9 * 111319.9;
            this.areaHa = areaM2 / 10000;
        }
    }

    @Override
    public String toString() {
        String prefix = technicalUnit != null && !technicalUnit.getIsMain()
                ? technicalUnit.getName() + " / "
                : "";
        return prefix + "Кв. " + number + (name != null ? " (" + name + ")" : "");
    }
}
