package com.alhrb.forestry.files;

import com.alhrb.forestry.common.specification.DynamicSpecificationBuilder;
import com.alhrb.forestry.common.specification.GridPageableBuilder;
import com.alhrb.forestry.config.DirectoryConfig;
import com.alhrb.forestry.dto.abgrid.GridP;
import com.alhrb.forestry.files.model.staging.StoredFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoredFileServiceImpl implements StoredFileService {
    private static final String STATUS_SAVING = "SAVING";
    private static final String STATUS_UPLOADED = "UPLOADED";
    private static final String STATUS_EXTRACTED = "EXTRACTED";
    private static final String STATUS_PROCESSED = "PROCESSED";
    private static final String STATUS_PROCESSING_ERROR = "PROCESSING_ERROR";

    private static final Set<String> FILTER_FIELDS = Set.of(
            "id",
            "userId",
            "type",
            "originalName",
            "storedName",
            "relativePath",
            "sha256",
            "contentType",
            "extension",
            "size",
            "status",
            "createdAt",
            "processedAt",
            "errorMessage"
    );

    private static final Set<String> SORT_FIELDS = Set.of(
            "id",
            "userId",
            "type",
            "originalName",
            "storedName",
            "relativePath",
            "sha256",
            "contentType",
            "extension",
            "size",
            "status",
            "createdAt",
            "processedAt",
            "errorMessage"
    );

    private final StoredFileRepository repository;
    private final ZipExtractorService zipExtractorService;

    @Override
    @Transactional
    public StoredFileDto uploadFile(Long userId, MultipartFile file) throws IOException {
        validate(userId, file);

        String checksum = sha256(file);
        repository.findFirstByUserIdAndSha256(userId, checksum).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "Этот файл уже загружен (ID " + existing.getId() + ", имя: " + existing.getOriginalName() + ")"
            );
        });

        String originalName = safeOriginalName(file.getOriginalFilename());
        String extension = getFileExtension(originalName).toLowerCase();
        String storedName = extension.isBlank() ? "source" : "source." + extension;

        StoredFile entity = StoredFile.builder()
                .userId(userId)
                .type(resolveType(extension))
                .originalName(originalName)
                .storedName(storedName)
                .relativePath("pending")
                .sha256(checksum)
                .contentType(file.getContentType())
                .extension(extension)
                .size(file.getSize())
                .status(STATUS_SAVING)
                .processed(false)
                .build();

        entity = repository.saveAndFlush(entity);
        Path target = null;
        try {
            target = savePhysicalFile(file, entity.getId(), userId);
            entity.setRelativePath(toRelativePath(target));
            entity.setStatus(STATUS_UPLOADED);
            return StoredFileDto.fromEntity(repository.save(entity));
        } catch (Exception e) {
            deleteStorageDirectory(userId, entity.getId());
            repository.delete(entity);
            if (e instanceof IOException io) {
                throw io;
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public ZipExtractResultDto uploadAndExtractZip(Long userId, MultipartFile file) throws IOException {
        if (!isZipFile(file)) {
            throw new IllegalArgumentException("Файл должен быть ZIP-архивом");
        }

        StoredFileDto saved = uploadFile(userId, file);
        try {
            ZipExtractResultDto result = zipExtractorService.extractZip(
                    getPhysicalFilePath(saved.id(), userId), userId, saved.id()
            );
            result.setStorageId(saved.id());
            updateFileStatus(saved.id(), STATUS_EXTRACTED);
            return result;
        } catch (Exception e) {
            deleteFile(saved.id(), userId);
            if (e instanceof IOException io) {
                throw io;
            }
            throw e;
        }
    }

    @Override
    public Path savePhysicalFile(MultipartFile file, Long fileId, Long userId) throws IOException {
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();
        Path directory = storageDirectory(userId, fileId);
        Files.createDirectories(directory);

        Path target = directory.resolve(extension.isBlank() ? "source" : "source." + extension);
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    @Override
    public Path getPhysicalFilePath(Long fileId, Long userId) throws IOException {
        StoredFile file = userId == null
                ? repository.findById(fileId).orElseThrow(() -> new IllegalArgumentException("Файл не найден"))
                : repository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден или недоступен"));

        if (file.getRelativePath() == null || file.getRelativePath().isBlank() || "pending".equals(file.getRelativePath())) {
            throw new IOException("Для файла не сохранён путь на диске");
        }

        Path path = uploadRoot().resolve(file.getRelativePath()).normalize();
        if (!path.startsWith(uploadRoot()) || !Files.isRegularFile(path)) {
            throw new IOException("Физический файл не найден: " + path);
        }
        return path;
    }

    @Override
    public Map<String, Object> getUserFiles(GridP params) {

        Specification<StoredFile> specification =
                DynamicSpecificationBuilder.build(
                        params.getFilter(),
                        FILTER_FIELDS
                );

        Pageable pageable =
                GridPageableBuilder.build(
                        params,
                        SORT_FIELDS
                );

        Page page = repository
                .findAll(specification, pageable)
                .map(StoredFileDto::fromEntity);

        Map<String, Object> data = Map.of(
                "rows", page.getContent(),
                "totalRecords", page.getTotalElements()
        );
        return Map.of("success", true, "message", "OK", "data", data);
    }

    @Override
    public byte[] getFileData(Long fileId) {
        try {
            return Files.readAllBytes(getPhysicalFilePath(fileId, null));
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public byte[] getFileData(Long fileId, Long userId) {
        try {
            return Files.readAllBytes(getPhysicalFilePath(fileId, userId));
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        StoredFile file = repository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден или недоступен"));
        deleteStorageDirectory(userId, fileId);
        repository.delete(file);
    }

    @Override
    @Transactional
    public StoredFileDto updateFileStatus(Long fileId, String status) {
        StoredFile file = repository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));
        file.setStatus(status);
        return StoredFileDto.fromEntity(repository.save(file));
    }

    @Override
    @Transactional
    public StoredFileDto markProcessed(Long fileId, Long userId) {
        StoredFile file = repository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден или недоступен"));
        file.setStatus(STATUS_PROCESSED);
        file.setProcessed(true);
        file.setProcessedAt(LocalDateTime.now());
        file.setErrorMessage(null);
        return StoredFileDto.fromEntity(repository.save(file));
    }

    @Override
    @Transactional
    public StoredFileDto markProcessingError(Long fileId, Long userId, String errorMessage) {
        StoredFile file = repository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден или недоступен"));
        file.setStatus(STATUS_PROCESSING_ERROR);
        file.setProcessed(false);
        file.setProcessedAt(null);
        file.setErrorMessage(limit(errorMessage, 2000));
        return StoredFileDto.fromEntity(repository.save(file));
    }

    @Override
    public List<StoredFileDto> getFilesByStatus(String status) {
        return repository.findByStatus(status).stream().map(StoredFileDto::fromEntity).toList();
    }

    @Override
    public boolean isZipFile(MultipartFile file) {
        return file != null && "zip".equalsIgnoreCase(getFileExtension(file.getOriginalFilename()));
    }

    @Override
    public boolean isZipFile(Path path) {
        return path != null && "zip".equalsIgnoreCase(getFileExtension(path.getFileName().toString()));
    }

    @Override
    public String getFileExtension(String name) {
        if (name == null) return "";
        int index = name.lastIndexOf('.');
        return index >= 0 && index < name.length() - 1 ? name.substring(index + 1) : "";
    }

    private String resolveType(String extension) {
        return switch (extension.toLowerCase()) {
            case "zip" -> "ARCHIVE";
            case "kml" -> "KML";
            case "kmz" -> "KMZ";
            default -> "DOCUMENT";
        };
    }

    private Path storageDirectory(Long userId, Long fileId) {
        return uploadRoot()
                .resolve("users")
                .resolve(String.valueOf(userId))
                .resolve("storage")
                .resolve(String.valueOf(fileId));
    }

    private void deleteStorageDirectory(Long userId, Long fileId) {
        deleteRecursively(storageDirectory(userId, fileId));
    }

    private Path uploadRoot() {
        return DirectoryConfig.getAbsoluteFileUploadPath().toAbsolutePath().normalize();
    }

    private String toRelativePath(Path path) {
        return uploadRoot().relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private void validate(Long userId, MultipartFile file) {
        if (userId == null) throw new IllegalArgumentException("Не найден ID пользователя");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Файл не выбран или пуст");
    }

    private String safeOriginalName(String name) {
        if (name == null || name.isBlank()) return "unnamed";
        String clean = Paths.get(name).getFileName().toString().replaceAll("[\\p{Cntrl}]", "_");
        return clean.length() <= 255 ? clean : clean.substring(clean.length() - 255);
    }

    private String sha256(MultipartFile file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, count);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private void deleteRecursively(Path root) {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("Не удалось удалить {}", path, e);
                }
            });
        } catch (IOException e) {
            log.warn("Не удалось очистить {}", root, e);
        }
    }
}
