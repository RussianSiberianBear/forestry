package com.alhrb.forestry.controller;

import com.alhrb.forestry.files.StoredFileService;
import com.alhrb.forestry.files.ZipExtractResultDto;
import com.alhrb.forestry.service.importer.ForestStandKmlImportService;
import com.alhrb.forestry.service.importer.KmlImportResult;
import com.alhrb.forestry.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ForestStandController {
    private static final long MAX_ZIP_SIZE = 50L * 1024 * 1024;

    private final SecurityHelper securityHelper;
    private final StoredFileService storedFileService;
    private final ForestStandKmlImportService forestStandKmlImportService;

    @PostMapping(value = "/uploadForestStand", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadForestStand(@RequestParam("file") MultipartFile file) {
        Long userId = securityHelper.getCurrentUserId();
        ZipExtractResultDto extractResult = null;

        try {
            validate(file);
            extractResult = storedFileService.uploadAndExtractZip(userId, file);

            List<KmlImportResult> imports = new ArrayList<>();
            for (ZipExtractResultDto.ExtractedFileInfo extractedFile : extractResult.getFiles()) {
                if (!"kml".equalsIgnoreCase(extractedFile.getExtension())) {
                    continue;
                }

                Path kmlPath = extractedFile.getPath().toAbsolutePath().normalize();
                imports.add(forestStandKmlImportService.importKml(
                        extractResult.getStorageId(),
                        kmlPath
                ));
            }

            if (imports.isEmpty()) {
                throw new IllegalArgumentException("В архиве не найден ни один KML-файл");
            }

            int total = imports.stream().mapToInt(KmlImportResult::totalPlacemarkCount).sum();
            int imported = imports.stream().mapToInt(KmlImportResult::importedCount).sum();
            int errors = imports.stream().mapToInt(KmlImportResult::errorCount).sum();

            if (errors == 0) {
                storedFileService.markProcessed(extractResult.getStorageId(), userId);
            } else {
                storedFileService.markProcessingError(
                        extractResult.getStorageId(),
                        userId,
                        "Импорт завершён с ошибками: " + errors
                );
            }

            return ResponseEntity.ok(Map.of(
                    "success", errors == 0,
                    "message", errors == 0
                            ? "ZIP-архив распакован, KML загружен во временную таблицу"
                            : "KML загружен частично; некоторые Placemark содержат ошибки",
                    "storageId", extractResult.getStorageId(),
                    "totalPlacemarkCount", total,
                    "importedCount", imported,
                    "errorCount", errors,
                    "files", imports
            ));
        } catch (IllegalArgumentException e) {
            markErrorIfStored(extractResult, userId, e);
            log.warn("Загрузка лесных выделов отклонена: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IOException e) {
            markErrorIfStored(extractResult, userId, e);
            log.error("Ошибка обработки архива лесных выделов", e);
            return ResponseEntity.unprocessableEntity().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            markErrorIfStored(extractResult, userId, e);
            log.error("Неожиданная ошибка загрузки лесных выделов", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Внутренняя ошибка сервера"
            ));
        }
    }

    private void markErrorIfStored(ZipExtractResultDto result, Long userId, Exception exception) {
        if (result == null || result.getStorageId() == null) {
            return;
        }
        try {
            storedFileService.markProcessingError(
                    result.getStorageId(),
                    userId,
                    exception.getMessage()
            );
        } catch (Exception statusException) {
            log.error("Не удалось записать статус ошибки storageId={}", result.getStorageId(), statusException);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не выбран или пуст");
        }
        if (!storedFileService.isZipFile(file)) {
            throw new IllegalArgumentException("Для загрузки лесных выделов требуется ZIP-архив с KML-файлом");
        }
        if (file.getSize() > MAX_ZIP_SIZE) {
            throw new IllegalArgumentException("Размер ZIP-архива превышает 50 МБ");
        }
    }
}
