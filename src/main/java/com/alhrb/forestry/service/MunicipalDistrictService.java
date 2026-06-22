package com.alhrb.forestry.service;

import com.alhrb.forestry.model.MunicipalDistrict;
import com.alhrb.forestry.repository.MunicipalDistrictRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MunicipalDistrictService {

    private final MunicipalDistrictRepository municipalDistrictRepository;

    @Transactional
    public MunicipalDistrict save(MunicipalDistrict municipalDistrict) {
        return municipalDistrictRepository.save(municipalDistrict);
    }

    public List<MunicipalDistrict> findAll() {
        return municipalDistrictRepository.findAll();
    }

    public Optional<MunicipalDistrict> findById(Long id) {
        return municipalDistrictRepository.findById(id);
    }

    public List<MunicipalDistrict> findByRegionId(Long regionId) {
        return municipalDistrictRepository.findByRegionIdOrderByName(regionId);
    }

    public Optional<MunicipalDistrict> findByRegionIdAndName(Long regionId, String name) {
        return municipalDistrictRepository.findByRegionIdAndName(regionId, name);
    }

    @Transactional
    public void deleteById(Long id) {
        municipalDistrictRepository.deleteById(id);
    }
}
