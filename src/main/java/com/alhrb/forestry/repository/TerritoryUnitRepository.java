package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.TerritoryType;
import com.alhrb.forestry.model.TerritoryUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TerritoryUnitRepository extends JpaRepository<TerritoryUnit, Long> {

    // ===== ОСНОВНЫЕ МЕТОДЫ =====
    List<TerritoryUnit> findByType(TerritoryType type);
    List<TerritoryUnit> findByTypeOrderByName(TerritoryType type);
    List<TerritoryUnit> findByParentId(Long parentId);
    List<TerritoryUnit> findByParentIdIsNull();

    // ===== ПОИСК ПО ТИПУ И ИМЕНИ =====
    @Query("SELECT tu FROM TerritoryUnit tu WHERE tu.type = :type AND tu.name = :name")
    List<TerritoryUnit> findByTypeAndName(@Param("type") TerritoryType type, @Param("name") String name);

    @Query("SELECT tu FROM TerritoryUnit tu WHERE tu.type = :type AND tu.parent.id = :parentId AND tu.name = :name")
    List<TerritoryUnit> findByTypeAndParentIdAndName(
            @Param("type") TerritoryType type,
            @Param("parentId") Long parentId,
            @Param("name") String name
    );

    @Query("SELECT tu FROM TerritoryUnit tu WHERE tu.type = :type AND tu.parent.id = :parentId")
    List<TerritoryUnit> findByTypeAndParentId(
            @Param("type") TerritoryType type,
            @Param("parentId") Long parentId
    );

    @Query("SELECT tu FROM TerritoryUnit tu WHERE tu.type = :type AND tu.parent.id = :parentId AND tu.number = :number")
    List<TerritoryUnit> findByTypeAndParentIdAndNumber(
            @Param("type") TerritoryType type,
            @Param("parentId") Long parentId,
            @Param("number") String number
    );

    @Query("SELECT tu FROM TerritoryUnit tu WHERE tu.type = :type AND tu.number = :number")
    List<TerritoryUnit> findByTypeAndNumber(
            @Param("type") TerritoryType type,
            @Param("number") String number
    );

    // ===== ПОЛУЧИТЬ ВСЕХ ДЕТЕЙ (РЕКУРСИВНО) =====
    @Query(value = """
        WITH RECURSIVE territory_tree AS (
            SELECT id, name, type, parent_id, 0 as depth
            FROM territory_units WHERE id = :rootId
            UNION ALL
            SELECT tu.id, tu.name, tu.type, tu.parent_id, tt.depth + 1
            FROM territory_units tu
            INNER JOIN territory_tree tt ON tu.parent_id = tt.id
        )
        SELECT * FROM territory_tree ORDER BY depth, name
    """, nativeQuery = true)
    List<Object[]> findAllDescendants(@Param("rootId") Long rootId);
}