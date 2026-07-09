package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.IntersectionReport;
import com.alhrb.forestry.dto.CuttingAreaDto;
import com.alhrb.forestry.dto.CuttingAreaMapDto;
import com.alhrb.forestry.model.CuttingArea;
import com.alhrb.forestry.model.ForestryUnit;
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
@RequestMapping("/api/cutting-area")
@RequiredArgsConstructor
@Slf4j
public class CuttingAreaController {

    private final CuttingAreaService cuttingAreaService;
    private final GeometryService geometryService;
    private final ForestryUnitService forestryUnitService;  // ← вместо QuarterService

    @PostMapping("/create")
    public String createCuttingArea(@Valid @ModelAttribute("cuttingAreaDto") CuttingAreaDto cuttingAreaDto,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("error", "Проверьте правильность заполнения формы");
            model.addAttribute("cuttingAreaDto", new CuttingAreaDto());
            model.addAttribute("forestStand", cuttingAreaService.findAll());
            return "cutting-area";
        }

        try {
            var geometry = geometryService.createPolygon(cuttingAreaDto.getCoordinates());

            if (cuttingAreaDto.getQuarterId() == null) {
                throw new IllegalArgumentException("Не выбран квартал!");
            }

            // Получаем территориальную единицу (квартал)
            ForestryUnit forestryUnit = forestryUnitService.findById(cuttingAreaDto.getQuarterId())
                    .orElseThrow(() -> new IllegalArgumentException("Квартал не найден"));

            // Проверяем, что это квартал
            if (!forestryUnit.isQuarter()) {
                throw new IllegalArgumentException("Выбранная территория не является кварталом!");
            }

            if (forestryUnit.getGeometry() != null) {
                geometryService.validatePlotInsideQuarter(
                        geometry,
                        (Polygon) forestryUnit.getGeometry(),
                        cuttingAreaDto.getNumberInQuarter(),
                        forestryUnit.getNumber() != null ? forestryUnit.getNumber() : forestryUnit.getName()
                );
            }

            List<IntersectionReport> conflicts = cuttingAreaService.createPlotWithValidation(
                    cuttingAreaDto.getNumberInQuarter(),
                    cuttingAreaDto.getForestStand(),
                    cuttingAreaDto.getDescription(),
                    geometry,
                    cuttingAreaDto.getQuarterId(),
                    cuttingAreaDto.getYearOfCut(),
                    cuttingAreaDto.getCutType()
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
            List<IntersectionReport> conflicts = cuttingAreaService.importFromExcel(file);

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
    public ResponseEntity<List<CuttingArea>> getAllCuttingAreas() {
        return ResponseEntity.ok(cuttingAreaService.findAll());
    }

    @GetMapping("/map-data")
    @ResponseBody
    public ResponseEntity<List<CuttingAreaMapDto>> getCuttingAreasForMap() {
        return ResponseEntity.ok(cuttingAreaService.getAllCuttingAreasForMap());
    }

    /**
     * Эндпоинт для получения отфильтрованных делян для карты
     */
    @GetMapping("/map-data-filtered")
    @ResponseBody
    public ResponseEntity<List<CuttingAreaMapDto>> getFilteredCuttingAreasForMap(
            @RequestParam(required = false) Long federalDistrictId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long municipalDistrictId,
            @RequestParam(required = false) Long forestryId,
            @RequestParam(required = false) Long subForestryId,
            @RequestParam(required = false) Long technicalUnitId,
            @RequestParam(required = false) Long quarterId,
            @RequestParam(required = false) String numberInQuarter,
            @RequestParam(required = false) String cutType,
            @RequestParam(required = false) Integer yearOfCut) {

        List<CuttingAreaMapDto> plots = cuttingAreaService.getFilteredCuttingAreasForMap(
                federalDistrictId, regionId, municipalDistrictId,
                forestryId, subForestryId, technicalUnitId,
                quarterId, numberInQuarter, cutType, yearOfCut
        );

        log.info("📊 Найдено {} делян по фильтру", plots.size());
        return ResponseEntity.ok(plots);
    }

    @PostMapping("/validate-all")
    @ResponseBody
    public ResponseEntity<List<IntersectionReport>> validateAll() {
        List<IntersectionReport> conflicts = cuttingAreaService.validateAllPlots();
        return ResponseEntity.ok(conflicts);
    }

    @PostMapping("/validate-by-territory")
    @ResponseBody
    public ResponseEntity<List<IntersectionReport>> validateByTerritory(
            @RequestParam String type,
            @RequestParam Long id) {

        List<CuttingArea> cuttingAreas = new ArrayList<>();
        String territoryName = "";

        // Используем рекурсивные методы для поиска по иерархии
        switch (type) {
            case "REGION":
                cuttingAreas = cuttingAreaService.findByTerritoryUnitRecursive(id);
                territoryName = "региону";
                break;
            case "MUNICIPAL_DISTRICT":
                cuttingAreas = cuttingAreaService.findByTerritoryUnitRecursive(id);
                territoryName = "муниципальному району";
                break;
            case "FORESTRY":
                cuttingAreas = cuttingAreaService.findByForestryUnitRecursive(id);
                territoryName = "лесничеству";
                break;
            case "SUB_FORESTRY":
                cuttingAreas = cuttingAreaService.findByForestryUnitRecursive(id);
                territoryName = "участковому лесничеству";
                break;
            case "TECHNICAL_UNIT":
                cuttingAreas = cuttingAreaService.findByForestryUnitRecursive(id);
                territoryName = "техническому участку";
                break;
            case "FOREST_QUARTER":
                cuttingAreas = cuttingAreaService.findByForestryUnitId(id);
                territoryName = "кварталу";
                break;
            default:
                throw new IllegalArgumentException("Неизвестный тип территории: " + type);
        }

        log.info("🔍 Проверка делян по {} (ID: {})", territoryName, id);
        log.info("📊 Найдено {} делян для проверки", cuttingAreas.size());

        List<IntersectionReport> conflicts = cuttingAreaService.validatePlots(cuttingAreas);

        if (conflicts.isEmpty()) {
            for (CuttingArea cuttingArea : cuttingAreas) {
                cuttingArea.setVerified(true);
                cuttingAreaService.save(cuttingArea);
            }
            log.info("✅ Все деляны по территории проверены, пересечений нет");
        } else {
            for (IntersectionReport report : conflicts) {
                cuttingAreaService.findById(report.getPlot1Id()).ifPresent(plot -> {
                    plot.setVerified(false);
                    cuttingAreaService.save(plot);
                });
                cuttingAreaService.findById(report.getPlot2Id()).ifPresent(plot -> {
                    plot.setVerified(false);
                    cuttingAreaService.save(plot);
                });
            }
            log.warn("⚠️ Найдено {} пересечений по территории", conflicts.size());
        }

        return ResponseEntity.ok(conflicts);
    }
}