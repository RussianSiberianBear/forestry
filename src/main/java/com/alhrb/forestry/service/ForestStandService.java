package com.alhrb.forestry.service;

import com.alhrb.forestry.model.ForestStand;
import com.alhrb.forestry.repository.ForestStandRepository;
import com.alhrb.forestry.repository.ForestryUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForestStandService {

    private final ForestStandRepository forestStandRepository;
    private final ForestryUnitRepository forestryUnitRepository;
    @Value("${forest.validation.min-area:0.01}")
    private double minArea;

    @Transactional
    public ForestStand save(ForestStand forestStand) {
        return forestStandRepository.save(forestStand);
    }

    public List<ForestStand> findAll() {
        return forestStandRepository.findAll();
    }

    public Optional<ForestStand> findById(Long id) {
        return forestStandRepository.findById(id);
    }

    public Optional<ForestStand> findByFullNumber(String fullNumber) {
        return forestStandRepository.findByFullNumber(fullNumber);
    }

    public List<ForestStand> findByForestryUnitId(Long forestryUnitId) {
        return forestStandRepository.findByForestryUnitIdOrderByNumberInQuarter(forestryUnitId);
    }

    public Optional<ForestStand> findByForestryUnitIdAndNumberInQuarter(Long territoryUnitId, String numberInQuarter) {
        return forestStandRepository.findByForestryUnitIdAndNumberInQuarter(territoryUnitId, numberInQuarter);
    }

    public List<ForestStand> findByForestryUnitRecursive(Long unitId) {
        return forestStandRepository.findByForestryUnitRecursive(unitId);
    }

    public List<ForestStand> findByTerritoryUnitRecursive(Long unitId) {
        return forestStandRepository.findByTerritoryUnitRecursive(unitId);
    }

    public List<ForestStand> findByForestryTypeAndIdRecursive(String type, Long id) {
        return forestStandRepository.findByForestryTypeAndIdRecursive(type, id);
    }

}
