package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.Plot;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlotRepository extends JpaRepository<Plot, Long> {

    /**
     * Проверка пересечений конкретной деляны с существующими
     */
    @Query(value = """
        SELECT 
            b.id, 
            b.plot_number,
            ST_Area(ST_Intersection(:geometry, b.geometry)) AS area
        FROM forest_plot b
        WHERE b.id != :plotId
            AND ST_Intersects(:geometry, b.geometry)
            AND ST_Area(ST_Intersection(:geometry, b.geometry)) > :minArea
    """, nativeQuery = true)
    List<Object[]> findIntersectionsWithPlot(
            @Param("geometry") Polygon geometry,
            @Param("plotId") Long plotId,
            @Param("minArea") Double minArea
    );

    /**
     * Массовая проверка всех делян
     */
    @Query(value = """
        WITH candidates AS (
            SELECT 
                a.id AS plot1_id,
                b.id AS plot2_id
            FROM forest_plot a
            JOIN forest_plot b ON a.id < b.id
            WHERE ST_Intersects(ST_Envelope(a.geometry), ST_Envelope(b.geometry))
        )
        SELECT 
            c.plot1_id,
            c.plot2_id,
            ST_Area(ST_Intersection(a.geometry, b.geometry)) AS area
        FROM candidates c
        JOIN forest_plot a ON a.id = c.plot1_id
        JOIN forest_plot b ON b.id = c.plot2_id
        WHERE ST_Intersects(a.geometry, b.geometry)
            AND ST_Area(ST_Intersection(a.geometry, b.geometry)) > :minArea
    """, nativeQuery = true)
    List<Object[]> findAllIntersections(@Param("minArea") Double minArea);

    /**
     * Проверка пересечений с учётом буфера (допуска)
     */
    @Query(value = """
        SELECT 
            b.id, 
            b.plot_number,
            ST_Area(ST_Intersection(
                ST_Buffer(:geometry, -:tolerance),
                ST_Buffer(b.geometry, -:tolerance)
            )) AS area
        FROM forest_plot b
        WHERE b.id != :plotId
            AND ST_Intersects(
                ST_Buffer(:geometry, -:tolerance),
                ST_Buffer(b.geometry, -:tolerance)
            )
            AND ST_Area(ST_Intersection(
                ST_Buffer(:geometry, -:tolerance),
                ST_Buffer(b.geometry, -:tolerance)
            )) > :minArea
    """, nativeQuery = true)
    List<Object[]> findIntersectionsWithPlotWithTolerance(
            @Param("geometry") Polygon geometry,
            @Param("plotId") Long plotId,
            @Param("minArea") Double minArea,
            @Param("tolerance") Double tolerance
    );

    Plot findByPlotNumber(String plotNumber);

    List<Plot> findByVerifiedTrue();

    List<Plot> findByVerifiedFalse();

    @Query(value = "SELECT ST_AsGeoJSON(geometry) FROM forest_plot WHERE id = :id", nativeQuery = true)
    String findGeometryAsGeoJson(@Param("id") Long id);
}