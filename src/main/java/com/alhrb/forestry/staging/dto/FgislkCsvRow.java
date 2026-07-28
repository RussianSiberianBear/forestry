package com.alhrb.forestry.staging.dto;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FgislkCsvRow {

    @CsvBindByName(column = "\uFEFFregion_code")
    private String regionCode;

    @CsvBindByName(column = "region_name")
    private String regionName;

    @CsvBindByName(column = "forest_district_code")
    private String forestDistrictCode;

    @CsvBindByName(column = "forest_district_name")
    private String forestDistrictName;

    @CsvBindByName(column = "forest_quarter_code")
    private String forestQuarterCode;

    @CsvBindByName(column = "forest_plot_code")
    private String forestPlotCode;

    @CsvBindByName(column = "forest_plot_area")
    private String forestPlotArea;

    @CsvBindByName(column = "forest_plot_characteristic")
    private String forestPlotCharacteristic;

    @CsvBindByName(column = "forest_type")
    private String forestType;

    @CsvBindByName(column = "dominant_species")
    private String dominantSpecies;

    @CsvBindByName(column = "age_class")
    private String ageClass;

    @CsvBindByName(column = "forest_group")
    private String forestGroup;

    @CsvBindByName(column = "forest_category")
    private String forestCategory;

    @CsvBindByName(column = "protection_category")
    private String protectionCategory;

    @CsvBindByName(column = "purpose")
    private String purpose;

    @CsvBindByName(column = "inventory_date")
    @CsvDate("yyyy-MM-dd")
    private LocalDate inventoryDate;

    @CsvBindByName(column = "notes")
    private String notes;
}