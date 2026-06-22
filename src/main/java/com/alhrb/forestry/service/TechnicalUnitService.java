package com.alhrb.forestry.service;

import com.alhrb.forestry.model.TechnicalUnit;
import com.alhrb.forestry.model.DistrictForestry;
import com.alhrb.forestry.repository.TechnicalUnitRepository;
import com.alhrb.forestry.repository.DistrictForestryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicalUnitService {

    private final TechnicalUnitRepository technicalUnitRepository;
    private final DistrictForestryRepository districtForestryRepository;

    @Transactional
    public TechnicalUnit save(TechnicalUnit technicalUnit) {
        return technicalUnitRepository.save(technicalUnit);
    }

    public List<TechnicalUnit> findAll() {
        return technicalUnitRepository.findAll();
    }

    public Optional<TechnicalUnit> findById(Long id) {
        return technicalUnitRepository.findById(id);
    }

    public List<TechnicalUnit> findByDistrictForestryId(Long districtForestryId) {
        return technicalUnitRepository.findByDistrictForestryIdOrderByName(districtForestryId);
    }

    public Optional<TechnicalUnit> findByDistrictForestryIdAndName(Long districtForestryId, String name) {
        return technicalUnitRepository.findByDistrictForestryIdAndName(districtForestryId, name);
    }

    public Optional<TechnicalUnit> findMainTechnicalUnit(Long districtForestryId) {
        return technicalUnitRepository.findByDistrictForestryIdAndIsMainTrue(districtForestryId);
    }

    @Transactional
    public TechnicalUnit createTechnicalUnit(String name, String code, Long districtForestryId, Boolean isMain, String description) {
        DistrictForestry districtForestry = districtForestryRepository.findById(districtForestryId)
                .orElseThrow(() -> new IllegalArgumentException("Участковое лесничество не найдено"));

        TechnicalUnit technicalUnit = new TechnicalUnit();
        technicalUnit.setName(name);
        technicalUnit.setCode(code);
        technicalUnit.setDescription(description);
        technicalUnit.setIsMain(isMain != null ? isMain : false);
        technicalUnit.setDistrictForestry(districtForestry);

        return technicalUnitRepository.save(technicalUnit);
    }

    @Transactional
    public void deleteById(Long id) {
        technicalUnitRepository.deleteById(id);
    }
}
