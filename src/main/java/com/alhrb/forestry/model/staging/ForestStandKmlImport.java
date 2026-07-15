package com.alhrb.forestry.model.staging;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Geometry;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "forest_stand_kml_import",
        schema = "staging",
        indexes = {
                @Index(
                        name = "idx_kml_import_file_id",
                        columnList = "upload_file_id"
                ),
                @Index(
                        name = "idx_kml_import_status",
                        columnList = "import_status"
                ),
                @Index(
                        name = "idx_kml_import_mk",
                        columnList = "mk"
                ),
                @Index(
                        name = "idx_kml_import_zk",
                        columnList = "zk"
                ),
                @Index(
                        name = "idx_kml_import_kvart",
                        columnList = "kvart"
                ),
                @Index(
                        name = "idx_kml_import_vydel",
                        columnList = "vydel"
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForestStandKmlImport {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "forest_stand_kml_import_seq"
    )
    @SequenceGenerator(
            name = "forest_stand_kml_import_seq",
            sequenceName = "staging.kml_import_id_seq",
            allocationSize = 500
    )
    private Long id;

    @Column(name = "upload_file_id", nullable = false)
    private Long uploadFileId;

    @Column(name = "mk")
    private Integer mk;

    @Column(name = "zk")
    private Integer zk;

    @Column(name = "vmr", length = 50)
    private String vmr;

    @Column(name = "bon")
    private Integer bon;

    @Column(name = "mtip", length = 50)
    private String mtip;

    @Column(name = "strata")
    private Integer strata;

    @Column(name = "ugir_1", length = 255)
    private String ugir1;

    @Column(name = "ind", length = 255)
    private String ind;

    @Column(name = "lesho", length = 255)
    private String lesho;

    @Column(name = "lesni", length = 255)
    private String lesni;

    @Column(name = "grupp", length = 255)
    private String grupp;

    @Column(name = "kateg", length = 255)
    private String kateg;

    @Column(name = "ozu", length = 255)
    private String ozu;

    @Column(name = "kvart", precision = 10, scale = 2)
    private BigDecimal kvart;

    @Column(name = "vydel", length = 50)
    private String vydel;

    /*
     * В KML поле "площа" объявлено как string и может содержать
     * локализованный десятичный разделитель, поэтому в staging
     * сохраняем исходное значение строкой.
     */
    @Column(name = "plosha", length = 50)
    private String plosha;

    @Column(name = "kate_1", length = 255)
    private String kate1;

    @Column(name = "sosta", length = 50)
    private String sosta;

    @Column(name = "preob", length = 50)
    private String preob;

    @Column(name = "vozra", precision = 10, scale = 2)
    private BigDecimal vozra;

    @Column(name = "vysot", precision = 10, scale = 2)
    private BigDecimal vysot;

    @Column(name = "diame", precision = 10, scale = 2)
    private BigDecimal diame;

    @Column(name = "klass", precision = 10, scale = 2)
    private BigDecimal klass;

    @Column(name = "bonit", length = 50)
    private String bonit;

    @Column(name = "tip_l", length = 255)
    private String tipL;

    @Column(name = "tlu", length = 50)
    private String tlu;

    /*
     * В исходном KML это строка и значение может быть "0,7".
     * В staging лучше сохранить исходный текст без потери формата.
     */
    @Column(name = "polno", length = 50)
    private String polno;

    @Column(name = "zapas", precision = 10, scale = 2)
    private BigDecimal zapas;

    @Column(name = "index_field", length = 255)
    private String indexField;

    @Column(name = "mu", length = 255)
    private String mu;

    @Column(name = "gir", length = 255)
    private String gir;

    @Column(name = "ugir", length = 255)
    private String ugir;

    /*
     * X и Y в исходном KML имеют запятую в качестве
     * десятичного разделителя, поэтому храним исходное значение.
     */
    @Column(name = "y", length = 50)
    private String y;

    @Column(name = "x", length = 50)
    private String x;

    @Column(
            name = "coordinates",
            nullable = false,
            columnDefinition = "text"
    )
    private String coordinates;

    /*
     * Используем общий Geometry, поскольку KML теоретически может
     * содержать как Polygon, так и MultiPolygon.
     */
    @Column(
            name = "geometry",
            columnDefinition = "geometry(Geometry,4326)"
    )
    private Geometry geometry;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_status", nullable = false, length = 20)
    private ImportStatus importStatus = ImportStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}