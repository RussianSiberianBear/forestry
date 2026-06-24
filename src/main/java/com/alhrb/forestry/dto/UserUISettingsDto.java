package com.alhrb.forestry.dto;

import lombok.Data;

@Data
public class UserUISettingsDto {

    private Long id;
    private Long userId;
    private Long regionId;
    private Long municipalDistrictId;
    private Long forestryId;
    private Long districtForestryId;
    private Long technicalUnitId;
    private Long quarterId;
    private Double centerLat;
    private Double centerLng;
    private Integer zoom;
}