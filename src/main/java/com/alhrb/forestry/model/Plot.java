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

    // ===== ПРЯМЫЕ ССЫЛКИ НА ВСЕ УРОВНИ ИЕРАРХИИ =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipal_district_id")
    private MunicipalDistrict municipalDistrict;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forestry_id")
    private Forestry forestry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_forestry_id")
    private DistrictForestry districtForestry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quarter_id")
    private Quarter quarter;

    // ===== НОМЕР ДЕЛЯНЫ (ВВОДИТСЯ ВРУЧНУЮ!) =====
    @Column(name = "number_in_quarter", nullable = false, length = 50)
    private String numberInQuarter;

    @Column(name = "full_number", length = 200, unique = true)
    private String fullNumber;

    // ===== ВЫДЕЛЫ (НОВОЕ ПОЛЕ!) =====
    @Column(name = "plots", length = 200)
    private String plots; // Например: "1, 2, 3-5, 7"

    // ===== ОПИСАНИЕ =====
    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    // ===== ГЕОМЕТРИЯ =====
    @Column(name = "geometry", columnDefinition = "geometry(Polygon,4326)")
    private Polygon geometry;

    // ===== СТАТУСЫ =====
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "verified")
    private Boolean verified = false;

    @Column(name = "area_m2")
    private Double areaM2;

    // ===== ДОПОЛНИТЕЛЬНО =====
    @Column(name = "year_of_cut")
    private Integer yearOfCut;

    @Column(name = "cut_type", length = 50)
    private String cutType;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (geometry != null) {
            areaM2 = geometry.getArea() * 111319.9 * 111319.9;
        }

        // Проверка: деляна должна быть ВНУТРИ квартала
        if (quarter != null && quarter.getGeometry() != null && geometry != null) {
            if (!quarter.getGeometry().contains(geometry)) {
                throw new IllegalStateException(
                        String.format("❌ Деляна '%s' выходит за границы квартала %d!",
                                numberInQuarter, quarter.getNumber())
                );
            }
        }

        // Формируем полный номер из иерархии
        if (fullNumber == null) {
            StringBuilder sb = new StringBuilder();

            if (region != null) {
                sb.append(region.getName()).append("/");
            }
            if (municipalDistrict != null) {
                sb.append(municipalDistrict.getName()).append("/");
            }
            if (forestry != null) {
                sb.append(forestry.getName()).append("/");
            }
            if (districtForestry != null) {
                sb.append(districtForestry.getName()).append("/");
            }
            if (quarter != null) {
                sb.append("Кв.").append(quarter.getNumber()).append("/");
            }
            if (numberInQuarter != null && !numberInQuarter.isEmpty()) {
                sb.append("Дел.").append(numberInQuarter);
            }

            fullNumber = sb.toString();
        }
    }
}
