package com.alhrb.forestry.service;

import com.alhrb.forestry.dto.IntersectionReport;
import com.alhrb.forestry.model.Plot;
import com.alhrb.forestry.model.Quarter;
import com.alhrb.forestry.repository.PlotRepository;
import com.alhrb.forestry.repository.QuarterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final QuarterRepository quarterRepository;
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

    public Plot findById(Long id) {
        return plotRepository.findById(id).orElse(null);
    }

    public Optional<Plot> findByFullNumber(String fullNumber) {
        return plotRepository.findByFullNumber(fullNumber);
    }

    public List<Plot> findByQuarterId(Long quarterId) {
        return plotRepository.findByQuarterIdOrderByNumberInQuarter(quarterId);
    }

    // ===== СОХРАНЕНИЕ С ПРОВЕРКОЙ =====

    @Transactional
    public List<IntersectionReport> saveWithValidation(Plot plot) {
        // 1. Проверка геометрии
        if (plot.getGeometry() == null) {
            throw new IllegalArgumentException("Геометрия деляны не задана");
        }

        // 2. Проверка на "бабочку"
        if (!geometryService.isValid(plot.getGeometry())) {
            throw new IllegalArgumentException(
                    "⚠️ Деляна имеет некорректную геометрию! " +
                            "Возможна 'бабочка' (самопересечение)."
            );
        }

        // 3. Проверка номера деляны
        if (plot.getNumberInQuarter() == null || plot.getNumberInQuarter().isEmpty()) {
            throw new IllegalArgumentException("Номер деляны в квартале обязателен!");
        }

        // 4. Проверка уникальности номера внутри квартала
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

        // 5. Проверка, что деляна внутри квартала
        if (plot.getQuarter() != null && plot.getQuarter().getGeometry() != null) {
            geometryService.validatePlotInsideQuarter(
                    plot.getGeometry(),
                    plot.getQuarter().getGeometry(),
                    plot.getFullNumber() != null ? plot.getFullNumber() : plot.getNumberInQuarter(),
                    plot.getQuarter().getNumber()
            );
        }

        // 6. Сохраняем
        Plot saved = plotRepository.save(plot);
        log.info("Сохранена деляна: {} (ID: {}, площадь: {} м²)",
                saved.getFullNumber(), saved.getId(), saved.getAreaM2());

        // 7. Проверяем пересечения
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
