package com.alhrb.forestry.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_ui_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUISettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    // ===== ТЕРРИТОРИЯ (новая структура) =====
    @Column(name = "territory_unit_id")
    private Long territoryUnitId;

    @Column(name = "territory_type")
    private String territoryType;

    // ===== КАРТА =====
    @Column(name = "center_lat")
    private Double centerLat;

    @Column(name = "center_lng")
    private Double centerLng;

    @Column(name = "zoom")
    private Integer zoom;

    // ===== ФИЛЬТРЫ ПО РУБКЕ =====
    @Column(name = "cut_type")
    private String cutType;

    @Column(name = "year_of_cut")
    private Integer yearOfCut;
}