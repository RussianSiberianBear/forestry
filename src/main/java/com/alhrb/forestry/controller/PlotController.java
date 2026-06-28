package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.IntersectionReport;
import com.alhrb.forestry.dto.PlotDto;
import com.alhrb.forestry.dto.PlotMapDto;
import com.alhrb.forestry.model.ForestryUnit;
import com.alhrb.forestry.model.Plot;
import com.alhrb.forestry.model.TerritoryUnit;
import com.alhrb.forestry.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/api/plots")
@RequiredArgsConstructor
@Slf4j
public class PlotController {

    private final PlotService plotService;
    private final GeometryService geometryService;
    private final ForestryUnitService forestryUnitService;  // ← вместо QuarterService

    @PostMapping("/create")
    public String createPlot(@Valid @ModelAttribute("plotDto") PlotDto plotDto,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("error", "Проверьте правильность заполнения формы");
            model.addAttribute("plots", plotService.findAll());
            return "forest-ploat";
        }

        try {
            var geometry = geometryService.createPolygon(plotDto.getCoordinates());

            if (plotDto.getTerritoryUnitId() == null) {
                throw new IllegalArgumentException("Не выбран квартал!");
            }

            // Получаем территориальную единицу (квартал)
            ForestryUnit territoryUnit = forestryUnitService.findById(plotDto.getTerritoryUnitId())
                    .orElseThrow(() -> new IllegalArgumentException("Квартал не найден"));

            // Проверяем, что это квартал
            if (!territoryUnit.isQuarter()) {
                throw new IllegalArgumentException("Выбранная территория не является кварталом!");
            }

            if (territoryUnit.getGeometry() != null) {
                geometryService.validatePlotInsideQuarter(
                        geometry,
                        (Polygon) territoryUnit.getGeometry(),
                        plotDto.getNumberInQuarter(),
                        territoryUnit.getNumber() != null ? territoryUnit.getNumber() : territoryUnit.getName()
                );
            }

            List<IntersectionReport> conflicts = plotService.createPlotWithValidation(
                    plotDto.getNumberInQuarter(),
                    plotDto.getPlots(),
                    plotDto.getDescription(),
                    geometry,
                    plotDto.getTerritoryUnitId(),
                    plotDto.getYearOfCut(),
                    plotDto.getCutType()
            );

            if (!conflicts.isEmpty()) {
                redirectAttributes.addFlashAttribute("conflicts", conflicts);
                redirectAttributes.addFlashAttribute("warning", "Обнаружены пересечения с существующими делянами!");
            } else {
                redirectAttributes.addFlashAttribute("success", "Деляна успешно создана и верифицирована!");
            }

        } catch (IllegalArgumentException e) {
            log.error("Ошибка валидации: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());

        } catch (Exception e) {
            log.error("Ошибка при создании деляны", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/mass-import")
    public String massImport(@RequestParam("file") MultipartFile file,
                             RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Пожалуйста, выберите файл");
            return "redirect:/mass-load";
        }

        try {
            List<IntersectionReport> conflicts = plotService.importFromExcel(file);

            if (conflicts.isEmpty()) {
                redirectAttributes.addFlashAttribute("success", "Все деляны успешно загружены и проверены!");
            } else {
                redirectAttributes.addFlashAttribute("conflicts", conflicts);
                redirectAttributes.addFlashAttribute("warning", "Обнаружены пересечения между делянами!");
            }

        } catch (Exception e) {
            log.error("Ошибка при массовой загрузке", e);

            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("character varying")) {
                errorMessage = "⚠️ Номер одной из делян слишком длинный (максимум 100 символов). " +
                        "Пожалуйста, сократите номер в Excel.";
            }

            redirectAttributes.addFlashAttribute("error", "Ошибка при загрузке файла: " + errorMessage);
        }

        return "redirect:/mass-load";
    }

    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<List<Plot>> getAllPlots() {
        return ResponseEntity.ok(plotService.findAll());
    }

    @GetMapping("/map-data")
    @ResponseBody
    public ResponseEntity<List<PlotMapDto>> getPlotsForMap() {
        return ResponseEntity.ok(plotService.getAllPlotsForMap());
    }

    /**
     * Эндпоинт для получения отфильтрованных делян для карты
     */
    @GetMapping("/map-data-filtered")
    @ResponseBody
    public ResponseEntity<List<PlotMapDto>> getFilteredPlotsForMap(
            @RequestParam(required = false) Long federalDistrictId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long municipalDistrictId,
            @RequestParam(required = false) Long forestryId,
            @RequestParam(required = false) Long districtForestryId,
            @RequestParam(required = false) Long technicalUnitId,
            @RequestParam(required = false) Long quarterId,
            @RequestParam(required = false) String cutType,
            @RequestParam(required = false) Integer yearOfCut) {

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

        List<PlotMapDto> plots = plotService.getFilteredPlotsForMap(
                federalDistrictId, regionId, municipalDistrictId,
                forestryId, districtForestryId, technicalUnitId,
                quarterId, cutType, yearOfCut
        );

        log.info("📊 Найдено {} делян по фильтру", plots.size());
        return ResponseEntity.ok(plots);
    }

    @PostMapping("/validate-all")
    @ResponseBody
    public ResponseEntity<List<IntersectionReport>> validateAll() {
        List<IntersectionReport> conflicts = plotService.validateAllPlots();
        return ResponseEntity.ok(conflicts);
    }

    @PostMapping("/validate-by-territory")
    @ResponseBody
    public ResponseEntity<List<IntersectionReport>> validateByTerritory(
            @RequestParam String type,
            @RequestParam Long id) {

        List<Plot> plots = new ArrayList<>();
        String territoryName = "";

        // Используем рекурсивные методы для поиска по иерархии
        switch (type) {
            case "REGION":
                plots = plotService.findByTerritoryUnitRecursive(id);
                territoryName = "региону";
                break;
            case "MUNICIPAL_DISTRICT":
                plots = plotService.findByTerritoryUnitRecursive(id);
                territoryName = "муниципальному району";
                break;
            case "FORESTRY":
                plots = plotService.findByForestryUnitRecursive(id);
                territoryName = "лесничеству";
                break;
            case "DISTRICT_FORESTRY":
                plots = plotService.findByForestryUnitRecursive(id);
                territoryName = "участковому лесничеству";
                break;
            case "TECHNICAL_UNIT":
                plots = plotService.findByForestryUnitRecursive(id);
                territoryName = "техническому участку";
                break;
            case "QUARTER":
                plots = plotService.findByForestryUnitId(id);
                territoryName = "кварталу";
                break;
            default:
                throw new IllegalArgumentException("Неизвестный тип территории: " + type);
        }

        log.info("🔍 Проверка делян по {} (ID: {})", territoryName, id);
        log.info("📊 Найдено {} делян для проверки", plots.size());

        List<IntersectionReport> conflicts = plotService.validatePlots(plots);

        if (conflicts.isEmpty()) {
            for (Plot plot : plots) {
                plot.setVerified(true);
                plotService.save(plot);
            }
            log.info("✅ Все деляны по территории проверены, пересечений нет");
        } else {
            for (IntersectionReport report : conflicts) {
                plotService.findById(report.getPlot1Id()).ifPresent(plot -> {
                    plot.setVerified(false);
                    plotService.save(plot);
                });
                plotService.findById(report.getPlot2Id()).ifPresent(plot -> {
                    plot.setVerified(false);
                    plotService.save(plot);
                });
            }
            log.warn("⚠️ Найдено {} пересечений по территории", conflicts.size());
        }

        return ResponseEntity.ok(conflicts);
    }
}