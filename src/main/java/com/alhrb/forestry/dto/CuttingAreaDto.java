package com.alhrb.forestry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CuttingAreaDto {

    // ===== ТЕРРИТОРИЯ (вместо quarterId) =====
    @NotNull(message = "Квартал обязателен")
    private Long territoryUnitId;  // ← вместо quarterId

    // ===== НОМЕР ДЕЛЯНЫ В КВАРТАЛЕ =====
    @NotBlank(message = "Номер деляны в квартале обязателен")
    private String numberInQuarter;

    // ===== ВЫДЕЛЫ =====
    private String forestStand;

    // ===== ОПИСАНИЕ =====
    private String description;

    // ===== КООРДИНАТЫ =====
    @NotNull(message = "Координаты обязательны")
    @Size(min = 3, message = "Необходимо минимум 3 точки для полигона")
    private List<CoordinateDto> coordinates;

    // ===== ДОПОЛНИТЕЛЬНО =====
    private Integer yearOfCut;
    private String cutType;
}