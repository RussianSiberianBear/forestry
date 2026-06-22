package com.alhrb.forestry.service;

import com.alhrb.forestry.model.Region;
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
public class RegionService {

    private final RegionRepository regionRepository;

    @Transactional
    public Region save(Region region) {
        return regionRepository.save(region);
    }

    public List<Region> findAll() {
        return regionRepository.findAll();
    }

    public Optional<Region> findById(Long id) {
        return regionRepository.findById(id);
    }

    public Optional<Region> findByName(String name) {
        return regionRepository.findByName(name);
    }

    public Optional<Region> findByCode(String code) {
        return regionRepository.findByCode(code);
    }

    @Transactional
    public void deleteById(Long id) {
        regionRepository.deleteById(id);
    }
}
