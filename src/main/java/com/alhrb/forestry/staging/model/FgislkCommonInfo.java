package com.alhrb.forestry.staging.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Сырой staging-слой полной выгрузки ФГИС ЛК.
 * <p>
 * Все 83 поля CSV сохраняются как строки. Это позволяет не терять
 * регистрационные и лесоустроительные номера, ведущие нули,
 * нестандартные значения и данные справочников.
 */
@Data
@NoArgsConstructor
@Entity
@Table(
        name = "fgislk_common_info",
        schema = "staging",
        indexes = {
                @Index(name = "idx_fgislk_user", columnList = "user_id"),
                @Index(name = "idx_fgislk_quarter_reg", columnList = "quarter_registration_number"),
                @Index(name = "idx_fgislk_quarter_fm", columnList = "quarter_forest_management_number"),
                @Index(name = "idx_fgislk_plot_reg", columnList = "plot_registration_number"),
                @Index(name = "idx_fgislk_plot_fm", columnList = "plot_forest_management_number")
        }
)
public class FgislkCommonInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 0: Код региона
     */
    @Column(name = "region_code", length = 500)
    private String regionCode;

    /**
     * 1: Наименование региона
     */
    @Column(name = "region_name", length = 500)
    private String regionName;

    /**
     * 2: Учетный номер лесничества
     */
    @Column(name = "forest_district_accounting_number", length = 500)
    private String forestDistrictAccountingNumber;

    /**
     * 3: Наименование лесничества
     */
    @Column(name = "forest_district_name", length = 500)
    private String forestDistrictName;

    /**
     * 4: Учетный номер участкового лесничества
     */
    @Column(name = "local_forest_district_accounting_number", length = 500)
    private String localForestDistrictAccountingNumber;

    /**
     * 5: Наименование участкового лесничества
     */
    @Column(name = "local_forest_district_name", length = 500)
    private String localForestDistrictName;

    /**
     * 6: Регистрационный номер квартала
     */
    @Column(name = "quarter_registration_number", length = 500)
    private String quarterRegistrationNumber;

    /**
     * 7: Лесоустроительный номер квартала
     */
    @Column(name = "quarter_forest_management_number", length = 500)
    private String quarterForestManagementNumber;

    /**
     * 8: Урочище
     */
    @Column(name = "tract", length = 500)
    private String tract;

    /**
     * 9: id выдела
     */
    @Column(name = "plot_id", length = 500)
    private String plotId;

    /**
     * 10: Регистрационный номер выдела
     */
    @Column(name = "plot_registration_number", length = 500)
    private String plotRegistrationNumber;

    /**
     * 11: Лесоустроительный номер выдела
     */
    @Column(name = "plot_forest_management_number", length = 500)
    private String plotForestManagementNumber;

    /**
     * 12: Статус выдела
     */
    @Column(name = "plot_status", length = 500)
    private String plotStatus;

    /**
     * 13: Площадь выдела
     */
    @Column(name = "plot_area", length = 500)
    private String plotArea;

    /**
     * 14: Общий запас лесных насаждений
     */
    @Column(name = "total_growing_stock", length = 500)
    private String totalGrowingStock;

    /**
     * 15: Код типа земель
     */
    @Column(name = "land_type_code", length = 500)
    private String landTypeCode;

    /**
     * 16: Наименование типа земель
     */
    @Column(name = "land_type_name", length = 500)
    private String landTypeName;

    /**
     * 17: Код вида лесных земель
     */
    @Column(name = "forest_land_type_code", length = 500)
    private String forestLandTypeCode;

    /**
     * 18: Наименование вида лесных земель
     */
    @Column(name = "forest_land_type_name", length = 500)
    private String forestLandTypeName;

    /**
     * 19: Код вида нелесных земель
     */
    @Column(name = "non_forest_land_type_code", length = 500)
    private String nonForestLandTypeCode;

    /**
     * 20: Наименование вида нелесных земель
     */
    @Column(name = "non_forest_land_type_name", length = 500)
    private String nonForestLandTypeName;

    /**
     * 21: Код типа леса
     */
    @Column(name = "forest_type_code", length = 500)
    private String forestTypeCode;

    /**
     * 22: Наименование типа леса
     */
    @Column(name = "forest_type_name", length = 500)
    private String forestTypeName;

    /**
     * 23: Наименование типа лесорастительных условий
     */
    @Column(name = "forest_site_conditions_type_name", length = 500)
    private String forestSiteConditionsTypeName;

    /**
     * 24: Общий запас сухостоя
     */
    @Column(name = "deadwood_stock", length = 500)
    private String deadwoodStock;

    /**
     * 25: Общий запас естественных редин
     */
    @Column(name = "natural_open_forest_stock", length = 500)
    private String naturalOpenForestStock;

    /**
     * 26: Общий запас единичных деревьев
     */
    @Column(name = "single_trees_stock", length = 500)
    private String singleTreesStock;

    /**
     * 27: Общий запас неликвидной древесины
     */
    @Column(name = "non_commercial_wood_stock", length = 500)
    private String nonCommercialWoodStock;

    /**
     * 28: Год создания лесных культур
     */
    @Column(name = "forest_plantation_creation_year", length = 500)
    private String forestPlantationCreationYear;

    /**
     * 29: Наименование состояния лесного насаждения
     */
    @Column(name = "forest_stand_condition_name", length = 500)
    private String forestStandConditionName;

    /**
     * 30: Код целевой породы
     */
    @Column(name = "target_species_code", length = 500)
    private String targetSpeciesCode;

    /**
     * 31: Целевая порода (справочник) НСИ groupsTreeSpecies
     */
    @Column(name = "target_species_nsi", length = 500)
    private String targetSpeciesNsi;

    /**
     * 32: Наименование целевой породы
     */
    @Column(name = "target_species_name", length = 500)
    private String targetSpeciesName;

    /**
     * 33: Наименование загрязнения радионуклидами земель, на которых расположены леса
     */
    @Column(name = "radionuclide_pollution_name", length = 1000)
    private String radionuclidePollutionName;

    /**
     * 34: Дата последнего лесоустройства
     */
    @Column(name = "last_forest_inventory_date", length = 500)
    private String lastForestInventoryDate;

    /**
     * 35: Наименование объекта лесного семеноводства
     */
    @Column(name = "forest_seed_production_object_name", length = 500)
    private String forestSeedProductionObjectName;

    /**
     * 36: Особо защитные участки
     */
    @Column(name = "special_protective_areas", length = 500)
    private String specialProtectiveAreas;

    /**
     * 37: Хозяйственная категория
     */
    @Column(name = "economic_category", length = 500)
    private String economicCategory;

    /**
     * 38: Категория защитности
     */
    @Column(name = "protection_category", length = 500)
    private String protectionCategory;

    /**
     * 39: Код категорий защитных лесов
     */
    @Column(name = "protective_forest_category_code", length = 500)
    private String protectiveForestCategoryCode;

    /**
     * 40: Наименование категории защитных лесов
     */
    @Column(name = "protective_forest_category_name", length = 500)
    private String protectiveForestCategoryName;

    /**
     * 41: Код подкатегорий защитных лесов
     */
    @Column(name = "protective_forest_subcategory_code", length = 500)
    private String protectiveForestSubcategoryCode;

    /**
     * 42: Наименование подкатегории защитных лесов
     */
    @Column(name = "protective_forest_subcategory_name", length = 500)
    private String protectiveForestSubcategoryName;

    /**
     * 43: Мнемоника, административный район
     */
    @Column(name = "administrative_district_mnemonic", length = 500)
    private String administrativeDistrictMnemonic;

    /**
     * 44: Административный район
     */
    @Column(name = "administrative_district_name", length = 500)
    private String administrativeDistrictName;

    /**
     * 45: Особенности выдела
     */
    @Column(name = "plot_features", length = 500)
    private String plotFeatures;

    /**
     * 46: Сведения о подсочке, осмолоподсочке
     */
    @Column(name = "tapping_information", length = 500)
    private String tappingInformation;

    /**
     * 47: Рекреационная характеристика
     */
    @Column(name = "recreational_characteristic", length = 500)
    private String recreationalCharacteristic;

    /**
     * 48: Селекционная оценка
     */
    @Column(name = "selection_assessment", length = 500)
    private String selectionAssessment;

    /**
     * 49: Запас захламленности в м3 на 1 га
     */
    @Column(name = "clutter_stock_per_hectare", length = 500)
    private String clutterStockPerHectare;

    /**
     * 50: Запас ликвида
     */
    @Column(name = "merchantable_stock", length = 500)
    private String merchantableStock;

    /**
     * 51: Код преобладающей породы
     */
    @Column(name = "dominant_species_code", length = 500)
    private String dominantSpeciesCode;

    /**
     * 52: Преобладающая порода
     */
    @Column(name = "dominant_species_name", length = 500)
    private String dominantSpeciesName;

    /**
     * 53: Код класса бонитета
     */
    @Column(name = "bonitet_class_code", length = 500)
    private String bonitetClassCode;

    /**
     * 54: Класс бонитета
     */
    @Column(name = "bonitet_class_name", length = 500)
    private String bonitetClassName;

    /**
     * 55: Хозяйственная секция
     */
    @Column(name = "economic_section", length = 500)
    private String economicSection;

    /**
     * 56: Давность вырубки
     */
    @Column(name = "logging_age", length = 500)
    private String loggingAge;

    /**
     * 57: Код возраста рубки
     */
    @Column(name = "cutting_age_code", length = 500)
    private String cuttingAgeCode;

    /**
     * 58: Наименование возраста рубки
     */
    @Column(name = "cutting_age_name", length = 500)
    private String cuttingAgeName;

    /**
     * 59: Площадь по лесоустройству
     */
    @Column(name = "forest_inventory_area", length = 500)
    private String forestInventoryArea;

    /**
     * 60: Площадь от ПОПД
     */
    @Column(name = "popd_area", length = 500)
    private String popdArea;

    /**
     * 61: Тип эрозии
     */
    @Column(name = "erosion_type", length = 500)
    private String erosionType;

    /**
     * 62: Степень эрозии
     */
    @Column(name = "erosion_degree", length = 500)
    private String erosionDegree;

    /**
     * 63: Код Экспозиция склона
     */
    @Column(name = "slope_exposure_code", length = 500)
    private String slopeExposureCode;

    /**
     * 64: Экспозиция склона
     */
    @Column(name = "slope_exposure_name", length = 500)
    private String slopeExposureName;

    /**
     * 65: Код Крутизна склона
     */
    @Column(name = "slope_steepness_code", length = 500)
    private String slopeSteepnessCode;

    /**
     * 66: Крутизна склона (справочник)
     */
    @Column(name = "slope_steepness_name", length = 500)
    private String slopeSteepnessName;

    /**
     * 67: Тип рельефа НСИ наименование
     */
    @Column(name = "relief_type_name", length = 500)
    private String reliefTypeName;

    /**
     * 68: Высота над уровнем моря
     */
    @Column(name = "elevation_above_sea_level", length = 500)
    private String elevationAboveSeaLevel;

    /**
     * 69: Количество пней на один гектар вырубки в штуках с округлением до 100 штук
     */
    @Column(name = "stumps_per_hectare", length = 1000)
    private String stumpsPerHectare;

    /**
     * 70: Средний диаметр пней в сантиметрах с округлением до 1 сантиметра
     */
    @Column(name = "average_stump_diameter", length = 1000)
    private String averageStumpDiameter;

    /**
     * 71: Тип вырубки (справочник) НСИ cuttingGroupCategory
     */
    @Column(name = "cutting_group_category_nsi", length = 500)
    private String cuttingGroupCategoryNsi;

    /**
     * 72: Наименование типа вырубки
     */
    @Column(name = "cutting_type_name", length = 500)
    private String cuttingTypeName;

    /**
     * 73: Год вырубки
     */
    @Column(name = "cutting_year", length = 500)
    private String cuttingYear;

    /**
     * 74: В том числе пней сосны
     */
    @Column(name = "pine_stumps_count", length = 500)
    private String pineStumpsCount;

    /**
     * 75: Код типа растительности
     */
    @Column(name = "vegetation_type_code", length = 500)
    private String vegetationTypeCode;

    /**
     * 76: Тип растительности
     */
    @Column(name = "vegetation_type_name", length = 500)
    private String vegetationTypeName;

    /**
     * 77: Код Тип болот
     */
    @Column(name = "bog_type_code", length = 500)
    private String bogTypeCode;

    /**
     * 78: Тип болот
     */
    @Column(name = "bog_type_name", length = 500)
    private String bogTypeName;

    /**
     * 79: Код породы НСИ
     */
    @Column(name = "species_nsi_code", length = 500)
    private String speciesNsiCode;

    /**
     * 80: Наименование породы НСИ
     */
    @Column(name = "species_nsi_name", length = 500)
    private String speciesNsiName;

    /**
     * 81: Процент зарастания
     */
    @Column(name = "overgrowth_percentage", length = 500)
    private String overgrowthPercentage;

    /**
     * 82: Толщина торфяного слоя
     */
    @Column(name = "peat_layer_thickness", length = 500)
    private String peatLayerThickness;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
