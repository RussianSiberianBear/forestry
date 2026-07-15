package com.alhrb.forestry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Polygon;

import java.time.LocalDateTime;

@Entity
@Table(name = "cutting_area")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuttingArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== ССЫЛКА НА КВАРТАЛ =====
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forestry_unit_id", nullable = false)
    private ForestryUnit forestryUnit;

    // ===== ПОЛЯ ДЕЛЯНЫ =====
    @Column(name = "number_in_quarter", nullable = false, length = 50)
    private String numberInQuarter;

    @Column(name = "full_number", length = 300, unique = true)
    private String fullNumber;

    @Column(name = "forest_stand", length = 200)
    private String forestStand;

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

    @Column(name = "area_ha")
    private Double areaHa;

    @Column(name = "year_of_cut")
    private Integer yearOfCut;

    @Column(name = "cut_type", length = 50)
    private String cutType;

    @Column(name = "account_number")
    private String accountNumber;         // учетный номер по НПА

    // ===== ТРАНЗИТНОЕ ПОЛЕ ДЛЯ THYMELEAF =====
    @Transient
    private String territoryPath;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (fullNumber == null && forestryUnit != null) {
            fullNumber = forestryUnit.getFullPath() + " / Дел." + numberInQuarter;
        }
        accountNumber = getFullAccountNumber() + ":" + forestryUnit.getNumber() + ":ЛС" + numberInQuarter;
    }

    public String getFullAccountNumber() {
        ForestryUnit forestryUnitTmp = forestryUnit;

        while (forestryUnitTmp != null
                && !(forestryUnitTmp.isForestry() || forestryUnitTmp.isSubForestry())) {
            forestryUnitTmp = forestryUnitTmp.getParent();
        }

        if (forestryUnitTmp == null || forestryUnitTmp.getAccountNumber() == null) {
            return "";
        }
        return forestryUnitTmp.getAccountNumber();
    }

    public String getForestryPath() {
        if (forestryUnit != null) {
            return forestryUnit.getFullPath();
        }
        return null;
    }

    @Override
    public String toString() {
        return fullNumber != null ? fullNumber : (numberInQuarter != null ? "Дел." + numberInQuarter : "Новая деляна");
    }
}