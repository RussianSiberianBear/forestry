package com.alhrb.forestry.service;

import com.alhrb.forestry.dto.IntersectionReport;
import com.alhrb.forestry.dto.PlotMapDto;
import com.alhrb.forestry.model.*;
import com.alhrb.forestry.repository.ForestryUnitRepository;
import com.alhrb.forestry.repository.PlotRepository;
import com.alhrb.forestry.repository.TerritoryUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlotService {

    private final PlotRepository plotRepository;
    private final ForestryUnitRepository forestryUnitRepository;
    private final TerritoryUnitRepository  territoryUnitRepository;
    private final GeometryService geometryService;
    private final ExcelImportService excelImportService;

    @Value("${forest.validation.min-area:0.01}")
    private double minArea;

    @Transactional
    public Plot save(Plot plot) {
        return plotRepository.save(plot);
    }

    public List<Plot> findAll() {
        return plotRepository.findAll();
    }

    public Optional<Plot> findById(Long id) {
        return plotRepository.findById(id);
    }

    public Optional<Plot> findByFullNumber(String fullNumber) {
        return plotRepository.findByFullNumber(fullNumber);
    }

    public List<Plot> findByForestryUnitId(Long forestryUnitId) {
        return plotRepository.findByForestryUnitIdOrderByNumberInQuarter(forestryUnitId);
    }

    public Optional<Plot> findByForestryUnitIdAndNumberInQuarter(Long territoryUnitId, String numberInQuarter) {
        return plotRepository.findByForestryUnitIdAndNumberInQuarter(territoryUnitId, numberInQuarter);
    }

    public List<Plot> findByForestryUnitRecursive(Long unitId) {
        return plotRepository.findByForestryUnitRecursive(unitId);
    }

    public List<Plot> findByTerritoryUnit(Long unitId) {
        return plotRepository.findByTerritoryUnit(unitId);
    }

    public List<Plot> findByForestryTypeAndIdRecursive(String type, Long id) {
        return plotRepository.findByForestryTypeAndIdRecursive(type, id);
    }

    // ==========================================
    // МЕТОД ДЛЯ КАРТЫ С ФИЛЬТРАЦИЕЙ
    // ==========================================

    public List<PlotMapDto> getFilteredPlotsForMap(
            Long federalDistrictId,
            Long regionId,
            Long municipalDistrictId,
            Long forestryId,
            Long districtForestryId,
            Long technicalUnitId,
            Long quarterId,
            String cutType,
            Integer yearOfCut) {

        log.info("📡 Запрос фильтрованных делян:");
        log.info("   federalDistrictId={}", federalDistrictId);
        log.info("   regionId={}", regionId);
        log.info("   municipalDistrictId={}", municipalDistrictId);
        log.info("   forestryId={}", forestryId);
        log.info("   districtForestryId={}", districtForestryId);
        log.info("   technicalUnitId={}", technicalUnitId);
        log.info("   quarterId={}", quarterId);
        log.info("   cutType={}", cutType);
        log.info("   yearOfCut={}", yearOfCut);

        List<Plot> plots = new ArrayList<>();

        if (quarterId != null) {
            plots = plotRepository.findByForestryUnitRecursive(quarterId);
            log.info("📊 Фильтр по кварталу ID={}, найдено {} делян", quarterId, plots.size());
        } else if (technicalUnitId != null) {
            plots = plotRepository.findByForestryUnitRecursive(technicalUnitId);
            log.info("📊 Фильтр по техучастку ID={}, найдено {} делян", technicalUnitId, plots.size());
        } else if (districtForestryId != null) {
            plots = plotRepository.findByForestryUnitRecursive(districtForestryId);
            log.info("📊 Фильтр по участковому лесничеству ID={}, найдено {} делян", districtForestryId, plots.size());
        } else if (forestryId != null) {
            plots = plotRepository.findByForestryUnitRecursive(forestryId);
            log.info("📊 Фильтр по лесничеству ID={}, найдено {} делян", forestryId, plots.size());
        } else if (municipalDistrictId != null) {
            plots = plotRepository.findByForestryUnitRecursive(municipalDistrictId);
            log.info("📊 Фильтр по району ID={}, найдено {} делян", municipalDistrictId, plots.size());
        } else if (regionId != null) {
            plots = plotRepository.findByForestryUnitRecursive(regionId);
            log.info("📊 Фильтр по региону ID={}, найдено {} делян", regionId, plots.size());
        } else if (federalDistrictId != null) {
            plots = plotRepository.findByForestryUnitRecursive(federalDistrictId);
            log.info("📊 Фильтр по федеральному округу ID={}, найдено {} делян", federalDistrictId, plots.size());
        } else {
            plots = plotRepository.findAll();
            log.info("📊 Фильтр не применен, всего {} делян", plots.size());
        }

        if (cutType != null && !cutType.isEmpty()) {
            int before = plots.size();
            plots = plots.stream()
                    .filter(p -> p.getCutType() != null && p.getCutType().equals(cutType))
                    .collect(Collectors.toList());
            log.info("📊 После фильтра по типу рубки '{}': {} -> {} делян", cutType, before, plots.size());
        }

        if (yearOfCut != null) {
            int before = plots.size();
            plots = plots.stream()
                    .filter(p -> p.getYearOfCut() != null && p.getYearOfCut().equals(yearOfCut))
                    .collect(Collectors.toList());
            log.info("📊 После фильтра по году рубки '{}': {} -> {} делян", yearOfCut, before, plots.size());
        }

        log.info("📊 ИТОГО найдено {} делян", plots.size());

        return plots.stream()
                .map(this::convertToMapDto)
                .collect(Collectors.toList());
    }

    public List<PlotMapDto> getAllPlotsForMap() {
        List<Plot> plots = plotRepository.findAll();
        log.info("📊 Всего делян для карты: {}", plots.size());
        return plots.stream()
                .map(this::convertToMapDto)
                .collect(Collectors.toList());
    }

    private PlotMapDto convertToMapDto(Plot plot) {
        PlotMapDto dto = new PlotMapDto();
        dto.setId(plot.getId());
        dto.setFullNumber(plot.getFullNumber());
        dto.setNumberInQuarter(plot.getNumberInQuarter());
        dto.setVerified(plot.getVerified());
        dto.setCutType(plot.getCutType());
        dto.setYearOfCut(plot.getYearOfCut());

        dto.setAreaHa(plot.getAreaHa());
        if (plot.getAreaHa() != null) {
            dto.setAreaM2(plot.getAreaHa() * 10000);
        }

        if (plot.getForestryUnit() != null) {
            ForestryUnit unit = plot.getForestryUnit();

            // Номер квартала
            if (unit.isQuarter()) {
                dto.setQuarterNumber(unit.getNumber() != null ? unit.getNumber() : unit.getName());
            } else {
                dto.setQuarterNumber(unit.getName());
            }

            // Ищем лесничество для отображения на карте
            ForestryUnit current = unit;
            while (current != null) {
                if (current.isForestry()) {
                    dto.setForestryName(current.getName());
                    break;
                }
                current = current.getParent();
            }

            // Сохраняем полный путь для показа в таблице
            dto.setTerritoryPath(unit.getFullPath());
        }

        if (plot.getGeometry() != null) {
            try {
                GeoJsonWriter writer = new GeoJsonWriter();
                dto.setGeometryGeoJson(writer.write(plot.getGeometry()));
            } catch (Exception e) {
                log.error("Ошибка конвертации геометрии в GeoJSON: {}", e.getMessage());
                dto.setGeometryGeoJson(null);
            }
        }

        return dto;
    }

    // ==========================================
    // СОЗДАНИЕ ДЕЛЯНЫ
    // ==========================================

    @Transactional
    public List<IntersectionReport> createPlotWithValidation(
            String numberInQuarter,
            String plots,
            String description,
            Polygon geometry,
            Long forestryUnitId,
            Integer yearOfCut,
            String cutType) {

        if (geometry == null) {
            throw new IllegalArgumentException("Геометрия деляны не задана");
        }

        if (!geometryService.isValid(geometry)) {
            throw new IllegalArgumentException(
                    "⚠️ Деляна имеет некорректную геометрию! Возможна 'бабочка' (самопересечение)."
            );
        }

        ForestryUnit forestryUnit = forestryUnitRepository.findById(forestryUnitId)
                .orElseThrow(() -> new IllegalArgumentException("Территориальная единица не найдена"));

        if (!forestryUnit.isQuarter()) {
            throw new IllegalArgumentException("Деляна может быть привязана только к кварталу!");
        }

        // Проверяем геометрию квартала
        if (forestryUnit.getGeometry() != null) {
            if (forestryUnit.getGeometry() instanceof Polygon) {
                // quarterNumber уже String, просто передаем
                String quarterNumber = forestryUnit.getNumber() != null ?
                        forestryUnit.getNumber() :
                        forestryUnit.getName();

                geometryService.validatePlotInsideQuarter(
                        geometry,
                        (Polygon) forestryUnit.getGeometry(),
                        numberInQuarter,
                        quarterNumber  // ← String
                );
            } else {
                log.warn("⚠️ Геометрия квартала не является Polygon, проверка пропущена");
            }
        }

        Optional<Plot> existing = plotRepository.findByForestryUnitIdAndNumberInQuarter(
                forestryUnitId, numberInQuarter);
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    String.format("❌ Деляна с номером '%s' уже существует в квартале %s!",
                            numberInQuarter,
                            forestryUnit.getNumber() != null ? forestryUnit.getNumber() : forestryUnit.getName())
            );
        }

        Plot plot = new Plot();
        plot.setNumberInQuarter(numberInQuarter);
        plot.setPlots(plots);
        plot.setDescription(description);
        plot.setGeometry(geometry);
        plot.setForestryUnit(forestryUnit);
        plot.setYearOfCut(yearOfCut);
        plot.setCutType(cutType);

        return saveWithValidation(plot);
    }

    @Transactional
    public List<IntersectionReport> saveWithValidation(Plot plot) {
        if (plot.getGeometry() == null) {
            throw new IllegalArgumentException("Геометрия деляны не задана");
        }

        if (!geometryService.isValid(plot.getGeometry())) {
            throw new IllegalArgumentException(
                    "⚠️ Деляна имеет некорректную геометрию! Возможна 'бабочка' (самопересечение)."
            );
        }

        if (plot.getNumberInQuarter() == null || plot.getNumberInQuarter().isEmpty()) {
            throw new IllegalArgumentException("Номер деляны в квартале обязателен!");
        }

        if (plot.getForestryUnit() != null) {
            Optional<Plot> existing = plotRepository.findByForestryUnitIdAndNumberInQuarter(
                    plot.getForestryUnit().getId(),
                    plot.getNumberInQuarter()
            );
            if (existing.isPresent() && !existing.get().getId().equals(plot.getId())) {
                throw new IllegalArgumentException(
                        String.format("❌ Деляна с номером '%s' уже существует в квартале %s!",
                                plot.getNumberInQuarter(),
                                plot.getForestryUnit().getNumber() != null ?
                                        plot.getForestryUnit().getNumber() :
                                        plot.getForestryUnit().getName())
                );
            }
        }

        if (plot.getForestryUnit() != null && plot.getForestryUnit().getGeometry() != null) {
            if (plot.getForestryUnit().getGeometry() instanceof Polygon) {
                String quarterNumber = plot.getForestryUnit().getNumber() != null ?
                        plot.getForestryUnit().getNumber() :
                        plot.getForestryUnit().getName();

                geometryService.validatePlotInsideQuarter(
                        plot.getGeometry(),
                        (Polygon) plot.getForestryUnit().getGeometry(),
                        plot.getFullNumber() != null ? plot.getFullNumber() : plot.getNumberInQuarter(),
                        quarterNumber  // ← String
                );
            }
        }

        Plot saved = plotRepository.save(plot);
        log.info("✅ Сохранена деляна: {} (ID: {}, площадь: {} га, территория: {})",
                saved.getFullNumber(), saved.getId(), saved.getAreaHa(),
                saved.getForestryUnit() != null ? saved.getForestryUnit().getFullPath() : "null");

        List<IntersectionReport> conflicts = validatePlot(saved);

        if (conflicts.isEmpty()) {
            saved.setVerified(true);
            plotRepository.save(saved);
            log.info("✅ Деляна {} верифицирована (пересечений нет)", saved.getFullNumber());
        } else {
            saved.setVerified(false);
            plotRepository.save(saved);
            log.warn("⚠️ Деляна {} имеет {} пересечений", saved.getFullNumber(), conflicts.size());
        }

        return conflicts;
    }

    @Transactional
    public List<IntersectionReport> validatePlot(Plot plot) {
        List<IntersectionReport> reports = new ArrayList<>();

        if (plot.getGeometry() == null) {
            return reports;
        }

        List<Object[]> results = plotRepository.findIntersectionsWithPlot(
                plot.getGeometry(),
                plot.getId(),
                minArea
        );

        for (Object[] row : results) {
            IntersectionReport report = new IntersectionReport();
            report.setPlot1Id(plot.getId());
            report.setPlot1Number(plot.getFullNumber());
            report.setPlot2Id((Long) row[0]);
            report.setPlot2Number((String) row[1]);
            report.setOverlapArea((Double) row[2]);

            if (report.getOverlapArea() > 1.0) {
                report.setSeverity("CRITICAL");
            } else if (report.getOverlapArea() > 0.1) {
                report.setSeverity("WARNING");
            } else {
                report.setSeverity("OK");
            }

            reports.add(report);
        }

        return reports;
    }

    @Transactional
    public List<IntersectionReport> validateAllPlots() {
        List<IntersectionReport> reports = new ArrayList<>();
        List<Object[]> results = plotRepository.findAllIntersections(minArea);

        for (Object[] row : results) {
            IntersectionReport report = new IntersectionReport();
            report.setPlot1Id((Long) row[0]);
            report.setPlot2Id((Long) row[1]);
            report.setOverlapArea((Double) row[2]);

            Plot plot1 = plotRepository.findById(report.getPlot1Id()).orElse(null);
            Plot plot2 = plotRepository.findById(report.getPlot2Id()).orElse(null);

            if (plot1 != null) report.setPlot1Number(plot1.getFullNumber());
            if (plot2 != null) report.setPlot2Number(plot2.getFullNumber());

            if (report.getOverlapArea() > 1.0) {
                report.setSeverity("CRITICAL");
            } else if (report.getOverlapArea() > 0.1) {
                report.setSeverity("WARNING");
            } else {
                report.setSeverity("OK");
            }

            reports.add(report);
        }

        if (reports.isEmpty()) {
            List<Plot> allPlots = plotRepository.findAll();
            for (Plot plot : allPlots) {
                plot.setVerified(true);
                plotRepository.save(plot);
            }
            log.info("✅ Все деляны верифицированы (пересечений нет)");
        } else {
            for (IntersectionReport report : reports) {
                plotRepository.findById(report.getPlot1Id()).ifPresent(plot -> {
                    plot.setVerified(false);
                    plotRepository.save(plot);
                });
                plotRepository.findById(report.getPlot2Id()).ifPresent(plot -> {
                    plot.setVerified(false);
                    plotRepository.save(plot);
                });
            }
            log.warn("⚠️ Найдено {} пересечений, верификация снята с проблемных делян", reports.size());
        }

        return reports;
    }

    @Transactional
    public List<IntersectionReport> validatePlots(List<Plot> plots) {
        List<IntersectionReport> allReports = new ArrayList<>();

        for (Plot plot : plots) {
            if (plot.getGeometry() != null) {
                List<IntersectionReport> reports = validatePlot(plot);
                allReports.addAll(reports);
            }
        }

        Set<String> seen = new HashSet<>();
        List<IntersectionReport> uniqueReports = new ArrayList<>();
        for (IntersectionReport report : allReports) {
            String key = Math.min(report.getPlot1Id(), report.getPlot2Id()) + "_" +
                    Math.max(report.getPlot1Id(), report.getPlot2Id());
            if (!seen.contains(key)) {
                seen.add(key);
                uniqueReports.add(report);
            }
        }

        return uniqueReports;
    }

    @Transactional
    public List<IntersectionReport> importFromExcel(MultipartFile file) {
        log.info("Начинаем импорт из Excel: {}", file.getOriginalFilename());

        List<Plot> plots = excelImportService.parseExcel(file);
        log.info("Распаршено {} делян", plots.size());

        List<Plot> savedPlots = new ArrayList<>();
        List<IntersectionReport> allConflicts = new ArrayList<>();

        for (Plot plot : plots) {
            try {
                List<IntersectionReport> conflicts = saveWithValidation(plot);
                savedPlots.add(plot);
                allConflicts.addAll(conflicts);
            } catch (Exception e) {
                log.error("Ошибка при сохранении деляны {}: {}",
                        plot.getNumberInQuarter(), e.getMessage());
                throw new RuntimeException(
                        String.format("Ошибка при импорте деляны '%s': %s",
                                plot.getNumberInQuarter(), e.getMessage())
                );
            }
        }

        if (allConflicts.isEmpty()) {
            for (Plot plot : savedPlots) {
                plot.setVerified(true);
                plotRepository.save(plot);
            }
            log.info("Все деляны успешно импортированы и верифицированы");
        } else {
            log.warn("Импорт завершён с {} конфликтами", allConflicts.size());
        }

        return allConflicts;
    }

    @Transactional
    public int fixMissingTerritoryUnit() {
        List<Plot> plots = plotRepository.findAll();
        int fixed = 0;

        for (Plot plot : plots) {
            if (plot.getForestryUnit() == null) {
                log.warn("⚠️ У деляны {} нет территориальной единицы", plot.getFullNumber());
            }
        }

        log.info("✅ Проверка завершена");
        return fixed;
    }
}