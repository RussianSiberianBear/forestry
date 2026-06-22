package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.TechnicalUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicalUnitRepository extends JpaRepository<TechnicalUnit, Long> {
    List<TechnicalUnit> findByDistrictForestryId(Long districtForestryId);
    Optional<TechnicalUnit> findByDistrictForestryIdAndIsMainTrue(Long districtForestryId);
    List<TechnicalUnit> findByDistrictForestryIdOrderByName(Long districtForestryId);
    Optional<TechnicalUnit> findByDistrictForestryIdAndName(Long districtForestryId, String name);
}
