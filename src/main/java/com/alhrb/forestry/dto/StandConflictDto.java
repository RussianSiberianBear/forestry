package com.alhrb.forestry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для отображения конфликтов (пересечений) выделов
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandConflictDto {

    /**
     * Номер первого выдела
     */
    private String stand1Number;

    /**
     * Номер второго выдела
     */
    private String stand2Number;

    /**
     * Площадь пересечения в м²
     */
    private BigDecimal overlapArea;

    /**
     * Серьезность конфликта
     */
    private String severity; // CRITICAL, WARNING, OK
}