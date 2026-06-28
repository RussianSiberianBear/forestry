package com.alhrb.forestry.service;

import com.alhrb.forestry.model.TerritoryType;
import com.alhrb.forestry.model.TerritoryUnit;
import com.alhrb.forestry.repository.TerritoryUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerritoryUnitService {

    private final TerritoryUnitRepository territoryUnitRepository;

    public List<TerritoryUnit> findAll() {
        return territoryUnitRepository.findAll();
    }

    public Optional<TerritoryUnit> findById(Long id) {
        return territoryUnitRepository.findById(id);
    }

    public List<TerritoryUnit> findByType(TerritoryType type) {
        return territoryUnitRepository.findByType(type);
    }

    public List<TerritoryUnit> findByParentId(Long parentId) {
        return territoryUnitRepository.findByParentId(parentId);
    }

    public List<TerritoryUnit> findMunicipalDistrictsByRegion(Long parentId) {
        return territoryUnitRepository.findByTypeAndParentId(TerritoryType.MUNICIPAL_DISTRICT, parentId);
    }

}