package com.alhrb.forestry.service;

import com.alhrb.forestry.dto.UserUISettingsDto;
import com.alhrb.forestry.model.Region;
import com.alhrb.forestry.model.UserUISettings;
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

    private final UserUISettingsRepository userUISettingsRepository;
    private final RegionService regionService;

    private static final String USER_ID_SESSION_KEY = "userId";

    // ==========================================
    // ПОЛУЧЕНИЕ НАСТРОЕК ПО СЕССИИ
    // ==========================================

    public UserUISettingsDto getSettings(HttpSession session) {
        Long userId = (Long) session.getAttribute(USER_ID_SESSION_KEY);
        return getSettings(userId);
    }

    // ==========================================
    // ПОЛУЧЕНИЕ НАСТРОЕК ПО ID ПОЛЬЗОВАТЕЛЯ
    // ==========================================

    public UserUISettingsDto getSettings(Long userId) {
        UserUISettings settings = userUISettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        return convertToDto(settings);
    }

    // ==========================================
    // СОЗДАНИЕ НАСТРОЕК ПО УМОЛЧАНИЮ
    // ==========================================

    @Transactional
    public UserUISettings createDefaultSettings(Long userId) {
        UserUISettings settings = new UserUISettings();
        settings.setUserId(userId);

        // Находим первый регион
        var regions = regionService.findAll();
        if (!regions.isEmpty()) {
            Region firstRegion = regions.get(0);
            settings.setRegionId(firstRegion.getId());
            settings.setCenterLat(firstRegion.getCenterLat());
            settings.setCenterLng(firstRegion.getCenterLng());
            settings.setZoom(firstRegion.getZoom() != null ? firstRegion.getZoom() : 6);
        } else {
            // Координаты по умолчанию для России
            settings.setCenterLat(56.0);
            settings.setCenterLng(92.0);
            settings.setZoom(4);
        }

        return userUISettingsRepository.save(settings);
    }

    // ==========================================
    // КОНВЕРТАЦИЯ В DTO
    // ==========================================

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

    // ==========================================
    // СОХРАНЕНИЕ НАСТРОЕК
    // ==========================================

    @Transactional
    public UserUISettingsDto saveSettings(UserUISettingsDto dto) {
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
        return convertToDto(saved);
    }

    public void saveRegion(HttpSession session, Long regionId) {
    }

    public void saveForestry(HttpSession session, Long forestryId) {
    }

    public void saveMunicipalDistrict(HttpSession session, Long municipalDistrictId) {
    }

    public void saveDistrictForestry(HttpSession session, Long districtForestryId) {
    }

    public void saveTechnicalUnit(HttpSession session, Long technicalUnitId) {
    }

    public void saveQuarter(HttpSession session, Long quarterId) {
    }
}
