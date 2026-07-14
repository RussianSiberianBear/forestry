package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.CuttingAreaDto;
import com.alhrb.forestry.dto.ForestStandDto;
import com.alhrb.forestry.model.CuttingArea;
import com.alhrb.forestry.model.ForestStand;
import com.alhrb.forestry.service.CuttingAreaService;
import com.alhrb.forestry.service.ForestStandService;
import com.alhrb.forestry.user.UserUISettings;
import com.alhrb.forestry.user.UserUISettingsService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final CuttingAreaService cuttingAreaService;
    private final ForestStandService forestStandService;
    private final UserUISettingsService userUISettingsService;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        return "index";
    }

    @GetMapping("/cutting_area")
    public String cuttingArea(Model model) {
        UserUISettings uiSettings = userUISettingsService.getOrCreateSettings();
        List<CuttingArea> cuttingAreas = cuttingAreaService.findAll();

        // Заполняем territoryPath для каждого plot
        cuttingAreas.forEach(area -> area.setTerritoryPath(area.getTerritoryPath()));

        model.addAttribute("forestStand", "");
        model.addAttribute("cuttingAreaDto", new CuttingAreaDto());
        model.addAttribute("uiSettings", uiSettings);

        List<CuttingArea> latestCuttingAreas = cuttingAreas.stream()
                .sorted(Comparator.comparing(CuttingArea::getId).reversed())
                .limit(5)
                .toList();

        model.addAttribute("latestCuttingAreas", latestCuttingAreas);

        return "cutting-area";
    }

    @GetMapping("/forest-stand")
    public String forestStand(Model model) {
/*
        UserUISettings uiSettings = userUISettingsService.getOrCreateSettings();
        List<ForestStand> forestStands = forestStandService.findAll();

        // Заполняем territoryPath для каждого plot
        forestStands.forEach(stand -> stand.setTerritoryPath(stand.getTerritoryPath()));

        model.addAttribute("forestStandDto", new ForestStandDto());
        model.addAttribute("uiSettings", uiSettings);

        List<ForestStand> latestForestStands = forestStands.stream()
                .sorted(Comparator.comparing(ForestStand::getId).reversed())
                .limit(5)
                .toList();

        model.addAttribute("latestStands", latestForestStands);
*/
        return "forest-stand";
    }
}