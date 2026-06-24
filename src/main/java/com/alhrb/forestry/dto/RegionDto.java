package com.alhrb.forestry.dto;

import lombok.Data;

@Data
public class RegionDto {
    private Long id;
    private String name;
    private Double centerLat;
    private Double centerLng;
    private Integer zoom;
}
