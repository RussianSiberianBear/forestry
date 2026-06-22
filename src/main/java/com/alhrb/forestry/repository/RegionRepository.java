package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByName(String name);
    Optional<Region> findByCode(String code);
}
