package com.alhrb.forestry.dto;

import com.alhrb.forestry.model.ForestryUnitType;
import org.locationtech.jts.geom.Geometry;

public record ForestryUnitRequestDto(
        Long parentId,              // ID родительского подразделения
        Long territoryUnitId,       // ID территориальной единицы
        String name,
        String type,                // ForestryUnitType как String
        String number,
        Geometry geometry,
        Long coordinateSystemId,
        Double centerLat,
        Double centerLng,
        Integer zoom,
        String accountNumber
) {}