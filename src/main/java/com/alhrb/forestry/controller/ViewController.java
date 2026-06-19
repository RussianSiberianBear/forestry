package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.PlotDto;
import com.alhrb.forestry.service.PlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final PlotService plotService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("plotDto", new PlotDto());
        model.addAttribute("plots", plotService.findAll());
        return "index";
    }

    @GetMapping("/mass-load")
    public String massLoad(Model model) {
        model.addAttribute("plots", plotService.findAll());
        return "mass-load";
    }
}
