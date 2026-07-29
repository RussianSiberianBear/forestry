package com.alhrb.forestry.staging.controller;

import com.alhrb.forestry.files.FileUploadServiceImpl;
import com.alhrb.forestry.files.ZipExtractResultDto;
import com.alhrb.forestry.staging.service.FgislkCommonInfoImportService;
import com.alhrb.forestry.staging.model.FgislkCommonInfo;
import com.alhrb.forestry.staging.repository.FgislkCommonInfoRepository;
import com.alhrb.forestry.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/uploadFromFgislk")
@RequiredArgsConstructor

public class FgislkUploadController {

    private final FgislkCommonInfoImportService importService;
    private final FileUploadServiceImpl uploadService;
    private final FgislkCommonInfoRepository repository;
    private final SecurityHelper securityHelper;

    @PostMapping("/importFromFGISLK")
    public ResponseEntity<Map<String, Object>> importFromFgisLK(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Файл не выбран или пустой"
            ));
        }

        // Проверка расширения
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".zip")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Поддерживаются только zip-файлы"
            ));
        }

        Long userId = securityHelper.getCurrentUserId();

        try {
    //        FgislkCommonInfoImportService.ImportResult result = importService.importCsv(file);
            ZipExtractResultDto resultDto = uploadService.uploadAndExtractZip(userId,file);


            Map<String, Object> response = new HashMap<>();
            response.put("success", resultDto.isSuccess());
            response.put("importedCount", resultDto.getTotalFiles());

            if (resultDto.isSuccess()) {
                response.put("message", "Данные успешно загружены");
                response.put("totalInTable", importService.getCount());
            } else {
                response.put("error", resultDto.getErrorMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при импорте", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }

    }

}
