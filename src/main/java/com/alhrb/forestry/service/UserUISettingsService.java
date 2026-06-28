package com.alhrb.forestry.service;

import com.alhrb.forestry.model.User;
import com.alhrb.forestry.model.UserUISettings;
import com.alhrb.forestry.repository.UserUISettingsRepository;
import com.alhrb.forestry.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
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
    private final UserRepository userRepository;

    private static final String SESSION_USER_ID = "userId";

    // ===== ПОЛУЧИТЬ НАСТРОЙКИ ПО СЕССИИ =====
    public UserUISettings getOrCreateSettings(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);

        if (userId == null) {
            log.debug("Пользователь не авторизован, возвращаем пустые настройки");
            return new UserUISettings();
        }

        return userUISettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserUISettings newSettings = new UserUISettings();
                    newSettings.setUser(userRepository.findById(userId).orElse(null));
                    return userUISettingsRepository.save(newSettings);
                });
    }

    // ===== ОБНОВИТЬ НАСТРОЙКИ (через сессию) =====
    @Transactional
    public UserUISettings updateSetting(HttpSession session, String key, String value) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new RuntimeException("Пользователь не авторизован");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return updateSetting(user, key, value);
    }

    // ===== ОБНОВИТЬ НАСТРОЙКИ (через пользователя) =====
    @Transactional
    public UserUISettings updateSetting(User user, String key, String value) {
        UserUISettings settings = userUISettingsRepository.findByUser(user)
                .orElse(new UserUISettings());

        settings.setUser(user);

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

    // ===== ПОЛУЧИТЬ НАСТРОЙКИ ПО ID ПОЛЬЗОВАТЕЛЯ =====
    public Optional<UserUISettings> findByUserId(Long userId) {
        return userUISettingsRepository.findByUserId(userId);
    }

    // ===== ПОЛУЧИТЬ НАСТРОЙКИ ПО ПОЛЬЗОВАТЕЛЮ =====
    public Optional<UserUISettings> findByUser(User user) {
        return userUISettingsRepository.findByUser(user);
    }
}
