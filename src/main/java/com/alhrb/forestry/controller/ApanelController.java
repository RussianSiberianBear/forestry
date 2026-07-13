package com.alhrb.forestry.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@PreAuthorize("hasAnyRole('SUPERADMIN',ADMIN')")
public class ApanelController {

    @GetMapping("/apanel")
    public String apanel(Model model) {

        return "admin/apanel";
    }


}
