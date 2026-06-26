package com.alhrb.forestry.controller;

import com.alhrb.forestry.model.UserUISettings;
import com.alhrb.forestry.service.UserUISettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ui-settings")
@RequiredArgsConstructor
@Slf4j
public class UserUISettingsController {

    private final UserUISettingsService userUISettingsService;

    @PostMapping("/{key}/{value}")
    public ResponseEntity<String> saveSetting(@PathVariable String key, @PathVariable String value) {
        try {
            userUISettingsService.updateSetting(key, value);
            log.info("✅ Настройка сохранена: {}={}", key, value);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("❌ Ошибка сохранения настройки: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
        }
    }

    @GetMapping("/current")
    public ResponseEntity<UserUISettings> getCurrentSettings() {
        try {
            UserUISettings settings = userUISettingsService.getOrCreateSettings();
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            log.error("❌ Ошибка получения настроек: {}", e.getMessage());
            return ResponseEntity.ok(new UserUISettings());
        }
    }
}