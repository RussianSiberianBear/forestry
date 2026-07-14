package com.alhrb.forestry.service;

import com.alhrb.forestry.common.specification.DynamicSpecificationBuilder;
import com.alhrb.forestry.common.specification.GridPageableBuilder;
import com.alhrb.forestry.dto.abgrid.GridP;
import com.alhrb.forestry.mapper.ForestryUnitMapper;
import com.alhrb.forestry.model.ForestryUnit;
import com.alhrb.forestry.model.ForestryUnitType;
import com.alhrb.forestry.repository.ForestryUnitRepository;
import com.alhrb.forestry.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


@Service
@RequiredArgsConstructor
@Slf4j
public class ForestryUnitService {

    private static final Set<String> FILTER_FIELDS = Set.of("id", "parent", "territoryUnit", "name", "type", "number",
            "geometry", "createdAt", "coordinateSystemId", "centerLat", "centerLng", "zoom", "accountNumber");
    private static final Set<String> SORT_FIELDS = Set.of("id", "parent", "territoryUnit", "name", "type", "number",
            "geometry", "createdAt", "coordinateSystemId", "centerLat", "centerLng", "zoom", "accountNumber");

    private final ForestryUnitRepository forestryUnitRepository;
    private final ForestryUnitMapper mapper;

    public List<ForestryUnit> findAllowedForestries(Long userId) {
        return forestryUnitRepository.findAllowedForestryByType(ForestryUnitType.FORESTRY, userId);
    }

    public Map<String, Object> findAllowedForestries(Long userId, GridP params) {

        Specification<User> specification =
                DynamicSpecificationBuilder.build(
                        params.getFilter(),
                        FILTER_FIELDS
                );

        Pageable pageable =
                GridPageableBuilder.build(
                        params,
                        SORT_FIELDS
                );

        Page page = forestryUnitRepository
                .findAll(specification, pageable, ForestryUnitType.FORESTRY, userId)
                .map(mapper::toResponse);

        Map<String, Object> data = Map.of(
                "rows", page.getContent(),
                "totalRecords", page.getTotalElements()
        );
        return Map.of("success", true, "message", "OK", "data", data);
    }

    public List<ForestryUnit> findAll() {
        return forestryUnitRepository.findAll();
    }

    public Optional<ForestryUnit> findById(Long id) {
        return forestryUnitRepository.findById(id);
    }

    public List<ForestryUnit> findByType(ForestryUnitType type) {
        return forestryUnitRepository.findByType(type);
    }

    public List<ForestryUnit> findByParentId(Long parentId) {
        return forestryUnitRepository.findByParentId(parentId);
    }

    public List<ForestryUnit> findForestriesByDistrict(Long parentId) {
        return forestryUnitRepository.findByTypeAndDistrictId(ForestryUnitType.FORESTRY, parentId);
    }

    public List<ForestryUnit> findSubForestriesByForestry(Long parentId) {
        return forestryUnitRepository.findByTypeAndParentId(ForestryUnitType.SUB_FORESTRY, parentId);
    }

    public List<ForestryUnit> findTechnicalUnit(Long districtForestryId) {
        return forestryUnitRepository.findByTypeAndParentId(ForestryUnitType.TECHNICAL_UNIT, districtForestryId);
    }

    public List<ForestryUnit> searchQuarters(Long technicalUnitId, String query) {
        return forestryUnitRepository.searchQuarters(technicalUnitId, query);
    }
}
