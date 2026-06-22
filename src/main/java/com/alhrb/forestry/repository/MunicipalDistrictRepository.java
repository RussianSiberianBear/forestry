package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.MunicipalDistrict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MunicipalDistrictRepository extends JpaRepository<MunicipalDistrict, Long> {
    List<MunicipalDistrict> findByRegionId(Long regionId);
    Optional<MunicipalDistrict> findByRegionIdAndName(Long regionId, String name);
    List<MunicipalDistrict> findByRegionIdOrderByName(Long regionId);
}
