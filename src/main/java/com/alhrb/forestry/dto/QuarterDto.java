package com.alhrb.forestry.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuarterDto {
    private Long id;
    private Integer number;
    private String name;
    private Double areaHa;
    private String description;
    private Long districtForestryId;

    // ===== ГЕОМЕТРИЯ КВАРТАЛА =====
    private List<CoordinateDto> coordinates; // ← Границы квартала

    // ===== СПИСОК ДЕЛЯН =====
    private List<PlotDto> plots;
}
