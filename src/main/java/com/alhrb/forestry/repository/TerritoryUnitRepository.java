package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.TerritoryType;
import com.alhrb.forestry.model.TerritoryUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TerritoryUnitRepository extends JpaRepository<TerritoryUnit, Long> {

    List<TerritoryUnit> findByType(TerritoryType type);

    List<TerritoryUnit> findByParentId(Long parentId);

    List<TerritoryUnit> findByTypeAndParentId(TerritoryType type, Long parentId);

    Optional<TerritoryUnit> findByTypeAndName(TerritoryType type, String name);

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

    // ===== ПОЛУЧИТЬ ПУТЬ К КОРНЮ =====
    @Query(value = """
        WITH RECURSIVE path_to_root AS (
            SELECT id, name, type, parent_id, 0 as depth
            FROM territory_units WHERE id = :id
            UNION ALL
            SELECT tu.id, tu.name, tu.type, tu.parent_id, ptr.depth + 1
            FROM territory_units tu
            INNER JOIN path_to_root ptr ON tu.id = ptr.parent_id
        )
        SELECT * FROM path_to_root ORDER BY depth DESC
    """, nativeQuery = true)
    List<Object[]> findPathToRoot(@Param("id") Long id);

    // ===== ПОИСК ПО НАЗВАНИЮ (AUTOCOMPLETE) =====
    @Query("SELECT tu FROM TerritoryUnit tu WHERE tu.name LIKE %:query% AND tu.type = :type")
    List<TerritoryUnit> searchByNameAndType(@Param("query") String query, @Param("type") TerritoryType type);

    @Query("SELECT tu FROM TerritoryUnit tu WHERE tu.name LIKE %:query%")
    List<TerritoryUnit> searchByName(@Param("query") String query);
}
