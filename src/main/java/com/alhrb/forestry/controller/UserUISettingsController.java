package com.alhrb.forestry.controller;

import com.alhrb.forestry.model.UserUISettings;
import com.alhrb.forestry.service.UserUISettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ui-settings")
@RequiredArgsConstructor
@Slf4j
public class UserUISettingsController {

    private final UserUISettingsService userUISettingsService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            // Здесь нужно получить ID пользователя из SecurityContext
            // Например: return ((UserDetails) auth.getPrincipal()).getId();
            return 1L; // временно, пока не настроена аутентификация
        }
        return null;
    }

    @PostMapping("/{key}/{value}")
    public ResponseEntity<String> saveSetting(@PathVariable String key, @PathVariable String value) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().body("Пользователь не авторизован");
        }

        try {
            userUISettingsService.updateSetting(userId, key, value);
            log.info("✅ Настройка сохранена: {}={}", key, value);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("❌ Ошибка сохранения настройки: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
        }
    }

    @GetMapping("/current")
    public ResponseEntity<UserUISettings> getCurrentSettings() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.ok(new UserUISettings());
        }

        return userUISettingsService.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new UserUISettings()));
    }
}
