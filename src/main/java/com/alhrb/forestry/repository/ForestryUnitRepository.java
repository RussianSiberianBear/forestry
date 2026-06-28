package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.ForestryUnitType;
import com.alhrb.forestry.model.ForestryUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForestryUnitRepository extends JpaRepository<ForestryUnit, Long> {
    // ===== ОСНОВНЫЕ МЕТОДЫ =====
    List<ForestryUnit> findByType(ForestryUnitType type);
    List<ForestryUnit> findByParentId(Long parentId);
    List<ForestryUnit> findByParentIdIsNull();

    // ===== ПОИСК ПО ТИПУ И ИМЕНИ =====
    @Query("SELECT tu FROM ForestryUnit tu WHERE tu.type = :type AND tu.name = :name")
    List<ForestryUnit> findByTypeAndName(@Param("type") ForestryUnitType type, @Param("name") String name);

    @Query("SELECT tu FROM ForestryUnit tu WHERE tu.type = :type AND tu.parent.id = :parentId AND tu.name = :name")
    List<ForestryUnit> findByTypeAndParentIdAndName(
            @Param("type") ForestryUnitType type,
            @Param("parentId") Long parentId,
            @Param("name") String name
    );

    @Query("SELECT tu FROM ForestryUnit tu WHERE tu.type = :type AND tu.parent.id = :parentId")
    List<ForestryUnit> findByTypeAndParentId(
            @Param("type") ForestryUnitType type,
            @Param("parentId") Long parentId
    );

    @Query("SELECT tu FROM ForestryUnit tu WHERE tu.type = :type AND tu.territory.units.id = :territoryUnitId")
    List<ForestryUnit> findByTypeAndDistrictId(
            @Param("type") ForestryUnitType type,
            @Param("territoryUnitId") Long territoryUnitId
    );

    @Query("SELECT tu FROM ForestryUnit tu WHERE tu.type = :type AND tu.parent.id = :parentId AND tu.number = :number")
    List<ForestryUnit> findByTypeAndParentIdAndNumber(
            @Param("type") ForestryUnitType type,
            @Param("parentId") Long parentId,
            @Param("number") String number
    );

    @Query("SELECT tu FROM ForestryUnit tu WHERE tu.type = :type AND tu.number = :number")
    List<ForestryUnit> findByTypeAndNumber(
            @Param("type") ForestryUnitType type,
            @Param("number") String number
    );

    // ===== ПОИСК КВАРТАЛОВ ДЛЯ AUTOCOMPLETE =====
    @Query("""
        SELECT tu FROM ForestryUnit tu
        WHERE tu.parent.id = :technicalUnitId
          AND tu.type = 'QUARTER'
          AND tu.number LIKE %:query%
        ORDER BY tu.number
    """)
    List<ForestryUnit> searchQuarters(
            @Param("technicalUnitId") Long technicalUnitId,
            @Param("query") String query
    );

    // ===== ПОЛУЧИТЬ ВСЕХ ДЕТЕЙ (РЕКУРСИВНО) =====
    @Query(value = """
        WITH RECURSIVE forestry_tree AS (
            SELECT id, name, type, parent_id, 0 as depth
            FROM territory_units WHERE id = :rootId
            UNION ALL
            SELECT tu.id, tu.name, tu.type, tu.parent_id, tt.depth + 1
            FROM territory_units tu
            INNER JOIN forestry_tree tt ON tu.parent_id = tt.id
        )
        SELECT * FROM forestry_tree ORDER BY depth, name
    """, nativeQuery = true)
    List<Object[]> findAllDescendants(@Param("rootId") Long rootId);
}
