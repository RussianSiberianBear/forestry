package com.alhrb.forestry.files.model.staging;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "uploaded_files", schema = "staging")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Lob
    @Column(name = "file_data", nullable = false, columnDefinition = "BYTEA")
    private byte[] fileData;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @CreationTimestamp
    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "UPLOADED";

    @Column(name = "processed")
    private Boolean processed = false;

    @Column(name = "arhicve_id", nullable = false)
    private Long archiveId;
}
