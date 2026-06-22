package com.alhrb.forestry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PlotDto {

    // ===== ИЕРАРХИЯ =====
    private Long regionId;
    private Long forestryId;
    private Long districtForestryId;
    private Long quarterId;

    // ===== НОМЕР ДЕЛЯНЫ (ВВОДИТСЯ ВРУЧНУЮ!) =====
    @NotBlank(message = "Номер деляны в квартале обязателен")
    private String numberInQuarter; // ← String! Например: "12", "12а", "12/1"

    private String fullNumber; // Полный номер (генерируется)

    // ===== ОПИСАНИЕ =====
    private String name;
    private String description;

    // ===== КООРДИНАТЫ =====
    @NotNull(message = "Координаты обязательны")
    @Size(min = 4, message = "Необходимо минимум 4 точки для полигона")
    private List<CoordinateDto> coordinates;

    // ===== ДОПОЛНИТЕЛЬНО =====
    private Integer yearOfCut;
    private String cutType;
}
