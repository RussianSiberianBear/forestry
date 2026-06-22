package com.alhrb.forestry.service;

import com.alhrb.forestry.model.Quarter;
import com.alhrb.forestry.model.DistrictForestry;
import com.alhrb.forestry.repository.QuarterRepository;
import com.alhrb.forestry.repository.DistrictForestryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public Quarter findById(Long id) {
        return quarterRepository.findById(id).orElse(null);
    }

    public List<Quarter> findByDistrictForestry(Long districtForestryId) {
        return quarterRepository.findByDistrictForestryId(districtForestryId);
    }

    @Transactional
    public Quarter createQuarter(Integer number, String name, Long districtForestryId) {
        DistrictForestry districtForestry = districtForestryRepository.findById(districtForestryId)
                .orElseThrow(() -> new IllegalArgumentException("Участковое лесничество не найдено"));

        Quarter quarter = new Quarter();
        quarter.setNumber(number);
        quarter.setName(name);
        quarter.setDistrictForestry(districtForestry);

        return quarterRepository.save(quarter);
    }
}
