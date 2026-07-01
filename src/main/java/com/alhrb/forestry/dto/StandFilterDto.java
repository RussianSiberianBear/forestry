package com.alhrb.forestry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для фильтрации выделов
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandFilterDto {

    /**
     * ID территории (регион/район/лесничество и т.д.)
     */
    private Long territoryUnitId;

    /**
     * Тип территории (REGION, MUNICIPAL_DISTRICT, FORESTRY, etc.)
     */
    private String territoryType;

    /**
     * Номер квартала
     */
    private String quarterNumber;

    /**
     * Номер выдела
     */
    private String standNumber;

    /**
     * Год актуальности
     */
    private Integer relevanceYear;

    /**
     * Тип покрытия
     */
    private String coverType;

    /**
     * Только верифицированные
     */
    private Boolean verifiedOnly;
}