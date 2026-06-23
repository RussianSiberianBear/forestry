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

    // ===== ПРЯМЫЕ ССЫЛКИ НА ВСЕ УРОВНИ =====
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipal_district_id")
    private MunicipalDistrict municipalDistrict;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forestry_id")
    private Forestry forestry;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_forestry_id")
    private DistrictForestry districtForestry;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technical_unit_id")
    private TechnicalUnit technicalUnit;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quarter_id")
    private Quarter quarter;

    // ===== НОМЕР ДЕЛЯНЫ =====
    @Column(name = "number_in_quarter", nullable = false, length = 50)
    private String numberInQuarter;

    @Column(name = "full_number", length = 300, unique = true)
    private String fullNumber;

    @Column(name = "plots", length = 200)
    private String plots;

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

    // УДАЛЯЕМ area_m2 — больше не нужен, используем area_ha из БД
    // @Column(name = "area_m2")
    // private Double areaM2;

    @Column(name = "area_ha")
    private Double areaHa;  // ← НОВОЕ ПОЛЕ

    @Column(name = "year_of_cut")
    private Integer yearOfCut;

    @Column(name = "cut_type", length = 50)
    private String cutType;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        // ===== УБИРАЕМ РУЧНОЙ РАСЧЁТ ПЛОЩАДИ =====
        // Теперь площадь считается триггером в БД
        // area_ha автоматически заполняется при INSERT/UPDATE

        // Проверка внутри квартала
        if (quarter != null && quarter.getGeometry() != null && geometry != null) {
            if (!quarter.getGeometry().contains(geometry)) {
                throw new IllegalStateException(
                        String.format("❌ Деляна '%s' выходит за границы квартала %d!",
                                numberInQuarter, quarter.getNumber())
                );
            }
        }

        // Формируем полный номер
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
            if (technicalUnit != null && !technicalUnit.getIsMain()) {
                sb.append(technicalUnit.getName()).append("/");
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

    @Override
    public String toString() {
        return fullNumber != null ? fullNumber : (numberInQuarter != null ? "Дел." + numberInQuarter : "Новая деляна");
    }
}
