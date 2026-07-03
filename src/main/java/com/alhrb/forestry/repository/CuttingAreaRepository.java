package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.CuttingArea;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuttingAreaRepository extends JpaRepository<CuttingArea, Long> {

    Optional<CuttingArea> findByFullNumber(String fullNumber);

    // ===== ПОИСК ПО ЛЕСНОЙ ЕДИНИЦЕ (рекурсивно по дереву) =====
    @Query(value = """
        WITH RECURSIVE forestry_tree AS (
            SELECT id FROM forestry_units WHERE id = :unitId
            UNION ALL
            SELECT fu.id FROM forestry_units fu
            INNER JOIN forestry_tree ft ON fu.parent_id = ft.id
        )
        SELECT p.* FROM forest_plot p
        WHERE p.forestry_unit_id IN (SELECT id FROM forestry_tree)
    """, nativeQuery = true)
    List<CuttingArea> findByForestryUnitRecursive(@Param("unitId") Long unitId);

    // ===== ПОИСК ПО ТЕРРИТОРИАЛЬНОЙ ЕДИНИЦЕ (рекурсивно по дереву территорий) =====
// В PlotRepository.java
    @Query(value = """
    WITH RECURSIVE territory_tree AS (
        SELECT id FROM territory_units WHERE id = :unitId
        UNION ALL
        SELECT tu.id FROM territory_units tu
        INNER JOIN territory_tree tt ON tu.parent_id = tt.id
    )
    SELECT p.* FROM forest_plot p
    WHERE p.forestry_unit_id IN (
        SELECT fu.id FROM forestry_units fu
        WHERE fu.territory_units_id IN (SELECT id FROM territory_tree)
    )
""", nativeQuery = true)
    List<CuttingArea> findByTerritoryUnitRecursive(@Param("unitId") Long unitId);

    // ===== ПОИСК ПО ТИПУ ЛЕСНОЙ ЕДИНИЦЫ И ТЕРРИТОРИИ =====
    @Query(value = """
        WITH RECURSIVE territory_tree AS (
            SELECT id FROM territory_units WHERE id = :unitId
            UNION ALL
            SELECT tu.id FROM territory_units tu
            INNER JOIN territory_tree tt ON tu.parent_id = tt.id
        )
        SELECT p.* FROM forest_plot p
        WHERE p.forestry_unit_id IN (
            SELECT fu.id FROM forestry_units fu
            WHERE fu.territory_units_id IN (SELECT id FROM territory_tree)
            AND fu.type = :type
        )
    """, nativeQuery = true)
    List<CuttingArea> findByForestryTypeAndIdRecursive(
            @Param("type") String type,
            @Param("unitId") Long unitId
    );

    // ===== ПОИСК ПО КВАРТАЛУ =====
    List<CuttingArea> findByForestryUnitIdOrderByNumberInQuarter(Long territoryUnitId);

    Optional<CuttingArea> findByForestryUnitIdAndNumberInQuarter(Long forestryUnitId, String numberInQuarter);

    // ===== ПОИСК ПО ТИПУ И РОДИТЕЛЮ =====
    @Query("SELECT p FROM CuttingArea p WHERE p.forestryUnit.type = :type AND p.forestryUnit.id = :parentId")
    List<CuttingArea> findByForestryTypeAndParentId(@Param("type") String type, @Param("parentId") Long parentId);

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