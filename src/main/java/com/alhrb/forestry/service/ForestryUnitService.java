package com.alhrb.forestry.service;

import com.alhrb.forestry.model.ForestryUnit;
import com.alhrb.forestry.model.ForestryUnitType;
import com.alhrb.forestry.model.TerritoryType;
import com.alhrb.forestry.repository.ForestryUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForestryUnitService {
    private final ForestryUnitRepository forestryUnitRepository;

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

    public List<ForestryUnit> findDistrictForestriesByForestry(Long parentId) {
        return forestryUnitRepository.findByTypeAndParentId(ForestryUnitType.SUB_FORESTRY, parentId);
    }

    public List<ForestryUnit> findTechnicalUnit(Long districtForestryId) {
        return forestryUnitRepository.findByTypeAndParentId(ForestryUnitType.TECHNICAL_UNIT, districtForestryId);
    }

    public List<ForestryUnit> searchQuarters(Long technicalUnitId, String query) {
        return forestryUnitRepository.searchQuarters(technicalUnitId, query);
    }
}
