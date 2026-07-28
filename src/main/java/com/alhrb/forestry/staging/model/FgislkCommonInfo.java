package com.alhrb.forestry.staging.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "fgislk_common_info",
        schema = "staging",
        indexes = {
                @Index(name = "idx_fgislk_region_code", columnList = "regionCode"),
                @Index(name = "idx_fgislk_region_name", columnList = "regionName"),
                @Index(name = "idx_fgislk_district_code", columnList = "forestDistrictCode"),
                @Index(name = "idx_fgislk_district_name", columnList = "forestDistrictName"),
                @Index(name = "idx_fgislk_quarter_code", columnList = "forestQuarterCode"),
                @Index(name = "idx_fgislk_plot_code", columnList = "forestPlotCode")
        }
)
public class FgislkCommonInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(name = "region_code", length = 50)
    private String regionCode;

    @Column(name = "region_name", length = 255)
    private String regionName;

    @Column(name = "forest_district_code", length = 50)
    private String forestDistrictCode;

    @Column(name = "forest_district_name", length = 255)
    private String forestDistrictName;

    @Column(name = "forest_quarter_code", length = 50)
    private String forestQuarterCode;

    @Column(name = "forest_plot_code", length = 50)
    private String forestPlotCode;

    @Column(name = "forest_plot_area", precision = 15, scale = 4)
    private BigDecimal forestPlotArea;

    @Column(name = "forest_plot_characteristic", columnDefinition = "TEXT")
    private String forestPlotCharacteristic;

    @Column(name = "forest_type", length = 100)
    private String forestType;

    @Column(name = "dominant_species", length = 50)
    private String dominantSpecies;

    @Column(name = "age_class", length = 50)
    private String ageClass;

    @Column(name = "forest_group", length = 100)
    private String forestGroup;

    @Column(name = "forest_category", length = 100)
    private String forestCategory;

    @Column(name = "protection_category", length = 100)
    private String protectionCategory;

    @Column(name = "purpose", length = 255)
    private String purpose;

    @Column(name = "inventory_date")
    private LocalDate inventoryDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}