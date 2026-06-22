package com.alhrb.forestry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forestry")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Forestry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name; // Например: "Емельяновское лесничество"

    @Column(name = "code", length = 20)
    private String code; // Код лесничества

    @Column(name = "description", length = 500)
    private String description;

    // ===== ПРИВЯЗКА К РАЙОНУ =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipal_district_id")
    private MunicipalDistrict municipalDistrict;

    // ===== ПРИВЯЗКА К РЕГИОНУ (дублируем для быстрого доступа) =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @OneToMany(mappedBy = "forestry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DistrictForestry> districtForestries = new ArrayList<>();
}
