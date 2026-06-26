package com.alhrb.forestry.controller;

import com.alhrb.forestry.service.*;
import com.alhrb.forestry.dto.UserUISettingsDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DictionaryController {

    private final RegionService regionService;
    private final MunicipalDistrictService municipalDistrictService;
    private final ForestryService forestryService;
    private final DistrictForestryService districtForestryService;
    private final TechnicalUnitService technicalUnitService;
    private final QuarterService quarterService;
    private final UserUISettingsService userUISettingsService;

    // ==========================================
    // СПРАВОЧНИКИ
    // ==========================================

    @GetMapping("/regions")
    public ResponseEntity<List<RegionDto>> getRegions() {
        List<Region> regions = regionService.findAll();
        List<RegionDto> dtos = regions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private RegionDto convertToDto(Region region) {
        RegionDto dto = new RegionDto();
        dto.setId(region.getId());
        dto.setName(region.getName());
        dto.setCenterLat(region.getCenterLat());
        dto.setCenterLng(region.getCenterLng());
        dto.setZoom(region.getZoom());
        return dto;
    }

    @GetMapping("/municipal-districts/by-region/{regionId}")
    public ResponseEntity<List<MunicipalDistrict>> getMunicipalDistricts(@PathVariable Long regionId) {
        return ResponseEntity.ok(municipalDistrictService.findByRegionId(regionId));
    }

    @GetMapping("/forestries/by-district/{municipalDistrictId}")
    public ResponseEntity<List<Forestry>> getForestries(@PathVariable Long municipalDistrictId) {
        return ResponseEntity.ok(forestryService.findByMunicipalDistrictId(municipalDistrictId));
    }

    @GetMapping("/district-forestries/by-forestry/{forestryId}")
    public ResponseEntity<List<DistrictForestry>> getDistrictForestries(@PathVariable Long forestryId) {
        return ResponseEntity.ok(districtForestryService.findByForestryId(forestryId));
    }

    @GetMapping("/technical-units/by-district/{districtForestryId}")
    public ResponseEntity<List<TechnicalUnit>> getTechnicalUnits(@PathVariable Long districtForestryId) {
        return ResponseEntity.ok(technicalUnitService.findByDistrictForestryId(districtForestryId));
    }

    @GetMapping("/quarters/search")
    public ResponseEntity<List<Quarter>> searchQuarters(
            @RequestParam Long technicalUnitId,
            @RequestParam String query) {
        return ResponseEntity.ok(quarterService.searchByTechnicalUnitAndNumber(technicalUnitId, query));
    }

    @GetMapping("/quarters/{id}")
    public ResponseEntity<Quarter> getQuarterById(@PathVariable Long id) {
        return quarterService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // НАСТРОЙКИ UI (СЕССИЯ)
    // ==========================================

    @GetMapping("/ui-settings")
    public ResponseEntity<UserUISettingsDto> getUISettings(HttpSession session) {
        return ResponseEntity.ok(userUISettingsService.getSettings(session));
    }

    @PostMapping("/ui-settings/region/{regionId}")
    public ResponseEntity<Void> saveRegion(HttpSession session, @PathVariable Long regionId) {
        userUISettingsService.saveRegion(session, regionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ui-settings/municipal-district/{municipalDistrictId}")
    public ResponseEntity<Void> saveMunicipalDistrict(HttpSession session, @PathVariable Long municipalDistrictId) {
        userUISettingsService.saveMunicipalDistrict(session, municipalDistrictId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ui-settings/forestry/{forestryId}")
    public ResponseEntity<Void> saveForestry(HttpSession session, @PathVariable Long forestryId) {
        userUISettingsService.saveForestry(session, forestryId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ui-settings/district-forestry/{districtForestryId}")
    public ResponseEntity<Void> saveDistrictForestry(HttpSession session, @PathVariable Long districtForestryId) {
        userUISettingsService.saveDistrictForestry(session, districtForestryId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ui-settings/technical-unit/{technicalUnitId}")
    public ResponseEntity<Void> saveTechnicalUnit(HttpSession session, @PathVariable Long technicalUnitId) {
        userUISettingsService.saveTechnicalUnit(session, technicalUnitId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ui-settings/quarter/{quarterId}")
    public ResponseEntity<Void> saveQuarter(HttpSession session, @PathVariable Long quarterId) {
        userUISettingsService.saveQuarter(session, quarterId);
        return ResponseEntity.ok().build();
    }
}
