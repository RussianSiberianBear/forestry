package com.alhrb.forestry.staging.dto;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

/**
 * Полная строка CSV-выгрузки ФГИС ЛК.
 * В текущем формате файла 83 столбца: позиции 0..82.
 * <p>
 * Все значения хранятся как String намеренно:
 * staging-слой должен принять исходные данные без потерь,
 * а типизация и сопоставление выполняются позднее.
 */
@Data
public class FgislkCsvRow {

    /**
     * 0: Код региона
     */
    @CsvBindByPosition(position = 0)
    private String regionCode;

    /**
     * 1: Наименование региона
     */
    @CsvBindByPosition(position = 1)
    private String regionName;

    /**
     * 2: Учетный номер лесничества
     */
    @CsvBindByPosition(position = 2)
    private String forestDistrictAccountingNumber;

    /**
     * 3: Наименование лесничества
     */
    @CsvBindByPosition(position = 3)
    private String forestDistrictName;

    /**
     * 4: Учетный номер участкового лесничества
     */
    @CsvBindByPosition(position = 4)
    private String localForestDistrictAccountingNumber;

    /**
     * 5: Наименование участкового лесничества
     */
    @CsvBindByPosition(position = 5)
    private String localForestDistrictName;

    /**
     * 6: Регистрационный номер квартала
     */
    @CsvBindByPosition(position = 6)
    private String quarterRegistrationNumber;

    /**
     * 7: Лесоустроительный номер квартала
     */
    @CsvBindByPosition(position = 7)
    private String quarterForestManagementNumber;

    /**
     * 8: Урочище
     */
    @CsvBindByPosition(position = 8)
    private String tract;

    /**
     * 9: id выдела
     */
    @CsvBindByPosition(position = 9)
    private String plotId;

    /**
     * 10: Регистрационный номер выдела
     */
    @CsvBindByPosition(position = 10)
    private String plotRegistrationNumber;

    /**
     * 11: Лесоустроительный номер выдела
     */
    @CsvBindByPosition(position = 11)
    private String plotForestManagementNumber;

    /**
     * 12: Статус выдела
     */
    @CsvBindByPosition(position = 12)
    private String plotStatus;

    /**
     * 13: Площадь выдела
     */
    @CsvBindByPosition(position = 13)
    private String plotArea;

    /**
     * 14: Общий запас лесных насаждений
     */
    @CsvBindByPosition(position = 14)
    private String totalGrowingStock;

    /**
     * 15: Код типа земель
     */
    @CsvBindByPosition(position = 15)
    private String landTypeCode;

    /**
     * 16: Наименование типа земель
     */
    @CsvBindByPosition(position = 16)
    private String landTypeName;

    /**
     * 17: Код вида лесных земель
     */
    @CsvBindByPosition(position = 17)
    private String forestLandTypeCode;

    /**
     * 18: Наименование вида лесных земель
     */
    @CsvBindByPosition(position = 18)
    private String forestLandTypeName;

    /**
     * 19: Код вида нелесных земель
     */
    @CsvBindByPosition(position = 19)
    private String nonForestLandTypeCode;

    /**
     * 20: Наименование вида нелесных земель
     */
    @CsvBindByPosition(position = 20)
    private String nonForestLandTypeName;

    /**
     * 21: Код типа леса
     */
    @CsvBindByPosition(position = 21)
    private String forestTypeCode;

    /**
     * 22: Наименование типа леса
     */
    @CsvBindByPosition(position = 22)
    private String forestTypeName;

    /**
     * 23: Наименование типа лесорастительных условий
     */
    @CsvBindByPosition(position = 23)
    private String forestSiteConditionsTypeName;

    /**
     * 24: Общий запас сухостоя
     */
    @CsvBindByPosition(position = 24)
    private String deadwoodStock;

    /**
     * 25: Общий запас естественных редин
     */
    @CsvBindByPosition(position = 25)
    private String naturalOpenForestStock;

