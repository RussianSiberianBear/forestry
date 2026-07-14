package com.alhrb.forestry.mapper;

import com.alhrb.forestry.dto.ForestryUnitRequestDto;
import com.alhrb.forestry.dto.ForestryUnitResponseDto;
import com.alhrb.forestry.model.ForestryUnit;
import com.alhrb.forestry.model.ForestryUnitType;
import com.alhrb.forestry.model.TerritoryUnit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ForestryUnitMapper extends BaseMapper<ForestryUnit, ForestryUnitRequestDto, ForestryUnitResponseDto> {

    // ===== ПЕРЕОПРЕДЕЛЯЕМ МЕТОДЫ С НАСТРОЙКАМИ =====

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", source = "parentId", qualifiedByName = "mapParent")
    @Mapping(target = "territoryUnit", source = "territoryUnitId", qualifiedByName = "mapTerritoryUnit")
    @Mapping(target = "type", source = "type", qualifiedByName = "stringToType")
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ForestryUnit toEntity(ForestryUnitRequestDto request);

    @Override
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "territoryUnitId", source = "territoryUnit.id")
    @Mapping(target = "territoryUnitName", source = "territoryUnit.name")
    @Mapping(target = "type", source = "type", qualifiedByName = "typeToString")
    @Mapping(target = "fullPath", source = "entity", qualifiedByName = "mapFullPath")
    ForestryUnitResponseDto toResponse(ForestryUnit entity);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", source = "parentId", qualifiedByName = "mapParent")
    @Mapping(target = "territoryUnit", source = "territoryUnitId", qualifiedByName = "mapTerritoryUnit")
    @Mapping(target = "type", source = "type", qualifiedByName = "stringToType")
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(ForestryUnitRequestDto request, @MappingTarget ForestryUnit entity);

    // ===== КАСТОМНЫЕ МЕТОДЫ ДЛЯ ПРЕОБРАЗОВАНИЙ =====

    /**
     * Преобразование ForestryUnitType → String
     */
    @Named("typeToString")
    default String typeToString(ForestryUnitType type) {
        return type == null ? null : type.name();
    }

    /**
     * Преобразование String → ForestryUnitType
     */
    @Named("stringToType")
    default ForestryUnitType stringToType(String type) {
        return type == null ? null : ForestryUnitType.valueOf(type);
    }

    /**
     * Получение ID родителя (для маппинга)
     */
    @Named("mapParent")
    default ForestryUnit mapParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        // Создаём прокси-объект только с ID для избежания лишних запросов
        ForestryUnit parent = new ForestryUnit();
        parent.setId(parentId);
        return parent;
    }

    /**
     * Получение территориальной единицы по ID
     */
    @Named("mapTerritoryUnit")
    default TerritoryUnit mapTerritoryUnit(Long territoryUnitId) {
        if (territoryUnitId == null) {
            return null;
        }
        TerritoryUnit territoryUnit = new TerritoryUnit();
        territoryUnit.setId(territoryUnitId);
        return territoryUnit;
    }

    /**
     * Преобразование списка детей в список ID
     */
    @Named("mapChildIds")
    default List<Long> mapChildIds(List<ForestryUnit> children) {
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        return children.stream()
                .map(ForestryUnit::getId)
                .collect(Collectors.toList());
    }

    /**
     * Получение полного пути
     */
    @Named("mapFullPath")
    default String mapFullPath(ForestryUnit entity) {
        return entity == null ? null : entity.getFullPath();
    }
}