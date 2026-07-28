






-- =====================================================
-- 1. Создаём схему staging
-- =====================================================
CREATE SCHEMA IF NOT EXISTS staging;

-- =====================================================
-- 2. Удаляем старую таблицу (если есть)
-- =====================================================
DROP TABLE IF EXISTS staging.fgislk_common_info CASCADE;

-- =====================================================
-- 3. Создаём таблицу для данных ФГИС ЛК
-- =====================================================
CREATE TABLE staging.fgislk_common_info (
                                            id                              BIGSERIAL PRIMARY KEY,
                                            user_id                         int8,
                                            region_code                     VARCHAR(50),    -- Код региона
                                            region_name                     VARCHAR(255),   -- Название региона
                                            forest_district_code            VARCHAR(50),    -- Код лесничества
                                            forest_district_name            VARCHAR(255),   -- Название лесничества
                                            forest_quarter_code             VARCHAR(50),    -- Код квартала
                                            forest_plot_code                VARCHAR(50),    -- Код выдела
                                            forest_plot_area                NUMERIC(15, 4), -- Площадь выдела (га)
                                            forest_plot_characteristic      TEXT,           -- Характеристика выдела
                                            forest_type                     VARCHAR(100),   -- Тип леса
                                            dominant_species                VARCHAR(50),    -- Преобладающая порода
                                            age_class                       VARCHAR(50),    -- Класс возраста
                                            forest_group                    VARCHAR(100),   -- Группа лесов
                                            forest_category                 VARCHAR(100),   -- Категория лесов
                                            protection_category             VARCHAR(100),   -- Категория защитности
                                            purpose                         VARCHAR(255),   -- Целевое назначение
                                            inventory_date                  DATE,           -- Дата таксации
                                            notes                           TEXT,           -- Примечания
                                            created_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            updated_at                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 4. Создаём индексы для ускорения поиска
-- =====================================================
CREATE INDEX idx_fgislk_region_code ON staging.fgislk_common_info (region_code);
CREATE INDEX idx_fgislk_region_name ON staging.fgislk_common_info (region_name);
CREATE INDEX idx_fgislk_district_code ON staging.fgislk_common_info (forest_district_code);
CREATE INDEX idx_fgislk_district_name ON staging.fgislk_common_info (forest_district_name);
CREATE INDEX idx_fgislk_quarter_code ON staging.fgislk_common_info (forest_quarter_code);
CREATE INDEX idx_fgislk_plot_code ON staging.fgislk_common_info (forest_plot_code);
CREATE INDEX idx_fgislk_inventory_date ON staging.fgislk_common_info (inventory_date);

-- =====================================================
-- 5. Добавляем комментарии
-- =====================================================
COMMENT ON TABLE staging.fgislk_common_info IS 'Атрибутивная информация по лесничествам из ФГИС ЛК';
COMMENT ON COLUMN staging.fgislk_common_info.region_code IS 'Код субъекта РФ';
COMMENT ON COLUMN staging.fgislk_common_info.region_name IS 'Наименование субъекта РФ';
COMMENT ON COLUMN staging.fgislk_common_info.forest_district_code IS 'Код лесничества';
COMMENT ON COLUMN staging.fgislk_common_info.forest_district_name IS 'Наименование лесничества';
COMMENT ON COLUMN staging.fgislk_common_info.forest_quarter_code IS 'Номер квартала';
COMMENT ON COLUMN staging.fgislk_common_info.forest_plot_code IS 'Номер выдела';
COMMENT ON COLUMN staging.fgislk_common_info.forest_plot_area IS 'Площадь выдела, га';
COMMENT ON COLUMN staging.fgislk_common_info.forest_plot_characteristic IS 'Характеристика выдела';
COMMENT ON COLUMN staging.fgislk_common_info.forest_type IS 'Тип лесорастительных условий';
COMMENT ON COLUMN staging.fgislk_common_info.dominant_species IS 'Преобладающая порода';
COMMENT ON COLUMN staging.fgislk_common_info.age_class IS 'Класс возраста';
COMMENT ON COLUMN staging.fgislk_common_info.forest_group IS 'Группа лесов';
COMMENT ON COLUMN staging.fgislk_common_info.forest_category IS 'Категория лесов';
COMMENT ON COLUMN staging.fgislk_common_info.protection_category IS 'Категория защитности';
COMMENT ON COLUMN staging.fgislk_common_info.purpose IS 'Целевое назначение лесов';
COMMENT ON COLUMN staging.fgislk_common_info.inventory_date IS 'Дата проведения таксации';
COMMENT ON COLUMN staging.fgislk_common_info.notes IS 'Примечания';
























SELECT  ind, upload_file_id, COUNT(*) AS total
FROM staging.forest_stand_kml_import
GROUP by upload_file_id, ind
HAVING COUNT(*) > 1;

SELECT ind, upload_file_id, kvart, vydel, COUNT(*) AS total
FROM staging.forest_stand_kml_import
GROUP BY upload_file_id, ind, kvart, vydel
HAVING COUNT(*) > 1
order by ind,  upload_file_id,  kvart, vydel ;

select *  from staging.forest_stand_kml_import fski where ind='Киретское_1_22'

select *  from staging.forest_stand_kml_import fski where ind='Киретское_1_6'

SELECT id,ind, fski.index_field , kvart, vydel
FROM staging.forest_stand_kml_import fski
WHERE EXISTS (
    SELECT 1
    FROM staging.forest_stand_kml_import fski2
    WHERE fski2.upload_file_id = fski.upload_file_id
      AND fski2.ind = fski.ind
    GROUP BY fski2.upload_file_id, fski2.ind
    HAVING COUNT(*) > 1
)
order by ind;


select *  from staging.forest_stand_kml_import fski where ind='Бичурское_170_33'

select *  from staging.forest_stand_kml_import fski where ind is null

select *  from staging.forest_stand_kml_import fski where ind='Окино-Ключевское_148_2'

select id,ind,index_field,kvart,vydel from staging.forest_stand_kml_import where ind <> index_field;

select id,ind,index_field,kvart,vydel from staging.forest_stand_kml_import where zapas =0 order by ind;

select id,ind,index_field,kvart,vydel from staging.forest_stand_kml_import where kvart=0 and vydel is null order by ind ;




select *  from staging.forest_stand_kml_import fski where ind='Окино-Ключевское_84_10'

select distinct ind  from staging.forest_stand_kml_import fski order by ind

SELECT COUNT(DISTINCT ind) FROM staging.forest_stand_kml_import fski;

select count(ind)  from staging.forest_stand_kml_import fski

SELECT ind, upload_file_id, COUNT(*) AS total
FROM staging.forest_stand_kml_import
GROUP BY upload_file_id, ind
HAVING COUNT(*) > 1;


select *  from staging.forest_stand_kml_import fski where ind='Киретское_1_22'

select *  from staging.forest_stand_kml_import fski where ind like 'Бичурское%'

select *  from staging.forest_stand_kml_import fski where ind='Куналейское_0_null'

select count(*)  from staging.forest_stand_kml_import fski where kate_1 is null

select *  from staging.forest_stand_kml_import fski where kate_1 is null or kate_1='Вырубка'

select *  from staging.forest_stand_kml_import fski where zapas=0





select distinct gir,lesni from staging.forest_stand_kml_import

select distinct lesni,ind from staging.forest_stand_kml_import

select distinct mu,gir,ugir from staging.forest_stand_kml_import

CREATE TABLE staging.forest_stand_kml_mapper (
                                                 user_id varchar NOT NULL,
                                                 lesni varchar(255) NOT NULL,
                                                 forestry_unit_id int8 NOT NULL
);

INSERT INTO staging.forest_stand_kml_mapper (user_id, lesni, forestry_unit_id)
SELECT DISTINCT 1, lesni, 0
FROM staging.forest_stand_kml_import;

create trigger trigger_forest_stand_area before
    insert
    or
update
    of geometry on
    public.forest_stand for each row execute function update_forest_stand_area()

ALTER TABLE forest_stand
ALTER COLUMN geometry TYPE geometry(MULTIPOLYGON, 4326)
  USING ST_Multi(geometry);

select


INSERT INTO forest_stand (
    forestry_unit_id,
    number_in_quarter,
    full_number,
    name,
    description,
    composition,
    predominant_species,
    age,
    height,
    diameter,
    bonitet,
    forest_type,
    tlu,
    fullness,
    stock,
    category,
    protection_category,
    group_type,
    ozu,
    geometry,
    area_m2,
    area_ha,
    verified,
    source_data,
    relevance_year,
    created_at,
    updated_at
)
SELECT
    q.id AS forestry_unit_id,
    fi.kvart AS number_in_quarter,
    CONCAT(fu.name, '_', fi.kvart, '_', fi.vydel) AS full_number,
    fi.ozu AS name,
    COALESCE(fi.ozu, CONCAT('Выдел ', fi.kvart, '-', fi.vydel)) AS description,
    fi.sosta AS composition,
    fi.preob AS predominant_species,
    fi.vozra::integer AS age,
    fi.vysot::numeric(10,2) AS height,
    fi.diame::numeric(10,2) AS diameter,
    fi.bonit AS bonitet,
    fi.tip_l AS forest_type,
    fi.tlu AS tlu,
    fi.polno AS fullness,
    fi.zapas::numeric(10,2) AS stock,
    fi.kateg AS category,
    fi.kate_1 AS protection_category,
    fi.grupp AS group_type,
    fi.ozu AS ozu,
    fi.geometry AS geometry,
    ST_Area(ST_Transform(fi.geometry, 3857)) AS area_m2,
    fi.plosha::numeric(10,2) AS area_ha,
    false AS verified,
    to_jsonb(fi.*) AS source_data,
    EXTRACT(YEAR FROM NOW())::integer AS relevance_year,
    NOW() AS created_at,
    NOW() AS updated_at
FROM
    staging.forest_stand_kml_import fi
        INNER JOIN staging.forest_stand_kml_mapper mapper
                   ON fi.lesni = mapper.lesni
        INNER JOIN forestry_units fu
                   ON mapper.forestry_unit_id = fu.id
                       AND fu.type = 'SUB_FORESTRY'
        INNER JOIN forestry_units q
                   ON q.parent_id = fu.id
                       AND q.type = 'FOREST_QUARTER'
                       AND q.number = fi.kvart::varchar
WHERE
    fi.vydel IS NOT NULL
  AND fi.vydel != ''
  AND fi.kvart IS NOT NULL
  AND q.id IS NOT NULL;





SELECT id, vysot FROM staging.forest_stand_kml_import WHERE vysot ~ ',';
SELECT id, diame FROM staging.forest_stand_kml_import WHERE diame ~ ',';
SELECT id, zapas FROM staging.forest_stand_kml_import WHERE zapas ~ ',';
SELECT id, plosha FROM staging.forest_stand_kml_import WHERE plosha ~ ',';


select count(id) from staging.forest_stand_kml_import