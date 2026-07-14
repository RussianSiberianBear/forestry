package com.alhrb.forestry.dto;

import lombok.Data;

@Data
public class ForestryUnitDto {
    private Long id;
    private Long parentId;
    private String name;
    private String type;
    private String number;
    private String accountNumber;
    private String parentName;
    private Double centerLat;
    private Double centerLng;
    private Integer zoom;
}
