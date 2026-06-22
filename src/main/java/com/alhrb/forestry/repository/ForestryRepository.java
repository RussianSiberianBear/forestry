package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.Forestry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForestryRepository extends JpaRepository<Forestry, Long> {
    List<Forestry> findByMunicipalDistrictId(Long municipalDistrictId);
    List<Forestry> findByRegionId(Long regionId);
    Optional<Forestry> findByMunicipalDistrictIdAndName(Long municipalDistrictId, String name);
    List<Forestry> findByMunicipalDistrictIdOrderByName(Long municipalDistrictId);
}
