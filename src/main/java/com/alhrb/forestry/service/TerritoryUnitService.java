package com.alhrb.forestry.service;

import com.alhrb.forestry.model.TerritoryType;
import com.alhrb.forestry.model.TerritoryUnit;
import com.alhrb.forestry.repository.TerritoryUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    public List<TerritoryUnit> findByTypeAndParentId(TerritoryType type, Long parentId) {
        return territoryUnitRepository.findByTypeAndParentId(type, parentId);
    }

    public List<TerritoryUnit> getFullPath(Long id) {
        List<Object[]> results = territoryUnitRepository.findPathToRoot(id);
        List<TerritoryUnit> path = new ArrayList<>();
        for (Object[] row : results) {
            TerritoryUnit unit = new TerritoryUnit();
            unit.setId(((Number) row[0]).longValue());
            unit.setName((String) row[1]);
            unit.setType(TerritoryType.valueOf((String) row[2]));
            path.add(unit);
        }
        return path;
    }

    @Transactional
    public TerritoryUnit save(TerritoryUnit unit) {
        return territoryUnitRepository.save(unit);
    }

    @Transactional
    public void delete(Long id) {
        territoryUnitRepository.deleteById(id);
    }

    @Transactional
    public TerritoryUnit createUnit(String name, TerritoryType type, Long parentId, String code, Boolean isMain, String number) {
        TerritoryUnit unit = new TerritoryUnit();
        unit.setName(name);
        unit.setType(type);
        unit.setCode(code);
        unit.setIsMain(isMain != null ? isMain : false);
        unit.setNumber(number);

        if (parentId != null) {
            TerritoryUnit parent = findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Родитель не найден: " + parentId));
            unit.setParent(parent);
        }

        return territoryUnitRepository.save(unit);
    }
}
