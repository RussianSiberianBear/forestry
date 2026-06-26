package com.alhrb.forestry.service;

import com.alhrb.forestry.dto.UserUISettingsDto;
import com.alhrb.forestry.model.UserUISettings;
import com.alhrb.forestry.repository.UserUISettingsRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
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
    private final RegionService regionService;

    private static final String USER_ID_SESSION_KEY = "userId";

    // ==========================================
    // ПРИ СТАРТЕ — СОЗДАЁМ ДЕФОЛТНУЮ ЗАПИСЬ, ЕСЛИ НЕТ НИ ОДНОЙ
    // ==========================================

    @PostConstruct
    @Transactional
    public void initDefaultSettings() {
        long count = userUISettingsRepository.count();
        if (count == 0) {
            log.info("⚠️ В таблице user_ui_settings нет записей. Создаём дефолтную.");

            UserUISettings settings = new UserUISettings();
            settings.setUserId(1L);

            var regions = regionService.findAll();
            if (!regions.isEmpty()) {
                Region firstRegion = regions.get(0);
                settings.setRegionId(firstRegion.getId());
                settings.setCenterLat(firstRegion.getCenterLat());
                settings.setCenterLng(firstRegion.getCenterLng());
                settings.setZoom(firstRegion.getZoom() != null ? firstRegion.getZoom() : 6);
            } else {
                settings.setCenterLat(56.0);
                settings.setCenterLng(92.0);
                settings.setZoom(6);
            }

            userUISettingsRepository.save(settings);
            log.info("✅ Создана дефолтная запись в user_ui_settings");
        } else {
            log.info("✅ В таблице user_ui_settings уже есть {} записей", count);
        }
    }

    // ==========================================
    // ПОЛУЧЕНИЕ НАСТРОЕК ПО СЕССИИ
    // ==========================================

    public UserUISettingsDto getSettings(HttpSession session) {
        Long userId = getUserIdFromSession(session);
        return getSettings(userId);
    }

    // ==========================================
    // ПОЛУЧЕНИЕ НАСТРОЕК ПО ID ПОЛЬЗОВАТЕЛЯ
    // ==========================================

    @Transactional
    public UserUISettingsDto getSettings(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("❌ userId не может быть null");
        }

        Optional<UserUISettings> existing = userUISettingsRepository.findByUserId(userId);

        if (existing.isPresent()) {
            log.debug("✅ Настройки найдены для userId: {}", userId);
            return convertToDto(existing.get());
        } else {
            log.info("⚠️ Настройки НЕ найдены для userId: {}. Создаём новые.", userId);
            UserUISettings newSettings = createDefaultSettings(userId);
            return convertToDto(newSettings);
        }
    }

    // ==========================================
    // СОЗДАНИЕ НАСТРОЕК ПО УМОЛЧАНИЮ
    // ==========================================

    @Transactional
    public UserUISettings createDefaultSettings(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("❌ userId не может быть null при создании настроек");
        }

        UserUISettings settings = new UserUISettings();
        settings.setUserId(userId);  // ← ОБЯЗАТЕЛЬНО!

        var regions = regionService.findAll();
        if (!regions.isEmpty()) {
            Region firstRegion = regions.get(0);
            settings.setRegionId(firstRegion.getId());
            settings.setCenterLat(firstRegion.getCenterLat());
            settings.setCenterLng(firstRegion.getCenterLng());
            settings.setZoom(firstRegion.getZoom() != null ? firstRegion.getZoom() : 6);
        } else {
            settings.setCenterLat(56.0);
            settings.setCenterLng(92.0);
            settings.setZoom(6);
        }

        log.info("✅ Созданы настройки UI для пользователя {}", userId);
        return userUISettingsRepository.save(settings);
    }

    // ==========================================
    // СОХРАНЕНИЕ НАСТРОЕК
    // ==========================================

    @Transactional
    public UserUISettingsDto saveSettings(UserUISettingsDto dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("❌ userId не может быть null при сохранении настроек");
        }

        UserUISettings settings = userUISettingsRepository.findByUserId(dto.getUserId())
                .orElse(new UserUISettings());

        settings.setUserId(dto.getUserId());
        settings.setRegionId(dto.getRegionId());
        settings.setMunicipalDistrictId(dto.getMunicipalDistrictId());
        settings.setForestryId(dto.getForestryId());
        settings.setDistrictForestryId(dto.getDistrictForestryId());
        settings.setTechnicalUnitId(dto.getTechnicalUnitId());
        settings.setQuarterId(dto.getQuarterId());
        settings.setCenterLat(dto.getCenterLat());
        settings.setCenterLng(dto.getCenterLng());
        settings.setZoom(dto.getZoom());

        UserUISettings saved = userUISettingsRepository.save(settings);
        log.info("✅ Сохранены настройки UI для пользователя {}", dto.getUserId());
        return convertToDto(saved);
    }

    // ==========================================
    // УДАЛЕНИЕ НАСТРОЕК
    // ==========================================

    @Transactional
    public void deleteSettings(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("❌ userId не может быть null при удалении настроек");
        }
        userUISettingsRepository.deleteByUserId(userId);
        log.info("🗑️ Удалены настройки UI для пользователя {}", userId);
    }

    // ==========================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ==========================================

    private Long getUserIdFromSession(HttpSession session) {
        Long userId = (Long) session.getAttribute(USER_ID_SESSION_KEY);

        if (userId == null) {
            // Временно: используем userId = 1
            userId = 1L;
            session.setAttribute(USER_ID_SESSION_KEY, userId);
            log.info("👤 Установлен userId в сессии: {}", userId);
        }

        return userId;
    }

    private UserUISettingsDto convertToDto(UserUISettings settings) {
        UserUISettingsDto dto = new UserUISettingsDto();
        dto.setId(settings.getId());
        dto.setUserId(settings.getUserId());
        dto.setRegionId(settings.getRegionId());
        dto.setMunicipalDistrictId(settings.getMunicipalDistrictId());
        dto.setForestryId(settings.getForestryId());
        dto.setDistrictForestryId(settings.getDistrictForestryId());
        dto.setTechnicalUnitId(settings.getTechnicalUnitId());
        dto.setQuarterId(settings.getQuarterId());
        dto.setCenterLat(settings.getCenterLat());
        dto.setCenterLng(settings.getCenterLng());
        dto.setZoom(settings.getZoom());
        return dto;
    }

    public void saveRegion(HttpSession session, Long regionId) {
    }

    public void saveMunicipalDistrict(HttpSession session, Long municipalDistrictId) {
    }

    public void saveForestry(HttpSession session, Long forestryId) {
    }

    public void saveDistrictForestry(HttpSession session, Long districtForestryId) {
    }

    public void saveTechnicalUnit(HttpSession session, Long technicalUnitId) {
    }

    public void saveQuarter(HttpSession session, Long quarterId) {
    }
}