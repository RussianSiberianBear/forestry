package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.Quarter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuarterRepository extends JpaRepository<Quarter, Long> {
    List<Quarter> findByDistrictForestryId(Long districtForestryId);
    Optional<Quarter> findByDistrictForestryIdAndNumber(Long districtForestryId, Integer number);
    List<Quarter> findByDistrictForestryIdOrderByNumber(Long districtForestryId);

    @Query("SELECT q FROM Quarter q WHERE q.districtForestry.id = :districtForestryId")
    List<Quarter> findAllByDistrictForestryId(@Param("districtForestryId") Long districtForestryId);
}
