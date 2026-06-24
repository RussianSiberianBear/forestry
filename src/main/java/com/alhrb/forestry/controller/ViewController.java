package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.PlotDto;
import com.alhrb.forestry.dto.UserUISettingsDto;
import com.alhrb.forestry.service.PlotService;
import com.alhrb.forestry.service.UserUISettingsService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final PlotService plotService;
    private final UserUISettingsService userUISettingsService;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        // Получаем настройки UI
        UserUISettingsDto uiSettings = userUISettingsService.getSettings(session);

        model.addAttribute("plotDto", new PlotDto());
        model.addAttribute("plots", plotService.findAll());
        model.addAttribute("uiSettings", uiSettings);

        return "index";
    }

    @GetMapping("/mass-load")
    public String massLoad(Model model, HttpSession session) {
        // Получаем настройки UI
        UserUISettingsDto uiSettings = userUISettingsService.getSettings(session);

        model.addAttribute("plots", plotService.findAll());
        model.addAttribute("uiSettings", uiSettings);

        return "mass-load";
    }
}
