package com.alhrb.forestry.files;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDto {
    private Long id;
    private Long userId;
    private String type;
    private String originalName;
    private String storedName;
    private String relativePath;
    private String sha256;
    private String contentType;
    private String extension;
    private Long size;
    private String status;
    private Boolean processed;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String errorMessage;
}
