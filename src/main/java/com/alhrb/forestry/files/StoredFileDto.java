package com.alhrb.forestry.files;

import com.alhrb.forestry.files.model.staging.StoredFile;

import java.time.LocalDateTime;

public record StoredFileDto(
        Long id,
        Long userId,
        String type,
        String originalName,
        String storedName,
        String relativePath,
        String sha256,
        String contentType,
        String extension,
        Long size,
        String status,
        Boolean processed,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        String errorMessage
) {
    // Статический фабричный метод для создания из Entity
    public static StoredFileDto fromEntity(StoredFile entity) {
        return new StoredFileDto(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getOriginalName(),
                entity.getStoredName(),
                entity.getRelativePath(),
                entity.getSha256(),
                entity.getContentType(),
                entity.getExtension(),
                entity.getSize(),
                entity.getStatus(),
                entity.getProcessed(),
                entity.getCreatedAt(),
                entity.getProcessedAt(),
                entity.getErrorMessage()
        );
    }

    // Метод для преобразования в Entity (если нужно)
    public StoredFile toEntity() {
        return StoredFile.builder()
                .id(id)
                .userId(userId)
                .type(type)
                .originalName(originalName)
                .storedName(storedName)
                .relativePath(relativePath)
                .sha256(sha256)
                .contentType(contentType)
                .extension(extension)
                .size(size)
                .status(status)
                .processed(processed)
                .createdAt(createdAt)
                .processedAt(processedAt)
                .errorMessage(errorMessage)
                .build();
    }
}