package com.alhrb.forestry.service;

import com.alhrb.forestry.model.UserUISettings;
import com.alhrb.forestry.repository.UserUISettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserUISettingsService {

    private final UserUISettingsRepository userUISettingsRepository;

    @Transactional
    public UserUISettings save(UserUISettings settings) {
        return userUISettingsRepository.save(settings);
    }

    public Optional<UserUISettings> findByUserId(Long userId) {
        return userUISettingsRepository.findByUserId(userId);
    }

    @Transactional
    public UserUISettings updateSetting(Long userId, String key, String value) {
        UserUISettings settings = userUISettingsRepository.findByUserId(userId)
                .orElse(new UserUISettings());

        settings.setUserId(userId);

        switch (key) {
            case "territory-unit":
                settings.setTerritoryUnitId(value != null && !value.equals("0") ? Long.parseLong(value) : null);
                break;
            case "territory-type":
                settings.setTerritoryType(value != null && !value.equals("0") ? value : null);
                break;
            case "center-lat":
                settings.setCenterLat(value != null ? Double.parseDouble(value) : null);
                break;
            case "center-lng":
                settings.setCenterLng(value != null ? Double.parseDouble(value) : null);
                break;
            case "zoom":
                settings.setZoom(value != null ? Integer.parseInt(value) : null);
                break;
            case "cut-type":
                settings.setCutType(value != null && !value.equals("0") ? value : null);
                break;
            case "year-of-cut":
                settings.setYearOfCut(value != null && !value.equals("0") ? Integer.parseInt(value) : null);
                break;
            default:
                log.warn("⚠️ Неизвестный ключ настройки: {}", key);
                return settings;
        }

        return userUISettingsRepository.save(settings);
    }
}