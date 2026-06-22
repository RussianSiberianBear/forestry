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

    // ===== ПО ТЕХНИЧЕСКОМУ УЧАСТКУ =====
    List<Quarter> findByTechnicalUnitIdOrderByNumber(Long technicalUnitId);
    Optional<Quarter> findByTechnicalUnitIdAndNumber(Long technicalUnitId, Integer number);
    List<Quarter> findByTechnicalUnitIdAndNameContainingIgnoreCase(Long technicalUnitId, String name);

    // ===== ПО УЧАСТКОВОМУ ЛЕСНИЧЕСТВУ (для обратной совместимости) =====
    @Query("SELECT q FROM Quarter q WHERE q.districtForestry.id = :districtForestryId")
    List<Quarter> findByDistrictForestryId(@Param("districtForestryId") Long districtForestryId);

    @Query("SELECT q FROM Quarter q WHERE q.districtForestry.id = :districtForestryId ORDER BY q.number")
    List<Quarter> findByDistrictForestryIdOrderByNumber(@Param("districtForestryId") Long districtForestryId);

    Optional<Quarter> findByDistrictForestryIdAndNumber(Long districtForestryId, Integer number);
}
