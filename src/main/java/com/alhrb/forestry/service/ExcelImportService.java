package com.alhrb.forestry.service;

import com.alhrb.forestry.model.*;
import com.alhrb.forestry.repository.PlotRepository;
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
    private final PlotRepository plotRepository;
    private final GeometryService geometryService;

    @Transactional
    public List<Plot> parseExcel(MultipartFile file) {
        List<Plot> plots = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            // Определяем индексы колонок
            int territoryNameIdx = -1;      // полное имя территории (регион/район/лесничество/квартал)
            int quarterNumberIdx = -1;      // номер квартала
            int plotNumberIdx = -1;         // номер деляны
            int wktGeometryIdx = -1;        // WKT геометрия
            int yearOfCutIdx = -1;          // год рубки
            int cutTypeIdx = -1;            // тип рубки
            int territoryTypeIdx = -1;      // тип территории (для поиска)

            for (Cell cell : headerRow) {
                String header = getStringValue(cell).trim().toLowerCase();
                switch (header) {
                    case "территория":
                    case "territory":
                    case "полный путь":
                    case "full_path":
                        territoryNameIdx = cell.getColumnIndex();
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
                    case "тип территории":
                    case "territory_type":
                        territoryTypeIdx = cell.getColumnIndex();
                        break;
                }
            }

            if (plotNumberIdx == -1 || wktGeometryIdx == -1) {
                throw new IllegalArgumentException(
                        "Не найдены обязательные колонки: номер деляны и геометрия (WKT)"
                );
            }

            if (quarterNumberIdx == -1 && territoryNameIdx == -1) {
                throw new IllegalArgumentException(
                        "Не найдена колонка с указанием квартала: 'квартал' или 'территория'"
                );
            }

            WKTReader wktReader = new WKTReader();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    Plot plot = new Plot();

                    // ===== НАХОДИМ ТЕРРИТОРИАЛЬНУЮ ЕДИНИЦУ (КВАРТАЛ) =====
                    TerritoryUnit territoryUnit = null;

                    // 1. Пробуем найти по номеру квартала
                    Integer quarterNumber = getIntValue(row.getCell(quarterNumberIdx));
                    if (quarterNumber != null) {
                        // Ищем квартал по номеру в иерархии
                        // Для этого нужно знать вышестоящие уровни
                        String regionName = getStringValue(row.getCell(headerRow.getCell(0))); // пример
                        String districtName = getStringValue(row.getCell(headerRow.getCell(1)));
                        String forestryName = getStringValue(row.getCell(headerRow.getCell(2)));

                        territoryUnit = findQuarterByHierarchy(regionName, districtName, forestryName, quarterNumber);
                    }

                    // 2. Если не нашли по номеру, пробуем по полному пути
                    if (territoryUnit == null && territoryNameIdx != -1) {
                        String fullPath = getStringValue(row.getCell(territoryNameIdx));
                        if (fullPath != null && !fullPath.isEmpty()) {
                            territoryUnit = findTerritoryByFullPath(fullPath);
                        }
                    }

                    if (territoryUnit == null) {
                        throw new IllegalArgumentException(
                                String.format("Не найден квартал для строки %d", i + 1)
                        );
                    }

                    // Проверяем, что это действительно квартал
                    if (!territoryUnit.isQuarter()) {
                        throw new IllegalArgumentException(
                                String.format("Найденная территория '%s' не является кварталом", territoryUnit.getName())
                        );
                    }

                    plot.setTerritoryUnit(territoryUnit);

                    // ===== НОМЕР ДЕЛЯНЫ =====
                    String plotNumber = getStringValue(row.getCell(plotNumberIdx));
                    if (plotNumber == null || plotNumber.isEmpty()) {
                        throw new IllegalArgumentException("Номер деляны не задан");
                    }
                    plot.setNumberInQuarter(plotNumber);

                    // ===== ГЕОМЕТРИЯ =====
                    String wkt = getStringValue(row.getCell(wktGeometryIdx));
                    if (wkt != null && !wkt.isEmpty()) {
                        Polygon polygon = (Polygon) wktReader.read(wkt);

                        if (!polygon.isValid()) {
                            throw new IllegalArgumentException("Невалидный полигон (возможно 'бабочка')");
                        }

                        plot.setGeometry(polygon);
                    }

                    // ===== ДОПОЛНИТЕЛЬНО =====
                    if (yearOfCutIdx != -1) {
                        plot.setYearOfCut(getIntValue(row.getCell(yearOfCutIdx)));
                    }
                    if (cutTypeIdx != -1) {
                        plot.setCutType(getStringValue(row.getCell(cutTypeIdx)));
                    }

                    // Проверяем уникальность номера в квартале
                    Optional<Plot> existing = plotRepository.findByTerritoryUnitIdAndNumberInQuarter(
                            territoryUnit.getId(), plotNumber
                    );
                    if (existing.isPresent()) {
                        throw new IllegalArgumentException(
                                String.format("Деляна с номером '%s' уже существует в квартале %s",
                                        plotNumber, territoryUnit.getNumber())
                        );
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

    /**
     * Поиск квартала по иерархии: Регион -> Район -> Лесничество -> Квартал
     */
    private TerritoryUnit findQuarterByHierarchy(String regionName, String districtName,
                                                 String forestryName, Integer quarterNumber) {
        if (quarterNumber == null) return null;

        // Ищем регион
        TerritoryUnit region = null;
        if (regionName != null && !regionName.isEmpty()) {
            List<TerritoryUnit> regions = territoryUnitRepository.findByTypeAndName(
                    TerritoryType.REGION, regionName
            );
            if (!regions.isEmpty()) {
                region = regions.get(0);
            }
        }

        // Ищем район
        TerritoryUnit district = null;
        if (region != null && districtName != null && !districtName.isEmpty()) {
            List<TerritoryUnit> districts = territoryUnitRepository.findByTypeAndParentIdAndName(
                    TerritoryType.MUNICIPAL_DISTRICT, region.getId(), districtName
            );
            if (!districts.isEmpty()) {
                district = districts.get(0);
            }
        }

        // Ищем лесничество
        TerritoryUnit forestry = null;
        if (district != null && forestryName != null && !forestryName.isEmpty()) {
            List<TerritoryUnit> forestries = territoryUnitRepository.findByTypeAndParentIdAndName(
                    TerritoryType.FORESTRY, district.getId(), forestryName
            );
            if (!forestries.isEmpty()) {
                forestry = forestries.get(0);
            }
        }

        // Ищем квартал
        if (forestry != null) {
            List<TerritoryUnit> quarters = territoryUnitRepository.findByTypeAndParentIdAndNumber(
                    TerritoryType.QUARTER, forestry.getId(), String.valueOf(quarterNumber)
            );
            if (!quarters.isEmpty()) {
                return quarters.get(0);
            }
        }

        // Если не нашли через лесничество, ищем по всем уровням
        if (region != null) {
            List<TerritoryUnit> quarters = territoryUnitRepository.findByTypeAndNumber(
                    TerritoryType.QUARTER, String.valueOf(quarterNumber)
            );
            // Проверяем, что квартал принадлежит нужному региону
            for (TerritoryUnit quarter : quarters) {
                TerritoryUnit current = quarter;
                while (current != null) {
                    if (current.getId().equals(region.getId())) {
                        return quarter;
                    }
                    current = current.getParent();
                }
            }
        }

        return null;
    }

    /**
     * Поиск территории по полному пути (например: "Республика Бурятия / Бичурский район / Бичурское лесничество / Квартал 12")
     */
    private TerritoryUnit findTerritoryByFullPath(String fullPath) {
        String[] parts = fullPath.split("/");
        TerritoryUnit current = null;

        for (String part : parts) {
            String name = part.trim();
            if (name.isEmpty()) continue;

            TerritoryUnit found = null;
            List<TerritoryUnit> children;

            if (current == null) {
                // Ищем среди корневых (Федеральные округа)
                children = territoryUnitRepository.findByParentIdIsNull();
            } else {
                children = territoryUnitRepository.findByParentId(current.getId());
            }

            // Ищем по имени или номеру (для кварталов)
            for (TerritoryUnit child : children) {
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
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
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