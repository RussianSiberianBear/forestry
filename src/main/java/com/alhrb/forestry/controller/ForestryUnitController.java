package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.ForestryUnitResponseDto;
import com.alhrb.forestry.mapper.ForestryUnitMapper;
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
    private final ForestryUnitMapper mapper;

    // ===== Допустимые ЛЕСНИЧЕСТВА =====
    @GetMapping("/forestries/all")
    public ResponseEntity<List<ForestryUnitResponseDto>> getAllowedForestries() {
        List<ForestryUnit> forestries = forestryUnitService.findAllowedForestries(securityHelper.getCurrentUserId());
        List<ForestryUnitResponseDto> dtos = forestries.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== ЛЕСНИЧЕСТВА ПО РАЙОНУ =====
    @GetMapping("/forestries/by-district/{districtId}")
    public ResponseEntity<List<ForestryUnitResponseDto>> getForestriesByDistrict(@PathVariable Long districtId) {
        log.info("📡 Запрос лесничеств для района ID: {}", districtId);
        List<ForestryUnit> forestries = forestryUnitService.findForestriesByDistrict(districtId);
        log.info("📊 Найдено {} лесничеств", forestries.size());

        List<ForestryUnitResponseDto> dtos = forestries.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== УЧАСТКОВЫЕ ЛЕСНИЧЕСТВА ПО ЛЕСНИЧЕСТВУ =====
    @GetMapping("/sub-forestries/by-forestry/{forestryId}")
    public ResponseEntity<List<ForestryUnitResponseDto>> getSubForestriesByForestry(@PathVariable Long forestryId) {
        log.info("📡 Запрос участковых лесничеств для лесничества ID: {}", forestryId);
        List<ForestryUnit> districtForestries = forestryUnitService.findSubForestriesByForestry(forestryId);
        log.info("📊 Найдено {} участковых лесничеств", districtForestries.size());

        List<ForestryUnitResponseDto> dtos = districtForestries.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== ТЕХНИЧЕСКИЕ УЧАСТКИ ПО УЧАСТКОВОМУ =====
    @GetMapping("/technical-units/by-district/{districtForestryId}")
    public ResponseEntity<List<ForestryUnitResponseDto>> getTechnicalUnitsByDistrict(@PathVariable Long districtForestryId) {
        log.info("📡 Запрос техучастков для участкового лесничества ID: {}", districtForestryId);
        List<ForestryUnit> technicalUnits = forestryUnitService.findTechnicalUnit(districtForestryId);

        log.info("📊 Найдено {} техучастков", technicalUnits.size());

        List<ForestryUnitResponseDto> dtos = technicalUnits.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ===== ПОЛУЧИТЬ ОДНУ ТЕРРИТОРИЮ =====
    @GetMapping("/{id}")
    public ResponseEntity<ForestryUnitResponseDto> getById(@PathVariable Long id) {
        log.info("📡 Запрос территории ID: {}", id);
        return forestryUnitService.findById(id)
                .map(mapper::toResponse)
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
    public ResponseEntity<List<ForestryUnitResponseDto>> searchQuarters(
            @RequestParam(required = false) Long technicalUnitId,
            @RequestParam(required = false) Long parentId,
            @RequestParam String query) {

        // Если передан parentId - используем его, иначе technicalUnitId
        Long searchParentId = parentId != null ? parentId : technicalUnitId;

        log.info("📡 Поиск кварталов для родителя ID: {}, запрос: {}", searchParentId, query);
        List<ForestryUnit> quarters = forestryUnitService.searchQuarters(searchParentId, query);

        List<ForestryUnitResponseDto> dtos = quarters.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

}
