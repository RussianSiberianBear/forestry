package com.alhrb.forestry.service;

import com.alhrb.forestry.model.Quarter;
import com.alhrb.forestry.model.DistrictForestry;
import com.alhrb.forestry.repository.QuarterRepository;
import com.alhrb.forestry.repository.DistrictForestryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuarterService {

    private final QuarterRepository quarterRepository;
    private final DistrictForestryRepository districtForestryRepository;
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

    public Optional<Quarter> findByDistrictForestryAndNumber(Long districtForestryId, Integer number) {
        return quarterRepository.findByDistrictForestryIdAndNumber(districtForestryId, number);
    }

    @Transactional
    public Quarter createQuarter(Integer number, String name, Long districtForestryId, Polygon geometry) {
        DistrictForestry districtForestry = districtForestryRepository.findById(districtForestryId)
                .orElseThrow(() -> new IllegalArgumentException("Участковое лесничество не найдено"));

        Quarter quarter = new Quarter();
        quarter.setNumber(number);
        quarter.setName(name);
        quarter.setDistrictForestry(districtForestry);
        quarter.setGeometry(geometry);

        return quarterRepository.save(quarter);
    }

    @Transactional
    public void deleteById(Long id) {
        quarterRepository.deleteById(id);
    }
}
