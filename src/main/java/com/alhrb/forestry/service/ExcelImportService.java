package com.alhrb.forestry.service;

import com.alhrb.forestry.model.*;
import com.alhrb.forestry.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final RegionRepository regionRepository;
    private final MunicipalDistrictRepository municipalDistrictRepository;
    private final ForestryRepository forestryRepository;
    private final DistrictForestryRepository districtForestryRepository;
    private final QuarterRepository quarterRepository;
    private final GeometryService geometryService;

    public List<Plot> parseExcel(MultipartFile file) {
        List<Plot> plots = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            // Определяем индексы колонок
            int regionNameIdx = -1;
            int municipalDistrictNameIdx = -1;
            int forestryNameIdx = -1;
            int districtForestryNameIdx = -1;
            int quarterNumberIdx = -1;
            int plotNumberIdx = -1;
            int wktGeometryIdx = -1;
            int yearOfCutIdx = -1;
            int cutTypeIdx = -1;

            for (Cell cell : headerRow) {
                String header = getStringValue(cell).trim().toLowerCase();
                switch (header) {
                    case "регион":
                    case "region":
                        regionNameIdx = cell.getColumnIndex();
                        break;
                    case "муниципальный район":
                    case "район":
                    case "municipal_district":
                        municipalDistrictNameIdx = cell.getColumnIndex();
                        break;
                    case "лесничество":
                    case "forestry":
                        forestryNameIdx = cell.getColumnIndex();
                        break;
                    case "участковое лесничество":
                    case "district_forestry":
                        districtForestryNameIdx = cell.getColumnIndex();
                        break;
                    case "квартал":
                    case "quarter":
                        quarterNumberIdx = cell.getColumnIndex();
                        break;
                    case "номер деляны":
                    case "plot_number":
                        plotNumberIdx = cell.getColumnIndex();
                        break;
                    case "wkt_geometry":
                    case "wkt":
                    case "геометрия":
                        wktGeometryIdx = cell.getColumnIndex();
                        break;
                    case "год рубки":
                    case "year_of_cut":
                        yearOfCutIdx = cell.getColumnIndex();
                        break;
                    case "тип рубки":
                    case "cut_type":
                        cutTypeIdx = cell.getColumnIndex();
                        break;
                }
            }

            if (plotNumberIdx == -1 || wktGeometryIdx == -1 || quarterNumberIdx == -1) {
                throw new IllegalArgumentException(
                        "Не найдены обязательные колонки: квартал, номер деляны и геометрия (WKT)"
                );
            }

            WKTReader wktReader = new WKTReader();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    Plot plot = new Plot();

                    // === ИЕРАРХИЯ ===

                    // Регион
                    String regionName = getStringValue(row.getCell(regionNameIdx));
                    if (regionName != null && !regionName.isEmpty()) {
                        Region region = regionRepository.findByName(regionName)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        String.format("Регион '%s' не найден", regionName))
                                );
                        plot.setRegion(region);
                    }

                    // Муниципальный район
                    String districtName = getStringValue(row.getCell(municipalDistrictNameIdx));
                    if (districtName != null && !districtName.isEmpty()) {
                        MunicipalDistrict municipalDistrict = municipalDistrictRepository
                                .findByRegionIdAndName(plot.getRegion().getId(), districtName)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        String.format("Район '%s' не найден в регионе", districtName))
                                );
                        plot.setMunicipalDistrict(municipalDistrict);
                    }

                    // Лесничество
                    String forestryName = getStringValue(row.getCell(forestryNameIdx));
                    if (forestryName != null && !forestryName.isEmpty()) {
                        Forestry forestry = forestryRepository
                                .findByMunicipalDistrictIdAndName(plot.getMunicipalDistrict().getId(), forestryName)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        String.format("Лесничество '%s' не найдено в районе", forestryName))
                                );
                        plot.setForestry(forestry);
                    }

                    // Участковое лесничество
                    String districtForestryName = getStringValue(row.getCell(districtForestryNameIdx));
                    if (districtForestryName != null && !districtForestryName.isEmpty()) {
                        DistrictForestry districtForestry = districtForestryRepository
                                .findByForestryIdAndName(plot.getForestry().getId(), districtForestryName)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        String.format("Участковое лесничество '%s' не найдено", districtForestryName))
                                );
                        plot.setDistrictForestry(districtForestry);
                    }

                    // Квартал
                    Integer quarterNumber = getIntValue(row.getCell(quarterNumberIdx));
                    if (quarterNumber != null) {
                        Quarter quarter = quarterRepository
                                .findByDistrictForestryIdAndNumber(plot.getDistrictForestry().getId(), quarterNumber)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        String.format("Квартал %d не найден", quarterNumber))
                                );
                        plot.setQuarter(quarter);
                    }

                    // === НОМЕР ДЕЛЯНЫ ===
                    String plotNumber = getStringValue(row.getCell(plotNumberIdx));
                    if (plotNumber == null || plotNumber.isEmpty()) {
                        throw new IllegalArgumentException("Номер деляны не задан");
                    }
                    plot.setNumberInQuarter(plotNumber);

                    // === ГЕОМЕТРИЯ ===
                    String wkt = getStringValue(row.getCell(wktGeometryIdx));
                    if (wkt != null && !wkt.isEmpty()) {
                        Polygon polygon = (Polygon) wktReader.read(wkt);

                        if (!polygon.isValid()) {
                            throw new IllegalArgumentException("Невалидный полигон (возможно 'бабочка')");
                        }

                        plot.setGeometry(polygon);
                    }

                    // === ДОПОЛНИТЕЛЬНО ===
                    if (yearOfCutIdx != -1) {
                        plot.setYearOfCut(getIntValue(row.getCell(yearOfCutIdx)));
                    }
                    if (cutTypeIdx != -1) {
                        plot.setCutType(getStringValue(row.getCell(cutTypeIdx)));
                    }

                    plots.add(plot);
                    log.debug("Добавлена деляна: {}", plot.getFullNumber());

                } catch (Exception e) {
                    String error = String.format("Строка %d: %s", i + 1, e.getMessage());
                    errors.add(error);
                    log.error(error);
                }
            }

            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(
                        "Ошибки при импорте:\n" + String.join("\n", errors)
                );
            }

        } catch (IOException e) {
            log.error("Ошибка при чтении Excel файла", e);
            throw new RuntimeException("Ошибка при чтении файла: " + e.getMessage());
        }

        return plots;
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return null;
        }
    }

    private Integer getIntValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            if (cell.getCellType() == CellType.STRING) {
                return Integer.parseInt(cell.getStringCellValue().trim());
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
