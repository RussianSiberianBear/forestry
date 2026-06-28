package com.alhrb.forestry.dto;

import lombok.Data;

@Data
public class TerritoryUnitDto {
    private Long id;
    private Long parentId;
    private String name;
    private String type;
    private String code;
    private String number;
    private String okato;
    private String oktmo;
    private Boolean isMain;
    private Double areaHa;
    private String parentName;
}
