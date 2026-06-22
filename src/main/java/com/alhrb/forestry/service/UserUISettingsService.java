package com.alhrb.forestry.service;

import com.alhrb.forestry.dto.UserUISettingsDto;
import com.alhrb.forestry.model.*;
import com.alhrb.forestry.repository.UserUISettingsRepository;
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

    private final UserUISettingsRepository settingsRepository;
    private final RegionService regionService;
    private final MunicipalDistrictService municipalDistrictService;
    private final ForestryService forestryService;
    private final DistrictForestryService districtForestryService;
    private final TechnicalUnitService technicalUnitService;
    private final QuarterService quarterService;

    private static final String SESSION_KEY = "UI_SETTINGS";

    // ==========================================
    // ПОЛУЧЕНИЕ НАСТРОЕК (ИЗ СЕССИИ)
    // ==========================================

    public UserUISettingsDto getSettings(HttpSession session) {
        // Пытаемся достать из сессии
        UserUISettingsDto settings = (UserUISettingsDto) session.getAttribute(SESSION_KEY);

        if (settings == null) {
            // Если в сессии нет — загружаем из БД или создаём дефолтные
            settings = loadFromDatabaseOrDefault();
            session.setAttribute(SESSION_KEY, settings);
            log.info("Настройки загружены в сессию");
        }

        return settings;
    }

    // ==========================================
    // СОХРАНЕНИЕ НАСТРОЕК (В СЕССИЮ И БД)
    // ==========================================

    public void saveSettings(HttpSession session, UserUISettingsDto settings) {
        // Сохраняем в сессию
        session.setAttribute(SESSION_KEY, settings);
        log.info("Настройки сохранены в сессию");

        // Сохраняем в БД (опционально, для постоянства)
        saveToDatabase(settings);
    }

    // ==========================================
    // СОХРАНЕНИЕ ОТДЕЛЬНЫХ УРОВНЕЙ
    // ==========================================

    public void saveRegion(HttpSession session, Long regionId) {
        UserUISettingsDto settings = getSettings(session);
        settings.setRegionId(regionId);

        // Сбрасываем нижестоящие уровни
        settings.setMunicipalDistrictId(null);
        settings.setForestryId(null);
        settings.setDistrictForestryId(null);
        settings.setTechnicalUnitId(null);
        settings.setQuarterId(null);

        // Обновляем координаты региона
        regionService.findById(regionId).ifPresent(region -> {
            settings.setCenterLat(region.getCenterLat());
            settings.setCenterLng(region.getCenterLng());
            settings.setZoom(region.getZoom());
        });

        saveSettings(session, settings);
    }

    public void saveMunicipalDistrict(HttpSession session, Long municipalDistrictId) {
        UserUISettingsDto settings = getSettings(session);
        settings.setMunicipalDistrictId(municipalDistrictId);
        // Сбрасываем нижестоящие уровни
        settings.setForestryId(null);
        settings.setDistrictForestryId(null);
        settings.setTechnicalUnitId(null);
        settings.setQuarterId(null);
        saveSettings(session, settings);
    }

    public void saveForestry(HttpSession session, Long forestryId) {
        UserUISettingsDto settings = getSettings(session);
        settings.setForestryId(forestryId);
        settings.setDistrictForestryId(null);
        settings.setTechnicalUnitId(null);
        settings.setQuarterId(null);
        saveSettings(session, settings);
    }

    public void saveDistrictForestry(HttpSession session, Long districtForestryId) {
        UserUISettingsDto settings = getSettings(session);
        settings.setDistrictForestryId(districtForestryId);
        settings.setTechnicalUnitId(null);
        settings.setQuarterId(null);
        saveSettings(session, settings);
    }

    public void saveTechnicalUnit(HttpSession session, Long technicalUnitId) {
        UserUISettingsDto settings = getSettings(session);
        settings.setTechnicalUnitId(technicalUnitId);
        settings.setQuarterId(null);
        saveSettings(session, settings);
    }

    public void saveQuarter(HttpSession session, Long quarterId) {
        UserUISettingsDto settings = getSettings(session);
        settings.setQuarterId(quarterId);
        saveSettings(session, settings);
    }

    // ==========================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ==========================================

    private UserUISettingsDto loadFromDatabaseOrDefault() {
        // Пытаемся загрузить из БД (для 'default' пользователя)
        Optional<UserUISettings> dbSettings = settingsRepository.findByUserId("default");

        if (dbSettings.isPresent()) {
            return convertToDto(dbSettings.get());
        }

        // Если в БД нет — создаём дефолтные (первый регион)
        return getDefaultSettings();
    }

    private UserUISettingsDto convertToDto(UserUISettings settings) {
        UserUISettingsDto dto = new UserUISettingsDto();

        if (settings.getLastRegion() != null) {
            dto.setRegionId(settings.getLastRegion().getId());
            dto.setCenterLat(settings.getLastRegion().getCenterLat());
            dto.setCenterLng(settings.getLastRegion().getCenterLng());
            dto.setZoom(settings.getLastRegion().getZoom());
        }
        if (settings.getLastMunicipalDistrict() != null) {
            dto.setMunicipalDistrictId(settings.getLastMunicipalDistrict().getId());
        }
        if (settings.getLastForestry() != null) {
            dto.setForestryId(settings.getLastForestry().getId());
        }
        if (settings.getLastDistrictForestry() != null) {
            dto.setDistrictForestryId(settings.getLastDistrictForestry().getId());
        }
        if (settings.getLastTechnicalUnit() != null) {
            dto.setTechnicalUnitId(settings.getLastTechnicalUnit().getId());
        }
        if (settings.getLastQuarter() != null) {
            dto.setQuarterId(settings.getLastQuarter().getId());
        }

        return dto;
    }

    private UserUISettingsDto getDefaultSettings() {
        UserUISettingsDto dto = new UserUISettingsDto();
        regionService.findAll().stream()
                .findFirst()
                .ifPresent(region -> {
                    dto.setRegionId(region.getId());
                    dto.setCenterLat(region.getCenterLat() != null ? region.getCenterLat() : 56.0);
                    dto.setCenterLng(region.getCenterLng() != null ? region.getCenterLng() : 92.0);
                    dto.setZoom(region.getZoom() != null ? region.getZoom() : 7);
                });
        return dto;
    }

    private void saveToDatabase(UserUISettingsDto dto) {
        UserUISettings settings = settingsRepository.findByUserId("default")
                .orElse(new UserUISettings());

        settings.setUserId("default");

        if (dto.getRegionId() != null) {
            settings.setLastRegion(regionService.findById(dto.getRegionId()).orElse(null));
        }
        if (dto.getMunicipalDistrictId() != null) {
            settings.setLastMunicipalDistrict(municipalDistrictService.findById(dto.getMunicipalDistrictId()).orElse(null));
        }
        if (dto.getForestryId() != null) {
            settings.setLastForestry(forestryService.findById(dto.getForestryId()).orElse(null));
        }
        if (dto.getDistrictForestryId() != null) {
            settings.setLastDistrictForestry(districtForestryService.findById(dto.getDistrictForestryId()).orElse(null));
        }
        if (dto.getTechnicalUnitId() != null) {
            settings.setLastTechnicalUnit(technicalUnitService.findById(dto.getTechnicalUnitId()).orElse(null));
        }
        if (dto.getQuarterId() != null) {
            settings.setLastQuarter(quarterService.findById(dto.getQuarterId()).orElse(null));
        }

        settingsRepository.save(settings);
        log.info("Настройки сохранены в БД");
    }
}
