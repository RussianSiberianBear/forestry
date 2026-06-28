package com.alhrb.forestry.model.staging;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "kml_import", schema = "staging")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KmlImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_file_id", nullable = false)
    private Long uploadFileId;

    // Поля из KML
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

    @Column(name = "y", length = 50)
    private String y;

    @Column(name = "x", length = 50)
    private String x;

    // Координаты из KML
    @Column(name = "coordinates", nullable = false, columnDefinition = "TEXT")
    private String coordinates;

    @Column(name = "geometry", columnDefinition = "GEOMETRY")
    private String geometry;

    // Метаданные импорта
    @Column(name = "import_status", length = 20)
    private String importStatus = "PENDING";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
