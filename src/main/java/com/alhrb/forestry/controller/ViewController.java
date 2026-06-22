package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.PlotDto;
import com.alhrb.forestry.dto.UserUISettingsDto;
import com.alhrb.forestry.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final PlotService plotService;
    private final RegionService regionService;
    private final UserUISettingsService userUISettingsService;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        model.addAttribute("plotDto", new PlotDto());
        model.addAttribute("plots", plotService.findAll());
        model.addAttribute("regions", regionService.findAll());

        // ===== ЗАГРУЖАЕМ НАСТРОЙКИ ИЗ СЕССИИ =====
        UserUISettingsDto settings = userUISettingsService.getSettings(session);
        model.addAttribute("uiSettings", settings);

        return "index";
    }

    @GetMapping("/mass-load")
    public String massLoad(Model model) {
        model.addAttribute("plots", plotService.findAll());
        return "mass-load";
    }
}