    /**
     * 26: Общий запас единичных деревьев
     */
    @CsvBindByPosition(position = 26)
    private String singleTreesStock;

    /**
     * 27: Общий запас неликвидной древесины
     */
    @CsvBindByPosition(position = 27)
    private String nonCommercialWoodStock;

    /**
     * 28: Год создания лесных культур
     */
    @CsvBindByPosition(position = 28)
    private String forestPlantationCreationYear;

    /**
     * 29: Наименование состояния лесного насаждения
     */
    @CsvBindByPosition(position = 29)
    private String forestStandConditionName;

    /**
     * 30: Код целевой породы
     */
    @CsvBindByPosition(position = 30)
    private String targetSpeciesCode;

    /**
     * 31: Целевая порода (справочник) НСИ groupsTreeSpecies
     */
    @CsvBindByPosition(position = 31)
    private String targetSpeciesNsi;

    /**
     * 32: Наименование целевой породы
     */
    @CsvBindByPosition(position = 32)
    private String targetSpeciesName;

    /**
     * 33: Наименование загрязнения радионуклидами земель, на которых расположены леса
     */
    @CsvBindByPosition(position = 33)
    private String radionuclidePollutionName;

    /**
     * 34: Дата последнего лесоустройства
     */
    @CsvBindByPosition(position = 34)
    private String lastForestInventoryDate;

    /**
     * 35: Наименование объекта лесного семеноводства
     */
    @CsvBindByPosition(position = 35)
    private String forestSeedProductionObjectName;

    /**
     * 36: Особо защитные участки
     */
    @CsvBindByPosition(position = 36)
    private String specialProtectiveAreas;

    /**
     * 37: Хозяйственная категория
     */
    @CsvBindByPosition(position = 37)
    private String economicCategory;

    /**
     * 38: Категория защитности
     */
    @CsvBindByPosition(position = 38)
    private String protectionCategory;

    /**
     * 39: Код категорий защитных лесов
     */
    @CsvBindByPosition(position = 39)
    private String protectiveForestCategoryCode;

    /**
     * 40: Наименование категории защитных лесов
     */
    @CsvBindByPosition(position = 40)
    private String protectiveForestCategoryName;

    /**
     * 41: Код подкатегорий защитных лесов
     */
    @CsvBindByPosition(position = 41)
    private String protectiveForestSubcategoryCode;

    /**
     * 42: Наименование подкатегории защитных лесов
     */
    @CsvBindByPosition(position = 42)
    private String protectiveForestSubcategoryName;

    /**
     * 43: Мнемоника, административный район
     */
    @CsvBindByPosition(position = 43)
    private String administrativeDistrictMnemonic;

    /**
     * 44: Административный район
     */
    @CsvBindByPosition(position = 44)
    private String administrativeDistrictName;

    /**
     * 45: Особенности выдела
     */
    @CsvBindByPosition(position = 45)
    private String plotFeatures;

    /**
     * 46: Сведения о подсочке, осмолоподсочке
     */
    @CsvBindByPosition(position = 46)
    private String tappingInformation;

    /**
     * 47: Рекреационная характеристика
     */
    @CsvBindByPosition(position = 47)
    private String recreationalCharacteristic;

    /**
     * 48: Селекционная оценка
     */
    @CsvBindByPosition(position = 48)
    private String selectionAssessment;

    /**
     * 49: Запас захламленности в м3 на 1 га
     */
    @CsvBindByPosition(position = 49)
    private String clutterStockPerHectare;

    /**
     * 50: Запас ликвида
     */
    @CsvBindByPosition(position = 50)
    private String merchantableStock;

    /**
     * 51: Код преобладающей породы
     */
    @CsvBindByPosition(position = 51)
    private String dominantSpeciesCode;

    /**
     * 52: Преобладающая порода
     */
    @CsvBindByPosition(position = 52)
    private String dominantSpeciesName;

    /**
     * 53: Код класса бонитета
     */
    @CsvBindByPosition(position = 53)
    private String bonitetClassCode;

