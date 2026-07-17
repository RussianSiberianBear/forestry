package com.alhrb.forestry.dto;

import org.locationtech.jts.geom.Geometry;

import java.time.LocalDateTime;

public record ForestryUnitResponseDto(
        Long id,
        Long parentId,              // только ID родителя, чтобы избежать циклических ссылок
        String parentName,          // имя родителя для удобства
        Long territoryUnitId,
        String territoryUnitName,
        String name,
        String type,                // как String
        String number,
        Geometry geometry,
        LocalDateTime createdAt,
        Long coordinateSystemId,
        Double centerLat,
        Double centerLng,
        Integer zoom,
        String accountNumber,
        String fullPath,             // полный путь из getFullPath()
        Boolean isLeaf
) {
}