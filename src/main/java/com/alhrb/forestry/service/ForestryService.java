package com.alhrb.forestry.service;

import com.alhrb.forestry.model.Forestry;
import com.alhrb.forestry.model.MunicipalDistrict;
import com.alhrb.forestry.model.Region;
import com.alhrb.forestry.repository.ForestryRepository;
import com.alhrb.forestry.repository.MunicipalDistrictRepository;
import com.alhrb.forestry.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForestryService {

    private final ForestryRepository forestryRepository;
    private final MunicipalDistrictRepository municipalDistrictRepository;
    private final RegionRepository regionRepository;

    @Transactional
    public Forestry save(Forestry forestry) {
        return forestryRepository.save(forestry);
    }

    public List<Forestry> findAll() {
        return forestryRepository.findAll();
    }

    public Optional<Forestry> findById(Long id) {
        return forestryRepository.findById(id);
    }

    public List<Forestry> findByMunicipalDistrictId(Long municipalDistrictId) {
        return forestryRepository.findByMunicipalDistrictIdOrderByName(municipalDistrictId);
    }

    public List<Forestry> findByRegionId(Long regionId) {
        return forestryRepository.findByRegionId(regionId);
    }

    public Optional<Forestry> findByMunicipalDistrictIdAndName(Long municipalDistrictId, String name) {
        return forestryRepository.findByMunicipalDistrictIdAndName(municipalDistrictId, name);
    }

    @Transactional
    public Forestry createForestry(String name, String code, Long municipalDistrictId, String description) {
        MunicipalDistrict municipalDistrict = municipalDistrictRepository.findById(municipalDistrictId)
                .orElseThrow(() -> new IllegalArgumentException("Муниципальный район не найден"));

        Region region = municipalDistrict.getRegion();

        Forestry forestry = new Forestry();
        forestry.setName(name);
        forestry.setCode(code);
        forestry.setDescription(description);
        forestry.setMunicipalDistrict(municipalDistrict);
        forestry.setRegion(region);

        return forestryRepository.save(forestry);
    }

    @Transactional
    public void deleteById(Long id) {
        forestryRepository.deleteById(id);
    }
}
