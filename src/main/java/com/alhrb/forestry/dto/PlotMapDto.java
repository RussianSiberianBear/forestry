package com.alhrb.forestry.dto;

import lombok.Data;

@Data
public class PlotMapDto {
    private Long id;
    private String fullNumber;
    private String numberInQuarter;
    private String quarterNumber;
    private String forestryName;
    private Boolean verified;
    private Double areaHa;
    private Double areaM2;
    private String geometryGeoJson;
    private String cutType;
    private Integer yearOfCut;
}
