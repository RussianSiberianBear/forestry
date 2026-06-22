package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.DistrictForestry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictForestryRepository extends JpaRepository<DistrictForestry, Long> {
    List<DistrictForestry> findByForestryId(Long forestryId);
    Optional<DistrictForestry> findByForestryIdAndName(Long forestryId, String name);
    List<DistrictForestry> findByForestryIdOrderByName(Long forestryId);
}
