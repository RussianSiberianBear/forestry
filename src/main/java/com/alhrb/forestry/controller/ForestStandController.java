package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.FileUploadResponseDto;
import com.alhrb.forestry.files.FileUploadService;
import com.alhrb.forestry.files.ZipExtractResultDto;
import com.alhrb.forestry.files.ZipExtractorService;
import com.alhrb.forestry.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ForestStandController {

    private final SecurityHelper securityHelper;
    private final FileUploadService fileUploadService;
    private final ZipExtractorService zipExtractorService;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "xlsx", "xls", "csv", "xml", "pdf", "doc", "docx", "zip"
    );
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB для ZIP

    @PostMapping("/uploadForestStand")
    public ResponseEntity<?> uploadForestStand(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "extractZip", defaultValue = "true") boolean extractZip) {

        Long userId = securityHelper.getCurrentUserId();

        // 1. Проверка обязательных параметров
        if (userId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Не найден ID пользователя"));
        }

        // 2. Валидация файла
        ValidationResult validation = validateFile(file);
        if (!validation.isValid()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", validation.getErrorMessage()));
        }

        try {
            // 3. Сохраняем файл в БД
            FileUploadResponseDto savedFile = fileUploadService.uploadFile(userId, file);

            // 4. Сохраняем физический файл
            Path physicalPath = fileUploadService.savePhysicalFile(file, savedFile.getId(), userId);

            // 5. Если это ZIP и нужно распаковать
            ZipExtractResultDto extractResult = null;
            if (fileUploadService.isZipFile(file) && extractZip) {
                extractResult = zipExtractorService.extractZip(
                        physicalPath,
                        userId,
                        savedFile.getId()
                );
            }

            Map<String, Object> data = Map.of(
                    "fileInfo", savedFile,
                    "physicalPath", physicalPath.toString(),
                    "isZip", fileUploadService.isZipFile(file),
                    "extractResult", extractResult
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Файл успешно загружен",
                    "data", data
            ));

        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (IOException e) {
            log.error("Ошибка при загрузке файла", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Ошибка сервера при сохранении файла: " +
                            e.getMessage()));
        } catch (Exception e) {
            log.error("Неожиданная ошибка", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Внутренняя ошибка сервера"));
        }
    }

    @PostMapping("/uploadZipAndExtract")
    public ResponseEntity<?> uploadZipAndExtract(
            @RequestParam("file") MultipartFile file) {

        Long userId = securityHelper.getCurrentUserId();

        if (!fileUploadService.isZipFile(file)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Файл должен быть ZIP-архивом"));
        }

        try {
            // Сохраняем архив
            FileUploadResponseDto savedFile = fileUploadService.uploadFile(userId, file);
            Path physicalPath = fileUploadService.savePhysicalFile(file, savedFile.getId(), userId);

            // Распаковываем
            ZipExtractResultDto extractResult = zipExtractorService.extractZip(
                    physicalPath,
                    userId,
                    savedFile.getId()
            );

            Map<String, Object> data = Map.of(
                    "archiveInfo", savedFile,
                    "extractResult", extractResult
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ZIP-архив успешно загружен и распакован",
                    "data", data
            ));

        } catch (Exception e) {
            log.error("Ошибка обработки ZIP-архива", e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Ошибка обработки ZIP-архива: " +
                            e.getMessage()));
        }
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private ValidationResult validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.error("Файл не выбран или пуст");
        }

        String originalFilename = file.getOriginalFilename();
        if (!isValidFileExtension(originalFilename)) {
            return ValidationResult.error(
                    "Неподдерживаемый формат файла. Допустимые: " +
                            String.join(", ", ALLOWED_EXTENSIONS)
            );
        }

        long maxSize = fileUploadService.isZipFile(file) ? MAX_FILE_SIZE : 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return ValidationResult.error(
                    String.format("Размер файла превышает %dMB", maxSize / (1024 * 1024))
            );
        }

        return ValidationResult.success();
    }

    private boolean isValidFileExtension(String filename) {
        if (filename == null) return false;
        String ext = fileUploadService.getFileExtension(filename).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
