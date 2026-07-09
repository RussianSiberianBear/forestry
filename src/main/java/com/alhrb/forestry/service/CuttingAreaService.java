package com.alhrb.forestry.service;

import com.alhrb.forestry.dto.IntersectionReport;
import com.alhrb.forestry.dto.CuttingAreaMapDto;
import com.alhrb.forestry.model.*;
import com.alhrb.forestry.repository.ForestryUnitRepository;
import com.alhrb.forestry.repository.CuttingAreaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CuttingAreaService {

    private final CuttingAreaRepository cuttingAreaRepository;
    private final ForestryUnitRepository forestryUnitRepository;
    private final GeometryService geometryService;
    private final ExcelImportService excelImportService;

    @Value("${forest.validation.min-area:0.01}")
    private double minArea;

    @Transactional
    public CuttingArea save(CuttingArea cuttingArea) {
        return cuttingAreaRepository.save(cuttingArea);
    }

    public List<CuttingArea> findAll() {
        return cuttingAreaRepository.findAll();
    }

    public Optional<CuttingArea> findById(Long id) {
        return cuttingAreaRepository.findById(id);
    }

    public Optional<CuttingArea> findByFullNumber(String fullNumber) {
        return cuttingAreaRepository.findByFullNumber(fullNumber);
    }

    public List<CuttingArea> findByForestryUnitId(Long forestryUnitId) {
        return cuttingAreaRepository.findByForestryUnitIdOrderByNumberInQuarter(forestryUnitId);
    }

    public Optional<CuttingArea> findByForestryUnitIdAndNumberInQuarter(Long territoryUnitId, String numberInQuarter) {
        return cuttingAreaRepository.findByForestryUnitIdAndNumberInQuarter(territoryUnitId, numberInQuarter);
    }

    public List<CuttingArea> findByForestryUnitRecursive(Long unitId) {
        return cuttingAreaRepository.findByForestryUnitRecursive(unitId);
    }

    public List<CuttingArea> findByTerritoryUnitRecursive(Long unitId) {
        return cuttingAreaRepository.findByTerritoryUnitRecursive(unitId);
    }

    public List<CuttingArea> findByForestryTypeAndIdRecursive(String type, Long id) {
        return cuttingAreaRepository.findByForestryTypeAndIdRecursive(type, id);
    }

    // ==========================================
    // МЕТОД ДЛЯ КАРТЫ С ФИЛЬТРАЦИЕЙ
    // ==========================================

    public List<CuttingAreaMapDto> getFilteredCuttingAreasForMap(
            Long federalDistrictId,
            Long regionId,
            Long municipalDistrictId,
            Long forestryId,
            Long districtForestryId,
            Long technicalUnitId,
            Long quarterId,
            String numberInQuarter,
            String cutType,
            Integer yearOfCut) {

        List<CuttingArea> cuttingAreas = new ArrayList<>();

        // ===== ФИЛЬТР ПО ЛЕСНЫМ ЕДИНИЦАМ (иерархия forestry_units) =====
        if (quarterId != null) {
            // Квартал - лесная единица
            cuttingAreas = cuttingAreaRepository.findByForestryUnitRecursive(quarterId);
            log.info("📊 Фильтр по кварталу ID={}, найдено {} делян", quarterId, cuttingAreas.size());
        } else if (technicalUnitId != null) {
            // Техучасток - лесная единица
            cuttingAreas = cuttingAreaRepository.findByForestryUnitRecursive(technicalUnitId);
            log.info("📊 Фильтр по техучастку ID={}, найдено {} делян", technicalUnitId, cuttingAreas.size());
        } else if (districtForestryId != null) {
            // Участковое лесничество - лесная единица
            cuttingAreas = cuttingAreaRepository.findByForestryUnitRecursive(districtForestryId);
            log.info("📊 Фильтр по участковому лесничеству ID={}, найдено {} делян", districtForestryId, cuttingAreas.size());
        } else if (forestryId != null) {
            // Лесничество - лесная единица
            cuttingAreas = cuttingAreaRepository.findByForestryUnitRecursive(forestryId);
            log.info("📊 Фильтр по лесничеству ID={}, найдено {} делян", forestryId, cuttingAreas.size());

            // ===== ФИЛЬТР ПО ТЕРРИТОРИАЛЬНЫМ ЕДИНИЦАМ (иерархия territory_units) =====
        } else if (municipalDistrictId != null) {
            // Муниципальный район - территориальная единица
            cuttingAreas = cuttingAreaRepository.findByTerritoryUnitRecursive(municipalDistrictId);
            log.info("📊 Фильтр по муниципальному району ID={}, найдено {} делян", municipalDistrictId, cuttingAreas.size());
        } else if (regionId != null) {
            // Регион - территориальная единица
            cuttingAreas = cuttingAreaRepository.findByTerritoryUnitRecursive(regionId);
            log.info("📊 Фильтр по региону ID={}, найдено {} делян", regionId, cuttingAreas.size());
        } else if (federalDistrictId != null) {
            // Федеральный округ - территориальная единица
            cuttingAreas = cuttingAreaRepository.findByTerritoryUnitRecursive(federalDistrictId);
            log.info("📊 Фильтр по федеральному округу ID={}, найдено {} делян", federalDistrictId, cuttingAreas.size());
        } else {
            // Без фильтра - все деляны
            cuttingAreas = cuttingAreaRepository.findAll();
            log.info("📊 Фильтр не применен, всего {} делян", cuttingAreas.size());
        }

        // ===== ДОПОЛНИТЕЛЬНАЯ ФИЛЬТРАЦИЯ ПО АТРИБУТАМ =====
        // Номера делян могут быть разделены запятой,точкой с запятой или пробелом чтобы можно было отобразить несколько
        if (numberInQuarter != null && !numberInQuarter.isEmpty()) {
            // Нормализуем строку: заменяем все разделители на пробелы
            String normalized = numberInQuarter
                    .replaceAll("[,;]", " ")  // заменяем запятые и точки с запятой на пробелы
                    .replaceAll("\\s+", " ")  // заменяем все пробелы (включая множественные) на один
                    .trim();                  // удаляем пробелы в начале и конце

            Set<String> allowedValues = new HashSet<>();
            if (!normalized.isEmpty()) {
                String[] parts = normalized.split(" ");
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        allowedValues.add(part);
                    }
                }
            }

            cuttingAreas = cuttingAreas.stream()
                    .filter(p -> {
                        String pValue = p.getNumberInQuarter() != null ?
                                p.getNumberInQuarter().trim() : "";
                        return allowedValues.contains(pValue);
                    })
                    .collect(Collectors.toList());
        }

        if (cutType != null && !cutType.isEmpty()) {
            cuttingAreas = cuttingAreas.stream()
                    .filter(p -> p.getCutType() != null && p.getCutType().equals(cutType))
                    .collect(Collectors.toList());
        }

        if (yearOfCut != null) {
            int before = cuttingAreas.size();
            cuttingAreas = cuttingAreas.stream()
                    .filter(p -> p.getYearOfCut() != null && p.getYearOfCut().equals(yearOfCut))
                    .collect(Collectors.toList());
            log.info("📊 После фильтра по году рубки '{}': {} -> {} делян", yearOfCut, before, cuttingAreas.size());
        }

        log.info("📊 ИТОГО найдено {} делян", cuttingAreas.size());

        return cuttingAreas.stream()
                .map(this::convertToMapDto)
                .collect(Collectors.toList());
    }

    public List<CuttingAreaMapDto> getAllCuttingAreasForMap() {
        List<CuttingArea> cuttingAreas = cuttingAreaRepository.findAll();
        log.info("📊 Всего делян для карты: {}", cuttingAreas.size());
        return cuttingAreas.stream()
                .map(this::convertToMapDto)
                .collect(Collectors.toList());
    }

    private CuttingAreaMapDto convertToMapDto(CuttingArea cuttingArea) {
        CuttingAreaMapDto dto = new CuttingAreaMapDto();
        dto.setId(cuttingArea.getId());
        dto.setFullNumber(cuttingArea.getFullNumber());
        dto.setNumberInQuarter(cuttingArea.getNumberInQuarter());
        dto.setVerified(cuttingArea.getVerified());
        dto.setCutType(cuttingArea.getCutType());
        dto.setYearOfCut(cuttingArea.getYearOfCut());

        dto.setAreaHa(cuttingArea.getAreaHa());
        if (cuttingArea.getAreaHa() != null) {
            dto.setAreaM2(cuttingArea.getAreaHa() * 10000);
        }

        if (cuttingArea.getForestryUnit() != null) {
            ForestryUnit unit = cuttingArea.getForestryUnit();

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

        if (cuttingArea.getGeometry() != null) {
            try {
                GeoJsonWriter writer = new GeoJsonWriter();
                dto.setGeometryGeoJson(writer.write(cuttingArea.getGeometry()));
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

        // НОРМАЛИЗУЕМ ГЕОМЕТРИЮ СРАЗУ ПРИ СОЗДАНИИ
        Polygon normalizedGeometry = geometryService.normalizePolygon(geometry);

        if (!geometryService.isValid(normalizedGeometry)) {
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
                Polygon normalizedQuarter = geometryService.normalizePolygon(
                        (Polygon) forestryUnit.getGeometry()
                );

                String quarterNumber = forestryUnit.getNumber() != null ?
                        forestryUnit.getNumber() :
                        forestryUnit.getName();

                geometryService.validatePlotInsideQuarter(
                        normalizedGeometry,
                        normalizedQuarter,
                        numberInQuarter,
                        quarterNumber
                );
            } else {
                log.warn("⚠️ Геометрия квартала не является Polygon, проверка пропущена");
            }
        }

        // Проверяем уникальность
        Optional<CuttingArea> existing = cuttingAreaRepository.findByForestryUnitIdAndNumberInQuarter(
                forestryUnitId, numberInQuarter);
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    String.format("❌ Деляна с номером '%s' уже существует в квартале %s!",
                            numberInQuarter,
                            forestryUnit.getNumber() != null ? forestryUnit.getNumber() : forestryUnit.getName())
            );
        }

        CuttingArea cuttingArea = new CuttingArea();
        cuttingArea.setNumberInQuarter(numberInQuarter);
        cuttingArea.setForestStand(plots);
        cuttingArea.setDescription(description);
        cuttingArea.setGeometry(normalizedGeometry); // ← Сохраняем нормализованную геометрию
        cuttingArea.setForestryUnit(forestryUnit);
        cuttingArea.setYearOfCut(yearOfCut);
        cuttingArea.setCutType(cutType);

        return saveWithValidation(cuttingArea);
    }

    @Transactional
    public List<IntersectionReport> saveWithValidation(CuttingArea cuttingArea) {
        // 1. Проверяем наличие геометрии
        if (cuttingArea.getGeometry() == null) {
            throw new IllegalArgumentException("Геометрия деляны не задана");
        }

        // 2. НОРМАЛИЗУЕМ ГЕОМЕТРИЮ (самое важное!)
        // Это гарантирует, что все полигоны будут сохранены в едином формате:
        // - против часовой стрелки
        // - с одинаковой начальной точкой (минимальная)
        // - без дублирующихся точек
        Polygon normalizedGeometry = geometryService.normalizePolygon(cuttingArea.getGeometry());
        cuttingArea.setGeometry(normalizedGeometry);

        // 3. Проверяем валидность геометрии после нормализации
        if (!geometryService.isValid(normalizedGeometry)) {
            throw new IllegalArgumentException(
                    "⚠️ Деляна имеет некорректную геометрию! Возможна 'бабочка' (самопересечение)."
            );
        }

        // 4. Проверяем обязательные поля
        if (cuttingArea.getNumberInQuarter() == null || cuttingArea.getNumberInQuarter().isEmpty()) {
            throw new IllegalArgumentException("Номер деляны в квартале обязателен!");
        }

        // 5. Проверяем уникальность номера деляны в квартале
        if (cuttingArea.getForestryUnit() != null) {
            Optional<CuttingArea> existing = cuttingAreaRepository.findByForestryUnitIdAndNumberInQuarter(
                    cuttingArea.getForestryUnit().getId(),
                    cuttingArea.getNumberInQuarter()
            );
            if (existing.isPresent() && !existing.get().getId().equals(cuttingArea.getId())) {
                throw new IllegalArgumentException(
                        String.format("❌ Деляна с номером '%s' уже существует в квартале %s!",
                                cuttingArea.getNumberInQuarter(),
                                cuttingArea.getForestryUnit().getNumber() != null ?
                                        cuttingArea.getForestryUnit().getNumber() :
                                        cuttingArea.getForestryUnit().getName())
                );
            }
        }

        // 6. Проверяем, что деляна находится внутри квартала
        if (cuttingArea.getForestryUnit() != null && cuttingArea.getForestryUnit().getGeometry() != null) {
            if (cuttingArea.getForestryUnit().getGeometry() instanceof Polygon) {
                // Нормализуем геометрию квартала для корректного сравнения
                Polygon normalizedQuarter = geometryService.normalizePolygon(
                        (Polygon) cuttingArea.getForestryUnit().getGeometry()
                );

                String quarterNumber = cuttingArea.getForestryUnit().getNumber() != null ?
                        cuttingArea.getForestryUnit().getNumber() :
                        cuttingArea.getForestryUnit().getName();

                // Используем номер деляны для сообщений
                String plotIdentifier = cuttingArea.getFullNumber() != null ?
                        cuttingArea.getFullNumber() :
                        cuttingArea.getNumberInQuarter();

                geometryService.validatePlotInsideQuarter(
                        normalizedGeometry,           // нормализованная деляна
                        normalizedQuarter,            // нормализованный квартал
                        plotIdentifier,
                        quarterNumber
                );
            } else {
                log.warn("⚠️ Геометрия квартала не является Polygon, проверка пропущена");
            }
        }

        // 7. Сохраняем деляну в БД
        CuttingArea saved = cuttingAreaRepository.save(cuttingArea);
        log.info("✅ Сохранена деляна: {} (ID: {}, площадь: {} га, территория: {})",
                saved.getFullNumber(),
                saved.getId(),
                saved.getAreaHa(),
                saved.getForestryUnit() != null ? saved.getForestryUnit().getFullPath() : "null");

        // 8. Проверяем пересечения с другими делянами
        List<IntersectionReport> conflicts = validatePlot(saved);

        // 9. Обновляем статус верификации
        if (conflicts.isEmpty()) {
            saved.setVerified(true);
            cuttingAreaRepository.save(saved);
            log.info("✅ Деляна {} верифицирована (пересечений нет)", saved.getFullNumber());
        } else {
            saved.setVerified(false);
            cuttingAreaRepository.save(saved);
            log.warn("⚠️ Деляна {} имеет {} пересечений", saved.getFullNumber(), conflicts.size());
        }

        return conflicts;
    }

    @Transactional
    public List<IntersectionReport> validatePlot(CuttingArea cuttingArea) {
        List<IntersectionReport> reports = new ArrayList<>();

        if (cuttingArea.getGeometry() == null) {
            return reports;
        }

        List<Object[]> results = cuttingAreaRepository.findIntersectionsWithCuttingArea(
                cuttingArea.getGeometry(),
                cuttingArea.getId(),
                minArea
        );

        for (Object[] row : results) {
            IntersectionReport report = new IntersectionReport();
            report.setPlot1Id(cuttingArea.getId());
            report.setPlot1Number(cuttingArea.getFullNumber());
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
        List<Object[]> results = cuttingAreaRepository.findAllIntersections(minArea);

        for (Object[] row : results) {
            IntersectionReport report = new IntersectionReport();
            report.setPlot1Id((Long) row[0]);
            report.setPlot2Id((Long) row[1]);
            report.setOverlapArea((Double) row[2]);

            CuttingArea cuttingArea1 = cuttingAreaRepository.findById(report.getPlot1Id()).orElse(null);
            CuttingArea cuttingArea2 = cuttingAreaRepository.findById(report.getPlot2Id()).orElse(null);

            if (cuttingArea1 != null) report.setPlot1Number(cuttingArea1.getFullNumber());
            if (cuttingArea2 != null) report.setPlot2Number(cuttingArea2.getFullNumber());

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
            List<CuttingArea> allCuttingAreas = cuttingAreaRepository.findAll();
            for (CuttingArea cuttingArea : allCuttingAreas) {
                cuttingArea.setVerified(true);
                cuttingAreaRepository.save(cuttingArea);
            }
            log.info("✅ Все деляны верифицированы (пересечений нет)");
        } else {
            for (IntersectionReport report : reports) {
                cuttingAreaRepository.findById(report.getPlot1Id()).ifPresent(plot -> {
                    plot.setVerified(false);
                    cuttingAreaRepository.save(plot);
                });
                cuttingAreaRepository.findById(report.getPlot2Id()).ifPresent(plot -> {
                    plot.setVerified(false);
                    cuttingAreaRepository.save(plot);
                });
            }
            log.warn("⚠️ Найдено {} пересечений, верификация снята с проблемных делян", reports.size());
        }

        return reports;
    }

    @Transactional
    public List<IntersectionReport> validatePlots(List<CuttingArea> cuttingAreas) {
        List<IntersectionReport> allReports = new ArrayList<>();

        for (CuttingArea cuttingArea : cuttingAreas) {
            if (cuttingArea.getGeometry() != null) {
                List<IntersectionReport> reports = validatePlot(cuttingArea);
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

        List<CuttingArea> cuttingAreas = excelImportService.parseExcel(file);
        log.info("Распаршено {} делян", cuttingAreas.size());

        List<CuttingArea> savedCuttingAreas = new ArrayList<>();
        List<IntersectionReport> allConflicts = new ArrayList<>();

        for (CuttingArea cuttingArea : cuttingAreas) {
            try {
                List<IntersectionReport> conflicts = saveWithValidation(cuttingArea);
                savedCuttingAreas.add(cuttingArea);
                allConflicts.addAll(conflicts);
            } catch (Exception e) {
                log.error("Ошибка при сохранении деляны {}: {}",
                        cuttingArea.getNumberInQuarter(), e.getMessage());
                throw new RuntimeException(
                        String.format("Ошибка при импорте деляны '%s': %s",
                                cuttingArea.getNumberInQuarter(), e.getMessage())
                );
            }
        }

        if (allConflicts.isEmpty()) {
            for (CuttingArea cuttingArea : savedCuttingAreas) {
                cuttingArea.setVerified(true);
                cuttingAreaRepository.save(cuttingArea);
            }
            log.info("Все деляны успешно импортированы и верифицированы");
        } else {
            log.warn("Импорт завершён с {} конфликтами", allConflicts.size());
        }

        return allConflicts;
    }

    @Transactional
    public int fixMissingTerritoryUnit() {
        List<CuttingArea> cuttingAreas = cuttingAreaRepository.findAll();
        int fixed = 0;

        for (CuttingArea cuttingArea : cuttingAreas) {
            if (cuttingArea.getForestryUnit() == null) {
                log.warn("⚠️ У деляны {} нет территориальной единицы", cuttingArea.getFullNumber());
            }
        }

        log.info("✅ Проверка завершена");
        return fixed;
    }
}