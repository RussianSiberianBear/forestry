package com.alhrb.forestry.service;

import com.alhrb.forestry.model.Quarter;
import com.alhrb.forestry.model.DistrictForestry;
import com.alhrb.forestry.model.TechnicalUnit;
import com.alhrb.forestry.repository.QuarterRepository;
import com.alhrb.forestry.repository.DistrictForestryRepository;
import com.alhrb.forestry.repository.TechnicalUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuarterService {

    private final QuarterRepository quarterRepository;
    private final DistrictForestryRepository districtForestryRepository;
    private final TechnicalUnitRepository technicalUnitRepository;
    private final GeometryService geometryService;

    @Transactional
    public Quarter save(Quarter quarter) {
        return quarterRepository.save(quarter);
    }

    public List<Quarter> findAll() {
        return quarterRepository.findAll();
    }

    public Optional<Quarter> findById(Long id) {
        return quarterRepository.findById(id);
    }

    public List<Quarter> findByDistrictForestry(Long districtForestryId) {
        return quarterRepository.findByDistrictForestryIdOrderByNumber(districtForestryId);
    }

    public List<Quarter> findByTechnicalUnit(Long technicalUnitId) {
        return quarterRepository.findByTechnicalUnitIdOrderByNumber(technicalUnitId);
    }

    public Optional<Quarter> findByTechnicalUnitAndNumber(Long technicalUnitId, Integer number) {
        return quarterRepository.findByTechnicalUnitIdAndNumber(technicalUnitId, number);
    }

    public Optional<Quarter> findByDistrictForestryAndNumber(Long districtForestryId, Integer number) {
        return quarterRepository.findByDistrictForestryIdAndNumber(districtForestryId, number);
    }

    // ===== НОВЫЙ МЕТОД ПОИСКА ДЛЯ AUTOCOMPLETE =====
    public List<Quarter> searchByTechnicalUnitAndNumber(Long technicalUnitId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Integer number = Integer.parseInt(query.trim());
            return quarterRepository.findByTechnicalUnitIdAndNumber(technicalUnitId, number)
                    .map(List::of)
                    .orElse(new ArrayList<>());
        } catch (NumberFormatException e) {
            return quarterRepository.findByTechnicalUnitIdAndNameContainingIgnoreCase(technicalUnitId, query.trim());
        }
    }

    @Transactional
    public Quarter createQuarter(Integer number, String name, Long technicalUnitId, Polygon geometry) {
        TechnicalUnit technicalUnit = technicalUnitRepository.findById(technicalUnitId)
                .orElseThrow(() -> new IllegalArgumentException("Технический участок не найден"));

        Quarter quarter = new Quarter();
        quarter.setNumber(number);
        quarter.setName(name);
        quarter.setTechnicalUnit(technicalUnit);
        quarter.setDistrictForestry(technicalUnit.getDistrictForestry()); // для быстрых запросов
        quarter.setGeometry(geometry);

        return quarterRepository.save(quarter);
    }

    @Transactional
    public void deleteById(Long id) {
        quarterRepository.deleteById(id);
    }
}
