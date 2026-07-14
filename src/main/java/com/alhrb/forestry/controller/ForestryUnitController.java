package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.ForestryUnitDto;
import com.alhrb.forestry.model.ForestryUnit;
import com.alhrb.forestry.service.ForestryUnitService;
import com.alhrb.forestry.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/forestry")
@RequiredArgsConstructor
@Slf4j
public class ForestryUnitController {

    private final ForestryUnitService forestryUnitService;
    private final SecurityHelper securityHelper;

    // ===== Допустимые ЛЕСНИЧЕСТВА =====
    @GetMapping("/forestries/all")
    public ResponseEntity<List<ForestryUnitDto>> getAllowedForestries() {
        List<ForestryUnit> forestries = forestryUnitService.findAllowedForestries(securityHelper.getCurrentUserId());
        List<ForestryUnitDto> dtos = forestries.stream()
                .map(this::toForestryDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== ЛЕСНИЧЕСТВА ПО РАЙОНУ =====
    @GetMapping("/forestries/by-district/{districtId}")
    public ResponseEntity<List<ForestryUnitDto>> getForestriesByDistrict(@PathVariable Long districtId) {
        log.info("📡 Запрос лесничеств для района ID: {}", districtId);
        List<ForestryUnit> forestries = forestryUnitService.findForestriesByDistrict(districtId);
        log.info("📊 Найдено {} лесничеств", forestries.size());

        List<ForestryUnitDto> dtos = forestries.stream()
                .map(this::toForestryDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== УЧАСТКОВЫЕ ЛЕСНИЧЕСТВА ПО ЛЕСНИЧЕСТВУ =====
    @GetMapping("/sub-forestries/by-forestry/{forestryId}")
    public ResponseEntity<List<ForestryUnitDto>> getSubForestriesByForestry(@PathVariable Long forestryId) {
        log.info("📡 Запрос участковых лесничеств для лесничества ID: {}", forestryId);
        List<ForestryUnit> districtForestries = forestryUnitService.findSubForestriesByForestry(forestryId);
        log.info("📊 Найдено {} участковых лесничеств", districtForestries.size());

        List<ForestryUnitDto> dtos = districtForestries.stream()
                .map(this::toForestryDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== ТЕХНИЧЕСКИЕ УЧАСТКИ ПО УЧАСТКОВОМУ =====
    @GetMapping("/technical-units/by-district/{districtForestryId}")
    public ResponseEntity<List<ForestryUnitDto>> getTechnicalUnitsByDistrict(@PathVariable Long districtForestryId) {
        log.info("📡 Запрос техучастков для участкового лесничества ID: {}", districtForestryId);
        List<ForestryUnit> technicalUnits = forestryUnitService.findTechnicalUnit(districtForestryId);

        log.info("📊 Найдено {} техучастков", technicalUnits.size());

        List<ForestryUnitDto> dtos = technicalUnits.stream()
                .map(this::toForestryDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== ПОЛУЧИТЬ ОДНУ ТЕРРИТОРИЮ =====
    @GetMapping("/{id}")
    public ResponseEntity<ForestryUnitDto> getById(@PathVariable Long id) {
        log.info("📡 Запрос территории ID: {}", id);
        return forestryUnitService.findById(id)
                .map(this::toForestryDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /*
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
*/
    // ===== ПОИСК КВАРТАЛОВ (AUTOCOMPLETE) =====
    @GetMapping("/quarters/search")
    public ResponseEntity<List<ForestryUnitDto>> searchQuarters(
            @RequestParam(required = false) Long technicalUnitId,
            @RequestParam(required = false) Long parentId,
            @RequestParam String query) {

        // Если передан parentId - используем его, иначе technicalUnitId
        Long searchParentId = parentId != null ? parentId : technicalUnitId;

        log.info("📡 Поиск кварталов для родителя ID: {}, запрос: {}", searchParentId, query);
        List<ForestryUnit> quarters = forestryUnitService.searchQuarters(searchParentId, query);

        List<ForestryUnitDto> dtos = quarters.stream()
                .map(this::toForestryDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    private ForestryUnitDto toForestryDto(ForestryUnit unit) {
        if (unit == null) {
            return null;
        }

        ForestryUnitDto dto = new ForestryUnitDto();
        dto.setId(unit.getId());
        dto.setName(unit.getName());
        dto.setType(unit.getType().name());
        dto.setNumber(unit.getNumber());
        dto.setAccountNumber(unit.getAccountNumber());
        dto.setCenterLat(unit.getCenterLat());
        dto.setCenterLng(unit.getCenterLng());
        dto.setZoom(unit.getZoom());

        if (unit.getParent() != null) {
            dto.setParentId(unit.getParent().getId());
            dto.setParentName(unit.getParent().getName());
        }

        return dto;
    }
}
