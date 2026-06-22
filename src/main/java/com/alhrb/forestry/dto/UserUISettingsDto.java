package com.alhrb.forestry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUISettingsDto {
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
