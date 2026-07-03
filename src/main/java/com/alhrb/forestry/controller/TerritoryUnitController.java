package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.ForestryUnitDto;
import com.alhrb.forestry.dto.TerritoryUnitDto;
import com.alhrb.forestry.model.ForestryUnit;
import com.alhrb.forestry.model.TerritoryType;
import com.alhrb.forestry.model.TerritoryUnit;
import com.alhrb.forestry.service.ForestryUnitService;
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
        List<TerritoryUnit> regions = territoryUnitService.findByTypeOrderByName(TerritoryType.REGION);
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
        List<TerritoryUnit> districts = territoryUnitService.findMunicipalDistrictsByRegion(regionId);
        log.info("📊 Найдено {} районов", districts.size());

        List<TerritoryUnitDto> dtos = districts.stream()
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