package com.alhrb.forestry.controller;

import com.alhrb.forestry.files.FileUploadService;
import com.alhrb.forestry.files.ZipExtractResultDto;
import com.alhrb.forestry.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ForestStandController {
    private static final long MAX_ZIP_SIZE = 50L * 1024 * 1024;

    private final SecurityHelper securityHelper;
    private final FileUploadService fileUploadService;

    @PostMapping(value = "/uploadForestStand", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadForestStand(@RequestParam("file") MultipartFile file) {
        Long userId = securityHelper.getCurrentUserId();
        try {
            validate(file);
            ZipExtractResultDto result = fileUploadService.uploadAndExtractZip(userId, file);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ZIP-архив загружен и KML-файлы успешно распакованы",
                    "data", result
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Загрузка лесных выделов отклонена: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IOException e) {
            log.error("Ошибка обработки архива лесных выделов", e);
            return ResponseEntity.unprocessableEntity().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Неожиданная ошибка загрузки лесных выделов", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Внутренняя ошибка сервера"));
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Файл не выбран или пуст");
        if (!fileUploadService.isZipFile(file)) throw new IllegalArgumentException("Для загрузки лесных выделов требуется ZIP-архив с KML-файлом");
        if (file.getSize() > MAX_ZIP_SIZE) throw new IllegalArgumentException("Размер ZIP-архива превышает 50 МБ");
    }
}
