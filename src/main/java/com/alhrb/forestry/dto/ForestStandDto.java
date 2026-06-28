package com.alhrb.forestry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO для работы с выделами (forest_stand)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForestStandDto {

    /**
     * Уникальный идентификатор выдела
     */
    private Long id;

    /**
     * ID квартала (forestry_units.id)
     */
    private Long forestryUnitId;

    /**
     * Номер выдела в пределах квартала
     */
    private String standNumber;

    /**
     * Полный номер выдела: лесничество_квартал_выдел
     */
    private String fullNumber;

    /**
     * Краткое название
     */
    private String name;

    /**
     * Описание
     */
    private String description;

    /**
     * Состав древостоя (6ОС4Б)
     */
    private String composition;

    /**
     * Преобладающая порода (ОС, Б, С, Е)
     */
    private String predominantSpecies;

    /**
     * Возраст древостоя в годах
     */
    private Integer age;

    /**
     * Средняя высота в метрах
     */
    private BigDecimal height;

    /**
     * Средний диаметр в см
     */
    private BigDecimal diameter;

    /**
     * Бонитет (I-V)
     */
    private String bonitet;

    /**
     * Тип леса
     */
    private String forestType;

    /**
     * Тип лесорастительных условий (А, В, С, Д)
     */
    private String tlu;

    /**
     * Полнота древостоя (0,1-1,0)
     */
    private String fullness;

    /**
     * Запас древесины на 1 га в м³
     */
    private BigDecimal stock;

    /**
     * Категория земель
     */
    private String category;

    /**
     * Категория защитности
     */
    private String protectionCategory;

    /**
     * Группа лесов
     */
    private String groupType;

    /**
     * Особо защитный участок (ОЗУ)
     */
    private String ozu;

    /**
     * Год актуальности таксационных данных
     */
    private Integer relevanceYear;

    /**
     * Площадь в гектарах
     */
    private BigDecimal areaHa;

    /**
     * Площадь в квадратных метрах (рассчитывается)
     */
    private BigDecimal areaM2;

    /**
     * Признак верификации
     */
    private Boolean verified;

    /**
     * Координаты полигона (для ввода)
     */
    private List<CoordinateDto> coordinates;

    /**
     * Геометрия полигона (для отображения)
     */
    private Polygon geometry;

    /**
     * Путь территории (для отображения в таблице)
     */
    private String territoryPath;
}