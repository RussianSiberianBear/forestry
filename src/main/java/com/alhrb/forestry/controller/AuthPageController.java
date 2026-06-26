package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.UserCreateDto;
import com.alhrb.forestry.model.User;
import com.alhrb.forestry.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthPageController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new UserCreateDto());
        return "register";
    }

    @PostMapping("/register")
    public String register(UserCreateDto dto, RedirectAttributes redirectAttributes) {
        try {
            userService.register(
                    dto.getUsername(),
                    dto.getEmail(),
                    dto.getPassword(),
                    dto.getFullName(),
                    dto.getPhone()
            );
            redirectAttributes.addAttribute("registered", true);
            return "redirect:/login";
        } catch (Exception e) {
            log.error("❌ Ошибка регистрации: {}", e.getMessage());
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }
}