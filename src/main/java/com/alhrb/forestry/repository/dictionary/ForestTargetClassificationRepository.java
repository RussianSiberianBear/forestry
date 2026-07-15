package com.alhrb.forestry.repository.dictionary;

import com.alhrb.forestry.model.dictionary.ForestTargetClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForestTargetClassificationRepository extends JpaRepository<ForestTargetClassification, Long> {

    List<ForestTargetClassification> findByLevel(Short level);

    List<ForestTargetClassification> findByParentId(Long parentId);

    List<ForestTargetClassification> findByParentIsNull();

    Optional<ForestTargetClassification> findByCode(String code);

    Optional<ForestTargetClassification> findByParentIdAndName(Long parentId, String name);

    boolean existsByParentIdAndName(Long parentId, String name);

    @Query(value = """
            WITH RECURSIVE cte AS (
                SELECT * FROM forest_target_classification 
                WHERE parent_id = :parentId
                UNION ALL
                SELECT ftc.* FROM forest_target_classification ftc
                INNER JOIN cte ON cte.id = ftc.parent_id
            )
            SELECT * FROM cte
            """, nativeQuery = true)
    List<ForestTargetClassification> findAllDescendants(@Param("parentId") Long parentId);

    List<ForestTargetClassification> findByLevelOrderByNameAsc(Short level);

    List<ForestTargetClassification> findByParentIdOrderByNameAsc(Long parentId);

    @Query("SELECT MAX(ftc.level) FROM ForestTargetClassification ftc")
    Short findMaxLevel();

    List<ForestTargetClassification> findByNameContainingIgnoreCase(String name);

    long countByParentId(Long parentId);
}