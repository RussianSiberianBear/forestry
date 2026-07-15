package com.alhrb.forestry.files.model.staging;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "file_storage",
        schema = "staging",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_file_storage_user_sha256",
                columnNames = {"user_id", "sha256"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Column(name = "relative_path", nullable = false, length = 1000)
    private String relativePath;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "extension", length = 30)
    private String extension;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "processed", nullable = false)
    private Boolean processed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;
}
