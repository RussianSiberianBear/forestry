package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.TerritoryUnitDto;
import com.alhrb.forestry.model.TerritoryType;
import com.alhrb.forestry.model.TerritoryUnit;
import com.alhrb.forestry.service.TerritoryUnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/territory")
@RequiredArgsConstructor
@Slf4j
public class TerritoryUnitController {

    private final TerritoryUnitService territoryUnitService;

    // ===== ВСЕ РЕГИОНЫ =====
    @GetMapping("/regions")
    public ResponseEntity<List<TerritoryUnitDto>> getRegions() {
        log.info("📡 Запрос всех регионов");
        List<TerritoryUnit> regions = territoryUnitService.findByType(TerritoryType.REGION);
        log.info("📊 Найдено {} регионов", regions.size());

        List<TerritoryUnitDto> dtos = regions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== РАЙОНЫ ПО РЕГИОНУ =====
    @GetMapping("/municipal-districts/by-region/{regionId}")
    public ResponseEntity<List<TerritoryUnitDto>> getMunicipalDistrictsByRegion(@PathVariable Long regionId) {
        log.info("📡 Запрос районов для региона ID: {}", regionId);
        List<TerritoryUnit> districts = territoryUnitService.findByParentId(regionId);
        log.info("📊 Найдено {} районов", districts.size());

        List<TerritoryUnitDto> dtos = districts.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== ЛЕСНИЧЕСТВА ПО РАЙОНУ =====
    @GetMapping("/forestries/by-district/{districtId}")
    public ResponseEntity<List<TerritoryUnitDto>> getForestriesByDistrict(@PathVariable Long districtId) {
        log.info("📡 Запрос лесничеств для района ID: {}", districtId);
        List<TerritoryUnit> forestries = territoryUnitService.findByParentId(districtId);
        log.info("📊 Найдено {} лесничеств", forestries.size());

        List<TerritoryUnitDto> dtos = forestries.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== УЧАСТКОВЫЕ ЛЕСНИЧЕСТВА ПО ЛЕСНИЧЕСТВУ =====
    @GetMapping("/district-forestries/by-forestry/{forestryId}")
    public ResponseEntity<List<TerritoryUnitDto>> getDistrictForestriesByForestry(@PathVariable Long forestryId) {
        log.info("📡 Запрос участковых лесничеств для лесничества ID: {}", forestryId);
        List<TerritoryUnit> districtForestries = territoryUnitService.findByParentId(forestryId);
        log.info("📊 Найдено {} участковых лесничеств", districtForestries.size());

        List<TerritoryUnitDto> dtos = districtForestries.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== ТЕХНИЧЕСКИЕ УЧАСТКИ ПО УЧАСТКОВОМУ =====
    @GetMapping("/technical-units/by-district/{districtForestryId}")
    public ResponseEntity<List<TerritoryUnitDto>> getTechnicalUnitsByDistrict(@PathVariable Long districtForestryId) {
        log.info("📡 Запрос техучастков для участкового лесничества ID: {}", districtForestryId);
        List<TerritoryUnit> technicalUnits = territoryUnitService.findTechnicalUnit(districtForestryId);

        log.info("📊 Найдено {} техучастков", technicalUnits.size());

        List<TerritoryUnitDto> dtos = technicalUnits.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== КВАРТАЛЫ ПО ТЕХУЧАСТКУ =====
    @GetMapping("/quarters/by-technical/{technicalUnitId}")
    public ResponseEntity<List<TerritoryUnitDto>> getQuartersByTechnical(@PathVariable Long technicalUnitId) {
        log.info("📡 Запрос кварталов для техучастка ID: {}", technicalUnitId);
        List<TerritoryUnit> quarters = territoryUnitService.findByParentId(technicalUnitId);
        log.info("📊 Найдено {} кварталов", quarters.size());

        List<TerritoryUnitDto> dtos = quarters.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== ПОИСК КВАРТАЛОВ (AUTOCOMPLETE) =====
    @GetMapping("/quarters/search")
    public ResponseEntity<List<TerritoryUnitDto>> searchQuarters(
            @RequestParam(required = false) Long technicalUnitId,
            @RequestParam(required = false) Long parentId,
            @RequestParam String query) {

        // Если передан parentId - используем его, иначе technicalUnitId
        Long searchParentId = parentId != null ? parentId : technicalUnitId;

        log.info("📡 Поиск кварталов для родителя ID: {}, запрос: {}", searchParentId, query);
        List<TerritoryUnit> quarters = territoryUnitService.searchQuarters(searchParentId, query);

        List<TerritoryUnitDto> dtos = quarters.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== ПОЛУЧИТЬ ОДНУ ТЕРРИТОРИЮ =====
    @GetMapping("/{id}")
    public ResponseEntity<TerritoryUnitDto> getById(@PathVariable Long id) {
        log.info("📡 Запрос территории ID: {}", id);
        return territoryUnitService.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== МЕТОД ПРЕОБРАЗОВАНИЯ В DTO =====
    private TerritoryUnitDto toDto(TerritoryUnit unit) {
        if (unit == null) {
            return null;
        }

        TerritoryUnitDto dto = new TerritoryUnitDto();
        dto.setId(unit.getId());
        dto.setName(unit.getName());
        dto.setType(unit.getType().name());
        dto.setCode(unit.getCode());
        dto.setNumber(unit.getNumber());
        dto.setIsMain(unit.getIsMain());
        dto.setAreaHa(unit.getAreaHa());

        if (unit.getParent() != null) {
            dto.setParentId(unit.getParent().getId());
            dto.setParentName(unit.getParent().getName());
        }

        return dto;
    }
}