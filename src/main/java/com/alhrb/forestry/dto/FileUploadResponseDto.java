package com.alhrb.forestry.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDto {
    private Long id;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private LocalDateTime uploadDate;
    private String status;
    private Boolean processed;
    private String sha256;
    private String relativePath;
}
