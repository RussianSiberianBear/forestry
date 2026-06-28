package com.alhrb.forestry.dto;

import lombok.Data;

@Data
public class ForestryUnitDto {
    private Long id;
    private Long parentId;
    private String name;
    private String type;
    private String code;
    private String number;
    private String parentName;
}
