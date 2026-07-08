package com.alhrb.forestry.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Сущность "Выдел" (forest_stand)
 * Хранит таксационную информацию о выделах из лесоустроительных материалов
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "forest_stand")
public class ForestStand {

    /**
     * Уникальный идентификатор выдела
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ссылка на квартал (forestry_units.id)
     * Определяется по лесничеству и номеру квартала
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forestry_unit_id", nullable = false)
    private ForestryUnit forestryUnit;



    /**
     * Номер выдела в пределах квартала
     * Из KML: выдел
     * Пример: 1, 2, 3, 12
     */
    @Column(name = "number_in_quarter", nullable = false, length = 50)
    private String numberInQuarter;

    /**
     * Полный номер выдела (уникальный)
     * Формат: {лесничество}_{квартал}_{выдел}
     * Пример: Бичурское_1_2
     */
    @Column(name = "full_number", nullable = false, length = 300, unique = true)
    private String fullNumber;

    /**
     * Краткое название/описание выдела
     * Может формироваться из ОЗУ или комбинации полей
     */
    @Column(name = "name", length = 200)
    private String name;

    /**
     * Полное описание выдела
     * Из KML: ОЗУ или комбинация полей
     */
    @Column(name = "description", length = 500)
    private String description;

    // ===== Таксационная характеристика =====

    /**
     * Состав древостоя
     * Из KML: соста
     * Пример: 6ОС4Б, 7Б3ОС, 10С
     */
    @Column(name = "composition", length = 50)
    private String composition;

    /**
     * Преобладающая порода
     * Из KML: преоб
     * Пример: ОС (осина), Б (береза), С (сосна), Е (ель)
     */
    @Column(name = "predominant_species", length = 50)
    private String predominantSpecies;

    /**
     * Возраст древостоя в годах
     * Из KML: возра
     */
    @Column(name = "age")
    private Integer age;

    /**
     * Средняя высота древостоя в метрах
     * Из KML: высот
     */
    @Column(name = "height", precision = 10, scale = 2)
    private BigDecimal height;

    /**
     * Средний диаметр стволов на высоте груди в см
     * Из KML: диаме
     */
    @Column(name = "diameter", precision = 10, scale = 2)
    private BigDecimal diameter;

    /**
     * Бонитет - оценка производительности насаждений
     * Из KML: бонит
     * Классы от I до V
     */
    @Column(name = "bonitet", length = 50)
    private String bonitet;

    /**
     * Тип леса
     * Из KML: тип_л
     * Пример: Рододендроновый, Сосняк лишайниковый
     */
    @Column(name = "forest_type", length = 100)
    private String forestType;

    /**
     * Тип лесорастительных условий
     * Из KML: тлу
     * Пример: А, В, С, Д
     */
    @Column(name = "tlu", length = 50)
    private String tlu;

    /**
     * Полнота древостоя
     * Из KML: полно
     * Значение от 0,1 до 1,0
     */
    @Column(name = "fullness", length = 50)
    private String fullness;

    /**
     * Запас древесины на 1 га в м³
     * Из KML: запас
     */
    @Column(name = "stock", precision = 10, scale = 2)
    private BigDecimal stock;

    // ===== Категории и классификация =====

    /**
     * Категория земель
     * Из KML: катег
     * Пример: Зеленые зоны, Лесные земли
     */
    @Column(name = "category", length = 255)
    private String category;

    /**
     * Категория защитности
     * Из KML: кате_1
     * Пример: Нас.ест.происх, Искусственные насаждения
     */
    @Column(name = "protection_category", length = 255)
    private String protectionCategory;

    /**
     * Группа лесов
     * Из KML: групп
     * Пример: Защитные, Эксплуатационные, Резервные
     */
    @Column(name = "group_type", length = 255)
    private String groupType;

    /**
     * Особо защитный участок
     * Из KML: ОЗУ
     * Полное описание особо защитного участка
     */
    @Column(name = "ozu", length = 255)
    private String ozu;

    // ===== Геометрия и площадь =====

    /**
     * Полигон выдела в системе координат WGS84 (EPSG:4326)
     * Извлекается из KML: coordinates
     */
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(name = "geometry", columnDefinition = "geometry(Polygon, 4326)")
    private Polygon geometry;

    /**
     * Площадь выдела в квадратных метрах
     * Рассчитывается автоматически из geometry через триггер
     */
    @Column(name = "area_m2", precision = 15, scale = 2)
    private BigDecimal areaM2;

    /**
     * Площадь выдела в гектарах
     * Из KML: площа
     * Используется как контрольное значение
     */
    @Column(name = "area_ha", precision = 10, scale = 2)
    private BigDecimal areaHa;

    // ===== Метаданные и статусы =====

    /**
     * Признак верификации выдела
     * false - не проверен, true - проверен и подтвержден
     */
    @Column(name = "verified")
    private Boolean verified;

    /**
     * Исходные данные в формате JSON
     * Хранит все поля из KML в сыром виде для истории и аудита
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_data", columnDefinition = "jsonb")
    private Map<String, Object> sourceData;

    /**
     * Дата и время создания записи
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления записи
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Год актуальности таксационных данных
     * По лесоустроительной инструкции, в зоне интенсивного освоения лесов
     * периодичность таксации составляет 10 лет [citation:10]
     * Данные старше 10 лет считаются неактуальными
     */
    @Column(name = "relevance_year")
    private Integer relevanceYear;
}