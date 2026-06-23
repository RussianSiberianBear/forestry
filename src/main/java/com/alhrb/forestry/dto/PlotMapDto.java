package com.alhrb.forestry.dto;

import lombok.Data;

@Data
public class PlotMapDto {
    private Long id;
    private String fullNumber;
    private String numberInQuarter;
    private Integer quarterNumber;
    private String forestryName;
    private Boolean verified;
    private Double areaHa;        // ← площадь в гектарах (из БД)
    private Double areaM2;        // ← оставляем для обратной совместимости (можно удалить)
    private String geometryGeoJson;
}
