package com.alhrb.forestry.service;

import com.alhrb.forestry.dto.CoordinateDto;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GeometryService {

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * Создание полигона с проверкой на корректность
     */
    public Polygon createPolygon(List<CoordinateDto> coordinates) {
        // 1. Проверка количества точек
        if (coordinates == null || coordinates.size() < 4) {
            throw new IllegalArgumentException(
                    String.format("Необходимо минимум 4 точки для полигона (передано %d)",
                            coordinates != null ? coordinates.size() : 0)
            );
        }

        // 2. Проверка на дублирование точек
        for (int i = 0; i < coordinates.size() - 1; i++) {
            CoordinateDto c1 = coordinates.get(i);
            CoordinateDto c2 = coordinates.get(i + 1);
            if (c1.getLat().equals(c2.getLat()) && c1.getLng().equals(c2.getLng())) {
                throw new IllegalArgumentException(
                        String.format("Обнаружены дублирующиеся точки: точка %d и %d совпадают", i + 1, i + 2)
                );
            }
        }

        // 3. Создаём массив координат
        Coordinate[] coords = coordinates.stream()
                .map(c -> new Coordinate(c.getLng(), c.getLat()))
                .toArray(Coordinate[]::new);

        // 4. Замыкаем полигон
        if (!coords[0].equals(coords[coords.length - 1])) {
            Coordinate[] closedCoords = new Coordinate[coords.length + 1];
            System.arraycopy(coords, 0, closedCoords, 0, coords.length);
            closedCoords[coords.length] = coords[0];
            coords = closedCoords;
        }

        // 5. Создаём полигон
        Polygon polygon = geometryFactory.createPolygon(coords);

        // 6. ПРОВЕРКА НА САМОПЕРЕСЕЧЕНИЕ ("БАБОЧКА")
        if (!polygon.isValid()) {
            log.warn("Создан невалидный полигон: {}", polygon.toText());

            // Проверяем, есть ли внутренние кольца (дырки)
            if (polygon.getNumInteriorRing() > 0) {
                throw new IllegalArgumentException(
                        "Полигон содержит внутренние отверстия (дырки). " +
                                "Возможно, точки введены в неправильном порядке."
                );
            }

            throw new IllegalArgumentException(
                    "⚠️ Обнаружена 'бабочка' (самопересечение полигона)!\n" +
                            "Точки, вероятно, введены в неправильном порядке.\n" +
                            "Попробуйте ввести точки по порядку (по часовой стрелке или против)."
            );
        }

        // 7. Проверка на слишком маленькую площадь
        double area = polygon.getArea() * 111319.9 * 111319.9;
        if (area < 1.0) {
            log.warn("Полигон имеет очень маленькую площадь: {} м²", area);
            throw new IllegalArgumentException(
                    String.format("Площадь полигона слишком мала (%.2f м²). " +
                            "Проверьте правильность координат.", area)
            );
        }

        log.info("Создан валидный полигон с {} точками, площадь: {:.2f} м²",
                coords.length, area);

        return polygon;
    }

    /**
     * Проверка, является ли полигон "бабочкой"
     */
    public boolean isButterfly(Polygon polygon) {
        if (polygon == null) return false;
        return !polygon.isValid() && polygon.getNumInteriorRing() == 0;
    }

    /**
     * Проверка валидности полигона
     */
    public boolean isValid(Polygon polygon) {
        return polygon != null && polygon.isValid();
    }

    /**
     * Конвертация полигона в WKT
     */
    public String toWkt(Polygon polygon) {
        if (polygon == null) return null;
        return polygon.toText();
    }
}
