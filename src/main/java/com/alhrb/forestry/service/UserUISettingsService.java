package com.alhrb.forestry.service;

import com.alhrb.forestry.model.User;
import com.alhrb.forestry.model.UserUISettings;
import com.alhrb.forestry.repository.UserUISettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserUISettingsService {

    private final UserUISettingsRepository userUISettingsRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Пользователь не авторизован");
        }
        return (User) auth.getPrincipal();
    }

    @Transactional
    public UserUISettings save(UserUISettings settings) {
        return userUISettingsRepository.save(settings);
    }

    public Optional<UserUISettings> findByUser(User user) {
        return userUISettingsRepository.findByUser(user);
    }

    public Optional<UserUISettings> findByUserId(Long userId) {
        return userUISettingsRepository.findByUserId(userId);
    }

    @Transactional
    public UserUISettings updateSetting(String key, String value) {
        User currentUser = getCurrentUser();
        UserUISettings settings = userUISettingsRepository.findByUser(currentUser)
                .orElse(new UserUISettings());

        settings.setUser(currentUser);

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

    @Transactional
    public UserUISettings getOrCreateSettings() {
        User currentUser = getCurrentUser();
        return userUISettingsRepository.findByUser(currentUser)
                .orElseGet(() -> {
                    UserUISettings settings = new UserUISettings();
                    settings.setUser(currentUser);
                    return userUISettingsRepository.save(settings);
                });
    }
}