    /**
     * 54: Класс бонитета
     */
    @CsvBindByPosition(position = 54)
    private String bonitetClassName;

    /**
     * 55: Хозяйственная секция
     */
    @CsvBindByPosition(position = 55)
    private String economicSection;

    /**
     * 56: Давность вырубки
     */
    @CsvBindByPosition(position = 56)
    private String loggingAge;

    /**
     * 57: Код возраста рубки
     */
    @CsvBindByPosition(position = 57)
    private String cuttingAgeCode;

    /**
     * 58: Наименование возраста рубки
     */
    @CsvBindByPosition(position = 58)
    private String cuttingAgeName;

    /**
     * 59: Площадь по лесоустройству
     */
    @CsvBindByPosition(position = 59)
    private String forestInventoryArea;

    /**
     * 60: Площадь от ПОПД
     */
    @CsvBindByPosition(position = 60)
    private String popdArea;

    /**
     * 61: Тип эрозии
     */
    @CsvBindByPosition(position = 61)
    private String erosionType;

    /**
     * 62: Степень эрозии
     */
    @CsvBindByPosition(position = 62)
    private String erosionDegree;

    /**
     * 63: Код Экспозиция склона
     */
    @CsvBindByPosition(position = 63)
    private String slopeExposureCode;

    /**
     * 64: Экспозиция склона
     */
    @CsvBindByPosition(position = 64)
    private String slopeExposureName;

    /**
     * 65: Код Крутизна склона
     */
    @CsvBindByPosition(position = 65)
    private String slopeSteepnessCode;

    /**
     * 66: Крутизна склона (справочник)
     */
    @CsvBindByPosition(position = 66)
    private String slopeSteepnessName;

    /**
     * 67: Тип рельефа НСИ наименование
     */
    @CsvBindByPosition(position = 67)
    private String reliefTypeName;

    /**
     * 68: Высота над уровнем моря
     */
    @CsvBindByPosition(position = 68)
    private String elevationAboveSeaLevel;

    /**
     * 69: Количество пней на один гектар вырубки в штуках с округлением до 100 штук
     */
    @CsvBindByPosition(position = 69)
    private String stumpsPerHectare;

    /**
     * 70: Средний диаметр пней в сантиметрах с округлением до 1 сантиметра
     */
    @CsvBindByPosition(position = 70)
    private String averageStumpDiameter;

    /**
     * 71: Тип вырубки (справочник) НСИ cuttingGroupCategory
     */
    @CsvBindByPosition(position = 71)
    private String cuttingGroupCategoryNsi;

    /**
     * 72: Наименование типа вырубки
     */
    @CsvBindByPosition(position = 72)
    private String cuttingTypeName;

    /**
     * 73: Год вырубки
     */
    @CsvBindByPosition(position = 73)
    private String cuttingYear;

    /**
     * 74: В том числе пней сосны
     */
    @CsvBindByPosition(position = 74)
    private String pineStumpsCount;

    /**
     * 75: Код типа растительности
     */
    @CsvBindByPosition(position = 75)
    private String vegetationTypeCode;

    /**
     * 76: Тип растительности
     */
    @CsvBindByPosition(position = 76)
    private String vegetationTypeName;

    /**
     * 77: Код Тип болот
     */
    @CsvBindByPosition(position = 77)
    private String bogTypeCode;

    /**
     * 78: Тип болот
     */
    @CsvBindByPosition(position = 78)
    private String bogTypeName;

    /**
     * 79: Код породы НСИ
     */
    @CsvBindByPosition(position = 79)
    private String speciesNsiCode;

    /**
     * 80: Наименование породы НСИ
     */
    @CsvBindByPosition(position = 80)
    private String speciesNsiName;

    /**
     * 81: Процент зарастания
     */
    @CsvBindByPosition(position = 81)
    private String overgrowthPercentage;

    /**
     * 82: Толщина торфяного слоя
     */
    @CsvBindByPosition(position = 82)
    private String peatLayerThickness;

}
