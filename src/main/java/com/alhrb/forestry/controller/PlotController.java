package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.IntersectionReport;
import com.alhrb.forestry.dto.PlotDto;
import com.alhrb.forestry.model.Plot;
import com.alhrb.forestry.service.GeometryService;
import com.alhrb.forestry.service.PlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/api/plots")
@RequiredArgsConstructor
@Slf4j
public class PlotController {

    private final PlotService plotService;
    private final GeometryService geometryService;

    @PostMapping("/create")
    public String createPlot(@Valid @ModelAttribute("plotDto") PlotDto plotDto,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("error", "Проверьте правильность заполнения формы");
            model.addAttribute("plots", plotService.findAll());
            return "index";
        }

        try {
            // Создаём полигон
            var geometry = geometryService.createPolygon(plotDto.getCoordinates());

            Plot plot = new Plot();
            plot.setPlotNumber(plotDto.getPlotNumber());
            plot.setForestryName(plotDto.getForestryName());
            plot.setDescription(plotDto.getDescription());
            plot.setGeometry(geometry);

            // Сохраняем с проверкой
            List<IntersectionReport> conflicts = plotService.saveWithValidation(plot);

            if (!conflicts.isEmpty()) {
                redirectAttributes.addFlashAttribute("conflicts", conflicts);
                redirectAttributes.addFlashAttribute("warning", "Обнаружены пересечения с существующими делянами!");
            } else {
                plot.setVerified(true);
                plotService.save(plot);
                redirectAttributes.addFlashAttribute("success", "Деляна успешно создана и верифицирована!");
            }

        } catch (IllegalArgumentException e) {
            log.error("Ошибка валидации: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());

        } catch (Exception e) {
            log.error("Ошибка при создании деляны", e);

            // Показываем понятное сообщение
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("character varying")) {
                errorMessage = "⚠️ Номер деляны слишком длинный (максимум 100 символов). " +
                        "Пожалуйста, сократите номер.";
            } else if (errorMessage != null && errorMessage.contains("geometry")) {
                errorMessage = "⚠️ Ошибка геометрии. Проверьте правильность координат.";
            }

            redirectAttributes.addFlashAttribute("error", "Ошибка: " + errorMessage);
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

    @PostMapping("/validate-all")
    public String validateAll(RedirectAttributes redirectAttributes) {
        try {
            List<IntersectionReport> conflicts = plotService.validateAllPlots();

            if (conflicts.isEmpty()) {
                redirectAttributes.addFlashAttribute("success", "Все деляны проверены, пересечений не обнаружено!");
            } else {
                redirectAttributes.addFlashAttribute("conflicts", conflicts);
                redirectAttributes.addFlashAttribute("warning", "Обнаружены пересечения между делянами!");
            }

        } catch (Exception e) {
            log.error("Ошибка при проверке", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при проверке: " + e.getMessage());
        }

        return "redirect:/";
    }
}
