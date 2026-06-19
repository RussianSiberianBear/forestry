package com.alhrb.forestry.service;

import com.alhrb.forestry.dto.IntersectionReport;
import com.alhrb.forestry.model.Plot;
import com.alhrb.forestry.repository.PlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlotService {

    private final PlotRepository plotRepository;
    private final GeometryService geometryService;
    private final ExcelImportService excelImportService;

    @Value("${forest.validation.min-area:0.01}")
    private double minArea;

    @Transactional
    public Plot save(Plot plot) {
        try {
            return plotRepository.save(plot);
        } catch (Exception e) {
            log.error("Ошибка при сохранении деляны: {}", e.getMessage());
            throw new RuntimeException("Не удалось сохранить деляну: " + e.getMessage());
        }
    }

    @Transactional
    public List<IntersectionReport> saveWithValidation(Plot plot) {
        try {
            // Проверка геометрии
            if (plot.getGeometry() == null) {
                throw new IllegalArgumentException("Геометрия деляны не задана");
            }

            if (!geometryService.isValid(plot.getGeometry())) {
                throw new IllegalArgumentException(
                        "⚠️ Деляна имеет некорректную геометрию!\n" +
                                "Возможные причины:\n" +
                                "• Точки введены в неправильном порядке (получилась 'бабочка')\n" +
                                "• Есть самопересечения границ\n" +
                                "• Дублирующиеся точки\n" +
                                "Пожалуйста, проверьте порядок точек."
                );
            }

            // Сохраняем деляну
            Plot saved = plotRepository.save(plot);
            log.info("Сохранена деляна: {} (ID: {}, площадь: {} м²)",
                    saved.getPlotNumber(), saved.getId(), saved.getAreaM2());

            // Проверяем пересечения
            return validatePlot(saved);

        } catch (Exception e) {
            log.error("Ошибка при сохранении с валидацией: {}", e.getMessage());
            throw new RuntimeException("Ошибка при сохранении деляны: " + e.getMessage(), e);
        }
    }

    @Transactional
    public List<IntersectionReport> validatePlot(Plot plot) {
        List<IntersectionReport> reports = new ArrayList<>();

        if (plot.getGeometry() == null) {
            log.warn("Деляна {} не имеет геометрии", plot.getPlotNumber());
            return reports;
        }

        try {
            List<Object[]> results = plotRepository.findIntersectionsWithPlot(
                    plot.getGeometry(),
                    plot.getId(),
                    minArea
            );

            for (Object[] row : results) {
                IntersectionReport report = new IntersectionReport();
                report.setPlot1Id(plot.getId());
                report.setPlot1Number(plot.getPlotNumber());
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

            if (!reports.isEmpty()) {
                log.warn("Найдено {} пересечений для деляны {}", reports.size(), plot.getPlotNumber());
            }

        } catch (Exception e) {
            log.error("Ошибка при проверке пересечений: {}", e.getMessage());
            // Не прерываем выполнение, просто логируем ошибку
        }

        return reports;
    }

    @Transactional
    public List<IntersectionReport> validateAllPlots() {
        List<IntersectionReport> reports = new ArrayList<>();

        try {
            List<Object[]> results = plotRepository.findAllIntersections(minArea);

            for (Object[] row : results) {
                IntersectionReport report = new IntersectionReport();
                report.setPlot1Id((Long) row[0]);
                report.setPlot2Id((Long) row[1]);
                report.setOverlapArea((Double) row[2]);

                Plot plot1 = plotRepository.findById(report.getPlot1Id()).orElse(null);
                Plot plot2 = plotRepository.findById(report.getPlot2Id()).orElse(null);

                if (plot1 != null) report.setPlot1Number(plot1.getPlotNumber());
                if (plot2 != null) report.setPlot2Number(plot2.getPlotNumber());

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

        } catch (Exception e) {
            log.error("Ошибка при массовой проверке: {}", e.getMessage());
            throw new RuntimeException("Ошибка при проверке делян: " + e.getMessage(), e);
        }

        return reports;
    }

    public List<Plot> findAll() {
        return plotRepository.findAll();
    }

    public Plot findById(Long id) {
        return plotRepository.findById(id).orElse(null);
    }

    public Plot findByPlotNumber(String plotNumber) {
        return plotRepository.findByPlotNumber(plotNumber);
    }

    @Transactional
    public List<IntersectionReport> importFromExcel(MultipartFile file) {
        log.info("Начинаем импорт из Excel: {}", file.getOriginalFilename());

        try {
            List<Plot> plots = excelImportService.parseExcel(file);
            log.info("Распаршено {} делян", plots.size());

            List<Plot> savedPlots = new ArrayList<>();
            for (Plot plot : plots) {
                if (!geometryService.isValid(plot.getGeometry())) {
                    throw new IllegalArgumentException(
                            String.format("Деляна '%s' имеет некорректную геометрию (возможно 'бабочка')",
                                    plot.getPlotNumber())
                    );
                }
                Plot saved = plotRepository.save(plot);
                savedPlots.add(saved);
                log.debug("Сохранена деляна: {}", saved.getPlotNumber());
            }

            List<IntersectionReport> allConflicts = validateAllPlots();

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

        } catch (Exception e) {
            log.error("Ошибка при импорте из Excel: {}", e.getMessage());
            throw new RuntimeException("Ошибка при импорте: " + e.getMessage(), e);
        }
    }
}
