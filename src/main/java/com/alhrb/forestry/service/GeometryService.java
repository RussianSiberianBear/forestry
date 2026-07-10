package com.alhrb.forestry.service;

import com.alhrb.forestry.dto.CoordinateDto;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GeometryService {

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    // Константа для сравнения double
    private static final double EPSILON = 1e-10;

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
            if (Math.abs(c1.getLat() - c2.getLat()) < EPSILON &&
                    Math.abs(c1.getLng() - c2.getLng()) < EPSILON) {
                throw new IllegalArgumentException("Обнаружены дублирующиеся точки");
            }
        }

        Coordinate[] coords = coordinates.stream()
                .map(c -> new Coordinate(c.getLng(), c.getLat()))
                .toArray(Coordinate[]::new);

        // Замыкаем полигон
        if (coords[0].distance(coords[coords.length - 1]) > EPSILON) {
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

        // Нормализуем полигон перед возвратом
        return normalizePolygon(polygon);
    }

    /**
     * Нормализует полигон:
     * 1. Приводит к против часовой стрелки (CCW)
     * 2. Сдвигает массив так, чтобы минимальная точка была первой
     * 3. Удаляет дублирующиеся точки
     * 4. Обрабатывает внутренние кольца (дырки)
     *
     * @param polygon исходный полигон
     * @return нормализованный полигон
     */
    public Polygon normalizePolygon(Polygon polygon) {
        if (polygon == null || polygon.isEmpty()) {
            return polygon;
        }

        if (!polygon.isValid()) {
            log.warn("⚠️ Попытка нормализовать невалидный полигон");
            return polygon;
        }

        try {
            // 1. Нормализуем внешнее кольцо
            Coordinate[] exteriorCoords = polygon.getExteriorRing().getCoordinates();
            Coordinate[] normalizedExterior = normalizeRing(exteriorCoords);

            // 2. Нормализуем внутренние кольца (дырки), если есть
            int numInteriorRings = polygon.getNumInteriorRing();
            LinearRing[] normalizedInteriorRings = new LinearRing[numInteriorRings];

            for (int i = 0; i < numInteriorRings; i++) {
                Coordinate[] interiorCoords = polygon.getInteriorRingN(i).getCoordinates();
                // Дырки должны быть по часовой стрелке (противоположно внешнему кольцу)
                Coordinate[] normalizedInterior = normalizeRing(interiorCoords);
                // Для дырок инвертируем ориентацию
                if (isCounterClockwise(normalizedInterior)) {
                    normalizedInterior = reverseArray(normalizedInterior);
                }
                normalizedInteriorRings[i] = geometryFactory.createLinearRing(
                        closeRing(normalizedInterior)
                );
            }

            // Создаем нормализованный полигон
            LinearRing exteriorRing = geometryFactory.createLinearRing(
                    closeRing(normalizedExterior)
            );

            return geometryFactory.createPolygon(exteriorRing, normalizedInteriorRings);

        } catch (Exception e) {
            log.error("Ошибка при нормализации полигона: {}", e.getMessage());
            return polygon;
        }
    }

    /**
     * Нормализует кольцо координат:
     * 1. Удаляет дублирующиеся точки
     * 2. Приводит к против часовой стрелки
     * 3. Сдвигает к минимальной точке
     */
    private Coordinate[] normalizeRing(Coordinate[] coords) {
        // Удаляем последнюю точку, если она дублирует первую
        Coordinate[] ringCoords;
        if (coords.length > 1 && coords[0].distance(coords[coords.length - 1]) < EPSILON) {
            ringCoords = new Coordinate[coords.length - 1];
            System.arraycopy(coords, 0, ringCoords, 0, coords.length - 1);
        } else {
            ringCoords = coords.clone();
        }

        // Удаляем дублирующиеся точки внутри кольца
        ringCoords = removeDuplicatePoints(ringCoords);

        if (ringCoords.length < 3) {
            return ringCoords;
        }

        // Приводим к против часовой стрелки
        if (!isCounterClockwise(ringCoords)) {
            ringCoords = reverseArray(ringCoords);
        }

        // Сдвигаем к минимальной точке
        int minIndex = findMinCoordinateIndex(ringCoords);
        if (minIndex > 0) {
            ringCoords = shiftArray(ringCoords, minIndex);
        }

        return ringCoords;
    }

    /**
     * Удаляет дублирующиеся точки в кольце
     */
    private Coordinate[] removeDuplicatePoints(Coordinate[] coords) {
        if (coords.length <= 1) {
            return coords;
        }

        List<Coordinate> unique = new ArrayList<>();
        unique.add(coords[0]);

        for (int i = 1; i < coords.length; i++) {
            Coordinate last = unique.get(unique.size() - 1);
            if (last.distance(coords[i]) > EPSILON) {
                unique.add(coords[i]);
            }
        }

        // Проверяем последнюю и первую точки
        if (unique.size() > 1 &&
                unique.get(0).distance(unique.get(unique.size() - 1)) < EPSILON) {
            unique.remove(unique.size() - 1);
        }

        return unique.toArray(new Coordinate[0]);
    }

    /**
     * Замыкает кольцо (добавляет первую точку в конец)
     */
    private Coordinate[] closeRing(Coordinate[] coords) {
        if (coords.length == 0) {
            return coords;
        }
        if (coords[0].distance(coords[coords.length - 1]) < EPSILON) {
            return coords;
        }
        Coordinate[] closed = new Coordinate[coords.length + 1];
        System.arraycopy(coords, 0, closed, 0, coords.length);
        closed[coords.length] = coords[0];
        return closed;
    }

    /**
     * Проверяет, является ли массив координат ориентированным против часовой стрелки
     */
    private boolean isCounterClockwise(Coordinate[] coords) {
        if (coords.length < 3) {
            return false;
        }
        double signedArea = computeSignedArea(coords);
        return signedArea > 0;
    }

    /**
     * Вычисляет знаковую площадь (формула шнурка)
     */
    private double computeSignedArea(Coordinate[] coords) {
        if (coords.length < 3) {
            return 0.0;
        }
        double area = 0.0;
        int n = coords.length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += coords[i].x * coords[j].y;
            area -= coords[j].x * coords[i].y;
        }
        return area / 2.0;
    }

    /**
     * Разворачивает массив координат
     */
    private Coordinate[] reverseArray(Coordinate[] coords) {
        Coordinate[] reversed = new Coordinate[coords.length];
        for (int i = 0; i < coords.length; i++) {
            reversed[i] = coords[coords.length - 1 - i];
        }
        return reversed;
    }

    /**
     * Находит индекс лексикографически минимальной точки
     * (сначала по X, потом по Y)
     */
    private int findMinCoordinateIndex(Coordinate[] coords) {
        if (coords.length == 0) {
            return 0;
        }
        int minIndex = 0;
        for (int i = 1; i < coords.length; i++) {
            if (coords[i].x < coords[minIndex].x - EPSILON ||
                    (Math.abs(coords[i].x - coords[minIndex].x) < EPSILON &&
                            coords[i].y < coords[minIndex].y - EPSILON)) {
                minIndex = i;
            }
        }
        return minIndex;
    }

    /**
     * Сдвигает массив так, чтобы элемент с индексом startIndex стал первым
     */
    private Coordinate[] shiftArray(Coordinate[] coords, int startIndex) {
        int n = coords.length;
        Coordinate[] shifted = new Coordinate[n];
        for (int i = 0; i < n; i++) {
            shifted[i] = coords[(startIndex + i) % n];
        }
        return shifted;
    }

    /**
     * Проверка, что полигон валидный
     */
    public boolean isValid(Polygon polygon) {
        return polygon != null && !polygon.isEmpty() && polygon.isValid();
    }

    /**
     * Проверка, что деляна находится внутри квартала
     */
    public boolean isPlotInsideQuarter(Polygon plotGeometry, Polygon quarterGeometry) {
        if (plotGeometry == null || quarterGeometry == null) {
            return false;
        }

        // Нормализуем оба полигона для корректного сравнения
        Polygon normalizedPlot = normalizePolygon(plotGeometry);
        Polygon normalizedQuarter = normalizePolygon(quarterGeometry);

        return normalizedQuarter.contains(normalizedPlot);
    }

    /**
     * Проверка с понятным сообщением
     */
    public void validatePlotInsideQuarter(Polygon plotGeometry, Polygon quarterGeometry, String plotNumber, String quarterNumber) {
        if (plotGeometry == null || quarterGeometry == null) {
            return;
        }

        // Нормализуем оба полигона для корректного сравнения
        Polygon normalizedPlot = normalizePolygon(plotGeometry);
        Polygon normalizedQuarter = normalizePolygon(quarterGeometry);

        // Проверяем, что деляна полностью внутри квартала
        if (!normalizedQuarter.contains(normalizedPlot)) {
            throw new IllegalArgumentException(
                    String.format("❌ Деляна '%s' выходит за границы квартала %s!", plotNumber, quarterNumber)
            );
        }

        // Проверяем, что площадь деляны не превышает площадь квартала
        double plotArea = normalizedPlot.getArea();
        double quarterArea = normalizedQuarter.getArea();

        if (plotArea > quarterArea + EPSILON) {
            throw new IllegalArgumentException(
                    String.format("❌ Площадь деляны '%s' (%.2f га) превышает площадь квартала %s (%.2f га)!",
                            plotNumber, plotArea / 10000, quarterNumber, quarterArea / 10000)
            );
        }
    }

    /**
     * Проверяет пересечение двух полигонов с учетом нормализации
     * @return true если полигоны пересекаются (включая касание границ)
     */
    public boolean intersects(Polygon p1, Polygon p2) {
        if (p1 == null || p2 == null) {
            return false;
        }
        Polygon normalizedP1 = normalizePolygon(p1);
        Polygon normalizedP2 = normalizePolygon(p2);
        return normalizedP1.intersects(normalizedP2);
    }

    /**
     * Проверяет пересечение двух полигонов по площади
     * @return true если площадь пересечения > minArea
     */
    public boolean intersectsByArea(Polygon p1, Polygon p2, double minArea) {
        if (p1 == null || p2 == null) {
            return false;
        }
        Polygon normalizedP1 = normalizePolygon(p1);
        Polygon normalizedP2 = normalizePolygon(p2);

        if (!normalizedP1.intersects(normalizedP2)) {
            return false;
        }

        Geometry intersection = normalizedP1.intersection(normalizedP2);
        return intersection.getArea() > minArea;
    }

    /**
     * Вычисляет площадь пересечения двух полигонов
     */
    public double intersectionArea(Polygon p1, Polygon p2) {
        if (p1 == null || p2 == null) {
            return 0.0;
        }
        Polygon normalizedP1 = normalizePolygon(p1);
        Polygon normalizedP2 = normalizePolygon(p2);

        if (!normalizedP1.intersects(normalizedP2)) {
            return 0.0;
        }

        Geometry intersection = normalizedP1.intersection(normalizedP2);
        return intersection.getArea();
    }

    /**
     * Объединение нескольких полигонов в один
     */
    public Polygon unionPlots(List<Polygon> polygons) {
        if (polygons == null || polygons.isEmpty()) {
            return null;
        }
        if (polygons.size() == 1) {
            return normalizePolygon(polygons.get(0));
        }

        // Нормализуем все полигоны перед объединением
        List<Polygon> normalizedPolygons = polygons.stream()
                .map(this::normalizePolygon)
                .collect(java.util.stream.Collectors.toList());

        GeometryCollection geometryCollection = geometryFactory.createGeometryCollection(
                normalizedPolygons.toArray(new Polygon[0])
        );

        Geometry union = geometryCollection.union();
        if (union instanceof Polygon) {
            return normalizePolygon((Polygon) union);
        }

        log.warn("Результат объединения не является полигоном, возможно это MultiPolygon");
        return null;
    }

    /**
     * Конвертация полигона в WKT
     */
    public String toWkt(Polygon polygon) {
        return polygon != null ? polygon.toText() : null;
    }

    /**
     * Сравнивает два полигона на геометрическое равенство
     * (с учетом нормализации)
     */
    public boolean equalsExact(Polygon p1, Polygon p2) {
        if (p1 == null || p2 == null) {
            return p1 == p2;
        }
        Polygon normalizedP1 = normalizePolygon(p1);
        Polygon normalizedP2 = normalizePolygon(p2);
        return normalizedP1.equalsExact(normalizedP2, EPSILON);
    }

    /**
     * Вычисляет площадь полигона в гектарах
     */
    public double getAreaHa(Polygon polygon) {
        if (polygon == null || polygon.isEmpty()) {
            return 0.0;
        }
        // JTS возвращает площадь в квадратных метрах (для EPSG:4326 - приблизительно)
        // Для более точного расчета используйте проекцию
        return polygon.getArea() / 10000.0;
    }

    /**
     * Вычисляет площадь полигона в квадратных метрах
     */
    public double getAreaM2(Polygon polygon) {
        if (polygon == null || polygon.isEmpty()) {
            return 0.0;
        }
        return polygon.getArea();
    }

    /**
     * Проверяет, что полигон не вырожденный (площадь > minArea)
     */
    public boolean hasValidArea(Polygon polygon, double minArea) {
        if (polygon == null || polygon.isEmpty()) {
            return false;
        }
        return polygon.getArea() > minArea;
    }
}