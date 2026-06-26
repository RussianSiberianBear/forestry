package com.alhrb.forestry.controller;

import com.alhrb.forestry.model.TerritoryType;
import com.alhrb.forestry.model.TerritoryUnit;
import com.alhrb.forestry.service.TerritoryUnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/territory")
@RequiredArgsConstructor
@Slf4j
public class TerritoryUnitController {

    private final TerritoryUnitService territoryUnitService;

    // ===== ФЕДЕРАЛЬНЫЕ ОКРУГА =====
    @GetMapping("/federal-districts")
    public ResponseEntity<List<TerritoryUnit>> getFederalDistricts() {
        return ResponseEntity.ok(territoryUnitService.findByType(TerritoryType.FEDERAL_DISTRICT));
    }

    // ===== РЕГИОНЫ =====
    @GetMapping("/regions/by-federal/{federalDistrictId}")
    public ResponseEntity<List<TerritoryUnit>> getRegionsByFederalDistrict(@PathVariable Long federalDistrictId) {
        return ResponseEntity.ok(territoryUnitService.findByParentId(federalDistrictId));
    }

    @GetMapping("/regions")
    public ResponseEntity<List<TerritoryUnit>> getAllRegions() {
        return ResponseEntity.ok(territoryUnitService.findByType(TerritoryType.REGION));
    }

    // ===== РАЙОНЫ =====
    @GetMapping("/municipal-districts/by-region/{regionId}")
    public ResponseEntity<List<TerritoryUnit>> getMunicipalDistrictsByRegion(@PathVariable Long regionId) {
        return ResponseEntity.ok(territoryUnitService.findByParentId(regionId));
    }

    // ===== ЛЕСНИЧЕСТВА =====
    @GetMapping("/forestries/by-district/{districtId}")
    public ResponseEntity<List<TerritoryUnit>> getForestriesByDistrict(@PathVariable Long districtId) {
        return ResponseEntity.ok(territoryUnitService.findByParentId(districtId));
    }

    // ===== УЧАСТКОВЫЕ ЛЕСНИЧЕСТВА =====
    @GetMapping("/district-forestries/by-forestry/{forestryId}")
    public ResponseEntity<List<TerritoryUnit>> getDistrictForestriesByForestry(@PathVariable Long forestryId) {
        return ResponseEntity.ok(territoryUnitService.findByParentId(forestryId));
    }

    // ===== ТЕХНИЧЕСКИЕ УЧАСТКИ =====
    @GetMapping("/technical-units/by-district/{districtForestryId}")
    public ResponseEntity<List<TerritoryUnit>> getTechnicalUnitsByDistrict(@PathVariable Long districtForestryId) {
        return ResponseEntity.ok(territoryUnitService.findByParentId(districtForestryId));
    }

    // ===== КВАРТАЛЫ =====
    @GetMapping("/quarters/by-technical/{technicalUnitId}")
    public ResponseEntity<List<TerritoryUnit>> getQuartersByTechnical(@PathVariable Long technicalUnitId) {
        return ResponseEntity.ok(territoryUnitService.findByParentId(technicalUnitId));
    }

    // ===== ПОИСК КВАРТАЛОВ (AUTOCOMPLETE) =====
    @GetMapping("/quarters/search")
    public ResponseEntity<List<TerritoryUnit>> searchQuarters(
            @RequestParam Long technicalUnitId,
            @RequestParam String query) {
        return ResponseEntity.ok(territoryUnitService.searchQuarters(technicalUnitId, query));
    }

    // ===== ПОЛУЧИТЬ ОДНУ ТЕРРИТОРИАЛЬНУЮ ЕДИНИЦУ =====
    @GetMapping("/{id}")
    public ResponseEntity<TerritoryUnit> getById(@PathVariable Long id) {
        return territoryUnitService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}