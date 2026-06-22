package com.alhrb.forestry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlotMapDto {
    private Long id;
    private String fullNumber;
    private String numberInQuarter;
    private String geometryGeoJson;  // ← GeoJSON
    private Boolean verified;
    private Double areaM2;
    private String forestryName;     // ← только имя лесничества, не вся сущность
}
