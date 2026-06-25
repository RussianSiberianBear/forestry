package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.PlotDto;
import com.alhrb.forestry.dto.UserUISettingsDto;
import com.alhrb.forestry.model.Plot;
import com.alhrb.forestry.service.PlotService;
import com.alhrb.forestry.service.UserUISettingsService;
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

    private final PlotService plotService;
    private final UserUISettingsService userUISettingsService;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        return "index";
    }

    @GetMapping("/forest-ploat")
    public String forestPlot(Model model, HttpSession session) {
        // Получаем настройки UI
        UserUISettingsDto uiSettings = userUISettingsService.getSettings(session);
        List<Plot> plots = plotService.findAll();

        model.addAttribute("plots", plots);
        model.addAttribute("plotDto", new PlotDto());
        model.addAttribute("uiSettings", uiSettings);

        List<Plot> latestPlots = plots.stream()
                .sorted(Comparator.comparing(Plot::getId).reversed())
                .limit(5)
                .toList();

        model.addAttribute("latestPlots", latestPlots);

        return "forest-ploat";
    }

}
