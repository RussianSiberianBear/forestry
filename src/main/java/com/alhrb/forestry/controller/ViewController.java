package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.PlotDto;
import com.alhrb.forestry.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final PlotService plotService;
    private final RegionService regionService;
    private final MunicipalDistrictService municipalDistrictService;
    private final ForestryService forestryService;
    private final DistrictForestryService districtForestryService;
    private final QuarterService quarterService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("plotDto", new PlotDto());
        model.addAttribute("plots", plotService.findAll());
        model.addAttribute("regions", regionService.findAll());
        // Остальные списки загружаются через AJAX
        return "index";
    }

    @GetMapping("/mass-load")
    public String massLoad(Model model) {
        model.addAttribute("plots", plotService.findAll());
        return "mass-load";
    }
}
