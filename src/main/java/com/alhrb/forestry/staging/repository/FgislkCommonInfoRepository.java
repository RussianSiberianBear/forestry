package com.alhrb.forestry.staging.repository;

import com.alhrb.forestry.staging.model.FgislkCommonInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FgislkCommonInfoRepository extends JpaRepository<FgislkCommonInfo, Long> {

    /**
     * Очищает таблицу перед загрузкой
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM FgislkCommonInfo where userId = :userId")
    void truncateTable(@Param("userId") Long userId);

    /**
     * Проверяет наличие данных
     */
    @Query("SELECT COUNT(f) > 0 FROM FgislkCommonInfo f")
    boolean hasData(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM FgislkCommonInfo f WHERE f.userId =: userId")
    long getCount(@Param("userId") Long userId);

    /**
     * Получение статистики по регионам
     */
    @Query("""
        SELECT f.regionName, COUNT(f) as count 
        FROM FgislkCommonInfo f 
        WHERE f.userId =: userId    
        GROUP BY f.regionName 
        ORDER BY count DESC
    """)
    List<Object[]> getRegionStatistics(@Param("userId") Long userId);

    /**
     * Получение общей статистики
     */
    @Query("""
        SELECT 
            COUNT(f),
            COUNT(DISTINCT f.regionCode),
            COUNT(DISTINCT f.forestDistrictCode),
            SUM(f.forestPlotArea)
        FROM FgislkCommonInfo f
        WHERE f.userId = :userId    
    """)
    Object[] getTotalStatistics(@Param("userId") Long userId);

    /**
     * Поиск по региону
     */
    List<FgislkCommonInfo> findByRegionCode(String regionCode);

    /**
     * Поиск по лесничеству
     */
    List<FgislkCommonInfo> findByForestDistrictCode(String forestDistrictCode);
}