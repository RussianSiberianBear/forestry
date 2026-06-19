package com.alhrb.forestry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PlotDto {

    @NotBlank(message = "Номер деляны обязателен")
    private String plotNumber;

    private String forestryName;
    private String description;

    @NotNull(message = "Координаты обязательны")
    @Size(min = 4, message = "Необходимо минимум 4 точки для полигона")
    private List<CoordinateDto> coordinates;
}
