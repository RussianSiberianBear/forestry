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

    List<TerritoryUnit> findByType(TerritoryType type);

    List<TerritoryUnit> findByParentId(Long parentId);

    List<TerritoryUnit> findByTypeAndParentId(TerritoryType type, Long parentId);

    // ===== ПОИСК КВАРТАЛОВ ДЛЯ AUTOCOMPLETE =====
    @Query("""
        SELECT tu FROM TerritoryUnit tu
        WHERE tu.parent.id = :technicalUnitId
          AND tu.type = 'QUARTER'
          AND tu.number LIKE %:query%
        ORDER BY tu.number
    """)
    List<TerritoryUnit> searchQuarters(
            @Param("technicalUnitId") Long technicalUnitId,
            @Param("query") String query
    );

    // ===== ПОЛУЧИТЬ ВСЕ ДЕТИ (РЕКУРСИВНО) =====
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