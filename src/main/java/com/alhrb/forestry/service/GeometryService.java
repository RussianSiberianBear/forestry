package com.alhrb.forestry.service;

import com.alhrb.forestry.dto.CoordinateDto;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GeometryService {

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * Создание полигона из списка координат с проверкой на "бабочку"
     */
    public Polygon createPolygon(List<CoordinateDto> coordinates) {
        if (coordinates == null || coordinates.size() < 3) {
            throw new IllegalArgumentException("Необходимо минимум 3 точки для полигона");
        }

        // Проверка на дублирование точек
        for (int i = 0; i < coordinates.size() - 1; i++) {
            CoordinateDto c1 = coordinates.get(i);
            CoordinateDto c2 = coordinates.get(i + 1);
            if (c1.getLat().equals(c2.getLat()) && c1.getLng().equals(c2.getLng())) {
                throw new IllegalArgumentException("Обнаружены дублирующиеся точки");
            }
        }

        Coordinate[] coords = coordinates.stream()
                .map(c -> new Coordinate(c.getLng(), c.getLat()))
                .toArray(Coordinate[]::new);

        // Замыкаем полигон
        if (!coords[0].equals(coords[coords.length - 1])) {
            Coordinate[] closedCoords = new Coordinate[coords.length + 1];
            System.arraycopy(coords, 0, closedCoords, 0, coords.length);
            closedCoords[coords.length] = coords[0];
            coords = closedCoords;
        }

        Polygon polygon = geometryFactory.createPolygon(coords);

        if (!polygon.isValid()) {
            throw new IllegalArgumentException(
                    "⚠️ Обнаружена 'бабочка' (самопересечение полигона)!\n" +
                            "Точки введены в неправильном порядке."
            );
        }

        return polygon;
    }

    /**
     * Проверка, что полигон валидный
     */
    public boolean isValid(Polygon polygon) {
        return polygon != null && polygon.isValid();
    }

    /**
     * Проверка, что деляна находится внутри квартала
     */
    public boolean isPlotInsideQuarter(Polygon plotGeometry, Polygon quarterGeometry) {
        if (plotGeometry == null || quarterGeometry == null) {
            return false;
        }
        return quarterGeometry.contains(plotGeometry);
    }

    /**
     * Проверка с понятным сообщением
     */
    public void validatePlotInsideQuarter(Polygon plotGeometry, Polygon quarterGeometry, String plotNumber, String quarterNumber) {
        if (plotGeometry == null || quarterGeometry == null) {
            return;
        }

        // Проверяем, что деляна полностью внутри квартала
        if (!quarterGeometry.contains(plotGeometry)) {
            throw new IllegalArgumentException(
                    String.format("❌ Деляна '%s' выходит за границы квартала %s!", plotNumber, quarterNumber)
            );
        }

        // Проверяем, что площадь деляны не превышает площадь квартала
        double plotArea = plotGeometry.getArea();
        double quarterArea = quarterGeometry.getArea();

        if (plotArea > quarterArea) {
            throw new IllegalArgumentException(
                    String.format("❌ Площадь деляны '%s' (%.2f га) превышает площадь квартала %s (%.2f га)!",
                            plotNumber, plotArea / 10000, quarterNumber, quarterArea / 10000)
            );
        }
    }

    /**
     * Объединение нескольких полигонов в один
     */
    public Polygon unionPlots(List<Polygon> polygons) {
        if (polygons == null || polygons.isEmpty()) {
            return null;
        }
        if (polygons.size() == 1) {
            return polygons.get(0);
        }

        GeometryCollection geometryCollection = geometryFactory.createGeometryCollection(
                polygons.toArray(new Polygon[0])
        );

        return (Polygon) geometryCollection.union();
    }

    /**
     * Конвертация полигона в WKT
     */
    public String toWkt(Polygon polygon) {
        return polygon != null ? polygon.toText() : null;
    }
}
