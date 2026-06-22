package com.alhrb.forestry.service;

import com.alhrb.forestry.model.DistrictForestry;
import com.alhrb.forestry.model.Forestry;
import com.alhrb.forestry.repository.DistrictForestryRepository;
import com.alhrb.forestry.repository.ForestryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistrictForestryService {

    private final DistrictForestryRepository districtForestryRepository;
    private final ForestryRepository forestryRepository;

    @Transactional
    public DistrictForestry save(DistrictForestry districtForestry) {
        return districtForestryRepository.save(districtForestry);
    }

    public List<DistrictForestry> findAll() {
        return districtForestryRepository.findAll();
    }

    public Optional<DistrictForestry> findById(Long id) {
        return districtForestryRepository.findById(id);
    }

    public List<DistrictForestry> findByForestryId(Long forestryId) {
        return districtForestryRepository.findByForestryIdOrderByName(forestryId);
    }

    public Optional<DistrictForestry> findByForestryIdAndName(Long forestryId, String name) {
        return districtForestryRepository.findByForestryIdAndName(forestryId, name);
    }

    @Transactional
    public DistrictForestry createDistrictForestry(String name, String code, Long forestryId, String description) {
        Forestry forestry = forestryRepository.findById(forestryId)
                .orElseThrow(() -> new IllegalArgumentException("Лесничество не найдено"));

        DistrictForestry districtForestry = new DistrictForestry();
        districtForestry.setName(name);
        districtForestry.setCode(code);
        districtForestry.setDescription(description);
        districtForestry.setForestry(forestry);

        return districtForestryRepository.save(districtForestry);
    }

    @Transactional
    public void deleteById(Long id) {
        districtForestryRepository.deleteById(id);
    }
}
