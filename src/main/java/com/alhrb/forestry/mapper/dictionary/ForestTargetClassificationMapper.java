package com.alhrb.forestry.mapper.dictionary;

import com.alhrb.forestry.dto.dictionary.ForestTargetClassificationRequest;
import com.alhrb.forestry.dto.dictionary.ForestTargetClassificationResponse;
import com.alhrb.forestry.dto.dictionary.ForestTargetClassificationTree;
import com.alhrb.forestry.mapper.BaseMapper;
import com.alhrb.forestry.model.dictionary.ForestTargetClassification;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {}
)
@Component
public interface ForestTargetClassificationMapper extends BaseMapper<
        ForestTargetClassification,
        ForestTargetClassificationRequest,
        ForestTargetClassificationResponse
        > {

    @Override
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "level", source = "level")
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ForestTargetClassification toEntity(ForestTargetClassificationRequest request);

    @Override
    @Mapping(target = "parentId", expression = "java(entity.getParent() != null ? entity.getParent().getId() : null)")
    @Mapping(target = "parentName", expression = "java(entity.getParent() != null ? entity.getParent().getName() : null)")
    @Mapping(target = "children", ignore = true)
    ForestTargetClassificationResponse toResponse(ForestTargetClassification entity);

    @Override
    List<ForestTargetClassificationResponse> toResponseList(List<ForestTargetClassification> entities);

    @Override
    List<ForestTargetClassification> toEntityList(List<ForestTargetClassificationRequest> requests);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(ForestTargetClassificationRequest request, @MappingTarget ForestTargetClassification entity);

    /**
     * Преобразование в дерево (без циклов)
     */
    @Mapping(target = "children", ignore = true)
    ForestTargetClassificationTree toTree(ForestTargetClassification entity);

    /**
     * Преобразование списка в дерево
     */
    List<ForestTargetClassificationTree> toTreeList(List<ForestTargetClassification> entities);

    /**
     * Установка родителя для entity
     */
    default void setParent(ForestTargetClassification entity, Long parentId) {
        if (parentId != null && entity != null) {
            ForestTargetClassification parent = new ForestTargetClassification();
            parent.setId(parentId);
            entity.setParent(parent);
        }
    }

    /**
     * Пост-обработка Response для добавления детей
     */
    @AfterMapping
    default void mapChildren(@MappingTarget ForestTargetClassificationResponse response,
                             ForestTargetClassification entity) {
        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            response.setChildren(toResponseList(entity.getChildren()));
        }
    }

    /**
     * Пост-обработка Tree для добавления детей
     */
    @AfterMapping
    default void mapChildrenTree(@MappingTarget ForestTargetClassificationTree tree,
                                 ForestTargetClassification entity) {
        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            tree.setChildren(toTreeList(entity.getChildren()));
        }
    }
}