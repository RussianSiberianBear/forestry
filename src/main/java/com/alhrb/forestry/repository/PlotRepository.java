package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.Plot;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@SuppressWarnings({"SqlResolve", "SqlDialectInspection", "unused"})
public interface PlotRepository extends JpaRepository<Plot, Long> {

    // ===== ПОИСК ПО ИЕРАРХИИ =====
    Optional<Plot> findByQuarterIdAndNumberInQuarter(Long quarterId, String numberInQuarter);
    Optional<Plot> findByFullNumber(String fullNumber);

    List<Plot> findByQuarterIdOrderByNumberInQuarter(Long quarterId);
    List<Plot> findByForestryId(Long forestryId);
    List<Plot> findByMunicipalDistrictId(Long municipalDistrictId);
    List<Plot> findByRegionId(Long regionId);

    // ===== ПРОВЕРКА ПЕРЕСЕЧЕНИЙ =====
    @Query(value = """
        SELECT 
            b.id, 
            b.full_number,
            ST_Area(ST_Intersection(:geometry, b.geometry)) AS area
        FROM forest_plot b
        WHERE b.id != :plotId
            AND ST_Intersects(:geometry, b.geometry)
            AND ST_Area(ST_Intersection(:geometry, b.geometry)) > :minArea
    """, nativeQuery = true)
    List<Object[]> findIntersectionsWithPlot(
            @Param("geometry") Polygon geometry,
            @Param("plotId") Long plotId,
            @Param("minArea") Double minArea
    );

    @Query(value = """
        WITH candidates AS (
            SELECT 
                a.id AS plot1_id,
                b.id AS plot2_id
            FROM forest_plot a
            JOIN forest_plot b ON a.id < b.id
            WHERE ST_Intersects(ST_Envelope(a.geometry), ST_Envelope(b.geometry))
        )
        SELECT 
            c.plot1_id,
            c.plot2_id,
            ST_Area(ST_Intersection(a.geometry, b.geometry)) AS area
        FROM candidates c
        JOIN forest_plot a ON a.id = c.plot1_id
        JOIN forest_plot b ON b.id = c.plot2_id
        WHERE ST_Intersects(a.geometry, b.geometry)
            AND ST_Area(ST_Intersection(a.geometry, b.geometry)) > :minArea
    """, nativeQuery = true)
    List<Object[]> findAllIntersections(@Param("minArea") Double minArea);
}
