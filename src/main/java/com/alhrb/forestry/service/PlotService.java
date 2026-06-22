package com.alhrb.forestry.service;

import com.alhrb.forestry.dto.IntersectionReport;
import com.alhrb.forestry.model.*;
import com.alhrb.forestry.repository.PlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlotService {

    private final PlotRepository plotRepository;
    private final QuarterService quarterService;
    private final RegionService regionService;
    private final MunicipalDistrictService municipalDistrictService;
    private final ForestryService forestryService;
    private final DistrictForestryService districtForestryService;
    private final GeometryService geometryService;
    private final ExcelImportService excelImportService;

    @Value("${forest.validation.min-area:0.01}")
    private double minArea;

    // ===== БАЗОВЫЕ ОПЕРАЦИИ =====

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

    public List<Plot> findByQuarterId(Long quarterId) {
        return plotRepository.findByQuarterIdOrderByNumberInQuarter(quarterId);
    }

    public List<Plot> findByForestryId(Long forestryId) {
        return plotRepository.findByForestryId(forestryId);
    }

    public List<Plot> findByMunicipalDistrictId(Long municipalDistrictId) {
        return plotRepository.findByMunicipalDistrictId(municipalDistrictId);
    }

    public List<Plot> findByRegionId(Long regionId) {
        return plotRepository.findByRegionId(regionId);
    }

    // ===== СОЗДАНИЕ ДЕЛЯНЫ С ПРОВЕРКОЙ =====

    @Transactional
    public List<IntersectionReport> createPlotWithValidation(
            String numberInQuarter,
            String plots,
            String description,
            Polygon geometry,
            Long quarterId,
            Integer yearOfCut,
            String cutType) {

        // 1. Проверка геометрии
        if (geometry == null) {
            throw new IllegalArgumentException("Геометрия деляны не задана");
        }

        // 2. Проверка на "бабочку"
        if (!geometryService.isValid(geometry)) {
            throw new IllegalArgumentException(
                    "⚠️ Деляна имеет некорректную геометрию! " +
                            "Возможна 'бабочка' (самопересечение)."
            );
        }

        // 3. Получаем квартал
        Quarter quarter = quarterService.findById(quarterId)
                .orElseThrow(() -> new IllegalArgumentException("Квартал не найден"));

        // 4. Проверка, что деляна внутри квартала
        if (quarter.getGeometry() != null) {
            geometryService.validatePlotInsideQuarter(
                    geometry,
                    quarter.getGeometry(),
                    numberInQuarter,
                    quarter.getNumber()
            );
        }

        // 5. Проверка уникальности номера внутри квартала
        Optional<Plot> existing = plotRepository.findByQuarterIdAndNumberInQuarter(quarterId, numberInQuarter);
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                    String.format("❌ Деляна с номером '%s' уже существует в квартале %d!",
                            numberInQuarter, quarter.getNumber())
            );
        }

        // 6. Создаём деляну
        Plot plot = new Plot();
        plot.setNumberInQuarter(numberInQuarter);
        plot.setPlots(plots); // ← ТЕПЕРЬ РАБОТАЕТ!
        plot.setDescription(description);
        plot.setGeometry(geometry);
        plot.setQuarter(quarter);
        plot.setYearOfCut(yearOfCut);
        plot.setCutType(cutType);

        // 7. Заполняем иерархию из квартала
        if (quarter.getDistrictForestry() != null) {
            plot.setDistrictForestry(quarter.getDistrictForestry());

            if (quarter.getDistrictForestry().getForestry() != null) {
                plot.setForestry(quarter.getDistrictForestry().getForestry());

                if (quarter.getDistrictForestry().getForestry().getMunicipalDistrict() != null) {
                    plot.setMunicipalDistrict(quarter.getDistrictForestry().getForestry().getMunicipalDistrict());

                    if (quarter.getDistrictForestry().getForestry().getMunicipalDistrict().getRegion() != null) {
                        plot.setRegion(quarter.getDistrictForestry().getForestry().getMunicipalDistrict().getRegion());
                    }
                }
            }
        }

        // 8. Сохраняем
        Plot saved = plotRepository.save(plot);
        log.info("Сохранена деляна: {} (ID: {}, площадь: {} м²)",
                saved.getFullNumber(), saved.getId(), saved.getAreaM2());

        // 9. Проверяем пересечения
        return validatePlot(saved);
    }

    // ===== СТАРЫЙ МЕТОД ДЛЯ СОВМЕСТИМОСТИ =====

    @Transactional
    public List<IntersectionReport> saveWithValidation(Plot plot) {
        // Проверка геометрии
        if (plot.getGeometry() == null) {
            throw new IllegalArgumentException("Геометрия деляны не задана");
        }

        if (!geometryService.isValid(plot.getGeometry())) {
            throw new IllegalArgumentException(
                    "⚠️ Деляна имеет некорректную геометрию! " +
                            "Возможна 'бабочка' (самопересечение)."
            );
        }

        // Проверка номера
        if (plot.getNumberInQuarter() == null || plot.getNumberInQuarter().isEmpty()) {
            throw new IllegalArgumentException("Номер деляны в квартале обязателен!");
        }

        // Проверка уникальности
        if (plot.getQuarter() != null) {
            Optional<Plot> existing = plotRepository.findByQuarterIdAndNumberInQuarter(
                    plot.getQuarter().getId(),
                    plot.getNumberInQuarter()
            );
            if (existing.isPresent() && !existing.get().getId().equals(plot.getId())) {
                throw new IllegalArgumentException(
                        String.format("❌ Деляна с номером '%s' уже существует в квартале %d!",
                                plot.getNumberInQuarter(), plot.getQuarter().getNumber())
                );
            }
        }

        // Проверка внутри квартала
        if (plot.getQuarter() != null && plot.getQuarter().getGeometry() != null) {
            geometryService.validatePlotInsideQuarter(
                    plot.getGeometry(),
                    plot.getQuarter().getGeometry(),
                    plot.getFullNumber() != null ? plot.getFullNumber() : plot.getNumberInQuarter(),
                    plot.getQuarter().getNumber()
            );
        }

        // Заполняем иерархию из квартала, если не заполнена
        if (plot.getQuarter() != null) {
            Quarter quarter = plot.getQuarter();
            if (quarter.getDistrictForestry() != null) {
                plot.setDistrictForestry(quarter.getDistrictForestry());
                if (quarter.getDistrictForestry().getForestry() != null) {
                    plot.setForestry(quarter.getDistrictForestry().getForestry());
                    if (quarter.getDistrictForestry().getForestry().getMunicipalDistrict() != null) {
                        plot.setMunicipalDistrict(quarter.getDistrictForestry().getForestry().getMunicipalDistrict());
                        if (quarter.getDistrictForestry().getForestry().getMunicipalDistrict().getRegion() != null) {
                            plot.setRegion(quarter.getDistrictForestry().getForestry().getMunicipalDistrict().getRegion());
                        }
                    }
                }
            }
        }

        Plot saved = plotRepository.save(plot);
        log.info("Сохранена деляна: {} (ID: {}, площадь: {} м²)",
                saved.getFullNumber(), saved.getId(), saved.getAreaM2());

        return validatePlot(saved);
    }

    // ===== ВАЛИДАЦИЯ =====

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

        log.info("Проверка завершена. Найдено {} пересечений", reports.size());
        return reports;
    }

    // ===== ИМПОРТ ИЗ EXCEL =====

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
}
