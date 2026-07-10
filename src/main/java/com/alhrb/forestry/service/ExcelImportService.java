package com.alhrb.forestry.service;

import com.alhrb.forestry.model.*;
import com.alhrb.forestry.repository.CuttingAreaRepository;
import com.alhrb.forestry.repository.ForestryUnitRepository;
import com.alhrb.forestry.repository.TerritoryUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final TerritoryUnitRepository territoryUnitRepository;
    private final ForestryUnitRepository  forestryUnitRepository;
    private final CuttingAreaRepository cuttingAreaRepository;
    private final GeometryService geometryService;

    @Transactional
    public List<CuttingArea> parseExcel(MultipartFile file) {
        List<CuttingArea> cuttingAreas = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            // Определяем индексы колонок
            int territoryNameIdx = -1;
            int quarterNumberIdx = -1;
            int plotNumberIdx = -1;
            int wktGeometryIdx = -1;
            int yearOfCutIdx = -1;
            int cutTypeIdx = -1;
            int regionNameIdx = -1;
            int districtNameIdx = -1;
            int forestryNameIdx = -1;

            for (Cell cell : headerRow) {
                String header = getStringValue(cell).trim().toLowerCase();
                int colIndex = cell.getColumnIndex();

                switch (header) {
                    case "территория":
                    case "territory":
                    case "полный путь":
                    case "full_path":
                        territoryNameIdx = colIndex;
                        break;
                    case "квартал":
                    case "quarter":
                        quarterNumberIdx = colIndex;
                        break;
                    case "номер деляны":
                    case "plot_number":
                        plotNumberIdx = colIndex;
                        break;
                    case "wkt_geometry":
                    case "wkt":
                    case "геометрия":
                        wktGeometryIdx = colIndex;
                        break;
                    case "год рубки":
                    case "year_of_cut":
                        yearOfCutIdx = colIndex;
                        break;
                    case "тип рубки":
                    case "cut_type":
                        cutTypeIdx = colIndex;
                        break;
                    case "регион":
                    case "region":
                        regionNameIdx = colIndex;
                        break;
                    case "район":
                    case "муниципальный район":
                    case "municipal_district":
                        districtNameIdx = colIndex;
                        break;
                    case "лесничество":
                    case "forestry":
                        forestryNameIdx = colIndex;
                        break;
                }
            }

            if (plotNumberIdx == -1 || wktGeometryIdx == -1) {
                throw new IllegalArgumentException(
                        "Не найдены обязательные колонки: номер деляны и геометрия (WKT)"
                );
            }

            if (quarterNumberIdx == -1 && territoryNameIdx == -1 && regionNameIdx == -1) {
                throw new IllegalArgumentException(
                        "Не найдена колонка с указанием квартала: 'квартал', 'территория' или 'регион'"
                );
            }

            WKTReader wktReader = new WKTReader();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    CuttingArea cuttingArea = new CuttingArea();
                    TerritoryUnit territoryUnit = null;
                    ForestryUnit forestryUnit = null;

                    // 1. Пробуем найти по номеру квартала
                    Integer quarterNumber = getIntValue(row.getCell(quarterNumberIdx));
                    if (quarterNumber != null && regionNameIdx != -1 && districtNameIdx != -1 && forestryNameIdx != -1) {
                        // Исправлено: передаем индексы, а не Cell
                        String regionName = getStringValue(row.getCell(regionNameIdx));
                        String districtName = getStringValue(row.getCell(districtNameIdx));
                        String forestryName = getStringValue(row.getCell(forestryNameIdx));

                        forestryUnit = findQuarterByHierarchy(regionName, districtName, forestryName, quarterNumber);
                    }

                    // 2. Если не нашли, пробуем по номеру квартала без иерархии
                    if (forestryUnit == null && quarterNumber != null) {
                        forestryUnit = findQuarterByNumber(quarterNumber);
                    }

                    // 3. Если не нашли, пробуем по полному пути
                    if (forestryUnit == null && forestryNameIdx != -1) {
                        String fullPath = getStringValue(row.getCell(forestryNameIdx));
                        if (fullPath != null && !fullPath.isEmpty()) {
                            forestryUnit = findForestryByFullPath(fullPath);
                        }
                    }

                    if (forestryUnit == null) {
                        throw new IllegalArgumentException(
                                String.format("Не найден квартал для строки %d", i + 1)
                        );
                    }

                    if (!forestryUnit.isQuarter()) {
                        throw new IllegalArgumentException(
                                String.format("Найденная территория '%s' не является кварталом", territoryUnit.getName())
                        );
                    }

                    cuttingArea.setForestryUnit(forestryUnit);

                    // Номер деляны
                    String plotNumber = getStringValue(row.getCell(plotNumberIdx));
                    if (plotNumber == null || plotNumber.isEmpty()) {
                        throw new IllegalArgumentException("Номер деляны не задан");
                    }
                    cuttingArea.setNumberInQuarter(plotNumber);

                    // Геометрия
                    String wkt = getStringValue(row.getCell(wktGeometryIdx));
                    if (wkt != null && !wkt.isEmpty()) {
                        Polygon polygon = (Polygon) wktReader.read(wkt);
                        if (!polygon.isValid()) {
                            throw new IllegalArgumentException("Невалидный полигон (возможно 'бабочка')");
                        }
                        cuttingArea.setGeometry(polygon);
                    }

                    // Дополнительно
                    if (yearOfCutIdx != -1) {
                        cuttingArea.setYearOfCut(getIntValue(row.getCell(yearOfCutIdx)));
                    }
                    if (cutTypeIdx != -1) {
                        cuttingArea.setCutType(getStringValue(row.getCell(cutTypeIdx)));
                    }

                    // Проверяем уникальность
                    Optional<CuttingArea> existing = cuttingAreaRepository.findByForestryUnitIdAndNumberInQuarter(
                            territoryUnit.getId(), plotNumber
                    );
                    if (existing.isPresent()) {
                        throw new IllegalArgumentException(
                                String.format("Деляна с номером '%s' уже существует в квартале %s",
                                        plotNumber, forestryUnit.getNumber())
                        );
                    }

                    cuttingAreas.add(cuttingArea);
                    log.debug("Добавлена деляна: {}", cuttingArea.getFullNumber());

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

        return cuttingAreas;
    }

    private ForestryUnit findQuarterByHierarchy(String regionName, String districtName,
                                                 String forestryName, Integer quarterNumber) {
        if (quarterNumber == null) return null;

        TerritoryUnit region = null;
        if (regionName != null && !regionName.isEmpty()) {
            List<TerritoryUnit> regions = territoryUnitRepository.findByTypeAndName(
                    TerritoryType.REGION, regionName
            );
            if (!regions.isEmpty()) {
                region = regions.get(0);
            }
        }

        TerritoryUnit district = null;
        if (region != null && districtName != null && !districtName.isEmpty()) {
            List<TerritoryUnit> districts = territoryUnitRepository.findByTypeAndParentIdAndName(
                    TerritoryType.MUNICIPAL_DISTRICT, region.getId(), districtName
            );
            if (!districts.isEmpty()) {
                district = districts.get(0);
            }
        }

        ForestryUnit forestry = null;
        if (district != null && forestryName != null && !forestryName.isEmpty()) {
            List<ForestryUnit> forestries = forestryUnitRepository.findByTypeAndParentIdAndName(
                    ForestryUnitType.FORESTRY, district.getId(), forestryName
            );
            if (!forestries.isEmpty()) {
                forestry = forestries.get(0);
            }
        }

        if (forestry != null) {
            List<ForestryUnit> quarters = forestryUnitRepository.findByTypeAndParentIdAndNumber(
                    ForestryUnitType.FOREST_QUARTER, forestry.getId(), String.valueOf(quarterNumber)
            );
            if (!quarters.isEmpty()) {
                return quarters.get(0);
            }
        }

        return null;
    }

    private ForestryUnit findQuarterByNumber(Integer quarterNumber) {
        if (quarterNumber == null) return null;
        List<ForestryUnit> quarters = forestryUnitRepository.findByTypeAndNumber(
                ForestryUnitType.FOREST_QUARTER, String.valueOf(quarterNumber)
        );
        return quarters.isEmpty() ? null : quarters.get(0);
    }

    private ForestryUnit findForestryByFullPath(String fullPath) {
        String[] parts = fullPath.split("/");
        ForestryUnit current = null;

        for (String part : parts) {
            String name = part.trim();
            if (name.isEmpty()) continue;

            ForestryUnit found = null;
            List<ForestryUnit> children;

            if (current == null) {
                children = forestryUnitRepository.findByParentIdIsNull();
            } else {
                children = forestryUnitRepository.findByParentId(current.getId());
            }

            for (ForestryUnit child : children) {
                if (child.isQuarter()) {
                    if (child.getNumber() != null && name.contains(child.getNumber())) {
                        found = child;
                        break;
                    }
                } else {
                    if (child.getName().equals(name)) {
                        found = child;
                        break;
                    }
                }
            }

            if (found == null) {
                log.warn("Не найдена территория: {}", name);
                return null;
            }
            current = found;
        }

        return current;
    }

    private String getStringValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private Integer getIntValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                if (val.isEmpty()) return null;
                return Integer.parseInt(val);
            }
        } catch (NumberFormatException e) {
            log.warn("Не удалось преобразовать значение в Integer: {}", cell);
            return null;
        }
        return null;
    }
}