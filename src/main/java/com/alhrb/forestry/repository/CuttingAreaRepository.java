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
            SELECT fu.id
            FROM forestry_units fu
            JOIN forestry_tree ft ON fu.parent_id = ft.id
        )
        SELECT *
        FROM cutting_area
        WHERE forestry_unit_id IN (SELECT id FROM forestry_tree)
        """, nativeQuery = true)
    List<CuttingArea> findByForestryUnitRecursive(@Param("unitId") Long unitId);


    // ===== ПОИСК ПО ТЕРРИТОРИАЛЬНОЙ ЕДИНИЦЕ =====

    @Query(value = """
        WITH RECURSIVE territory_tree AS (
            SELECT id FROM territory_units WHERE id = :unitId
            UNION ALL
            SELECT tu.id
            FROM territory_units tu
            JOIN territory_tree tt ON tu.parent_id = tt.id
        )
        SELECT p.*
        FROM cutting_area p
        WHERE p.forestry_unit_id IN (
            SELECT fu.id
            FROM forestry_units fu
            WHERE fu.territory_units_id IN (SELECT id FROM territory_tree)
        )
        """, nativeQuery = true)
    List<CuttingArea> findByTerritoryUnitRecursive(@Param("unitId") Long unitId);


    // ===== ПОИСК ПО ТИПУ =====

    @Query(value = """
        WITH RECURSIVE territory_tree AS (
            SELECT id FROM territory_units WHERE id = :unitId
            UNION ALL
            SELECT tu.id
            FROM territory_units tu
            JOIN territory_tree tt ON tu.parent_id = tt.id
        )
        SELECT p.*
        FROM cutting_area p
        WHERE p.forestry_unit_id IN (
            SELECT fu.id
            FROM forestry_units fu
            WHERE fu.territory_units_id IN (SELECT id FROM territory_tree)
              AND fu.type = :type
        )
        """, nativeQuery = true)
    List<CuttingArea> findByForestryTypeAndIdRecursive(
            @Param("type") String type,
            @Param("unitId") Long unitId
    );

    List<CuttingArea> findByForestryUnitIdOrderByNumberInQuarter(Long forestryUnitId);

    Optional<CuttingArea> findByForestryUnitIdAndNumberInQuarter(
            Long forestryUnitId,
            String numberInQuarter
    );

    @Query("""
        SELECT p
        FROM CuttingArea p
        WHERE p.forestryUnit.type = :type
          AND p.forestryUnit.id = :parentId
        """)
    List<CuttingArea> findByForestryTypeAndParentId(
            @Param("type") String type,
            @Param("parentId") Long parentId
    );


    // ==========================================================
    // ПРОВЕРКА ПЕРЕСЕЧЕНИЯ ОДНОЙ ДЕЛЯНЫ
    // Площадь возвращается в квадратных метрах
    // ==========================================================

    @Query(value = """
        SELECT
            b.id,
            b.full_number,
            ST_Area(
                ST_Transform(
                    ST_Intersection(:geometry, b.geometry),
                    3857
                )
            ) AS area
        FROM cutting_area b
        WHERE (:plotId IS NULL OR b.id <> :plotId)
          AND b.geometry && :geometry
          AND ST_Intersects(:geometry, b.geometry)
          AND ST_Area(
                ST_Transform(
                    ST_Intersection(:geometry, b.geometry),
                    3857
                )
              ) > :minArea
        """, nativeQuery = true)
    List<Object[]> findIntersectionsWithCuttingArea(
            @Param("geometry") Polygon geometry,
            @Param("plotId") Long plotId,
            @Param("minArea") Double minArea
    );


    // ==========================================================
    // ПРОВЕРКА ВСЕХ ДЕЛЯН
    // Площадь возвращается в квадратных метрах
    // ==========================================================

    @Query(value = """
        SELECT
            a.id,
            b.id,
            ST_Area(
                ST_Transform(
                    ST_Intersection(a.geometry, b.geometry),
                    3857
                )
            ) AS area
        FROM cutting_area a
        JOIN cutting_area b
          ON a.id < b.id
        WHERE a.geometry && b.geometry
          AND ST_Intersects(a.geometry, b.geometry)
          AND ST_Area(
                ST_Transform(
                    ST_Intersection(a.geometry, b.geometry),
                    3857
                )
              ) > :minArea
        """, nativeQuery = true)
    List<Object[]> findAllIntersections(
            @Param("minArea") Double minArea
    );

}