package com.alhrb.forestry.model;

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

    @Column(name = "plot_number", nullable = false, unique = true)
    private String plotNumber;

    @Column(name = "forestry_name")
    private String forestryName;

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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (geometry != null) {
            // Приблизительный перевод градусов в метры (на экваторе)
            areaM2 = geometry.getArea() * 111319.9 * 111319.9;
        }
    }
}