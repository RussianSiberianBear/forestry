package com.alhrb.forestry.staging.controller;

import com.alhrb.forestry.staging.service.FgislkCommonInfoImportService;
import com.alhrb.forestry.staging.model.FgislkCommonInfo;
import com.alhrb.forestry.staging.repository.FgislkCommonInfoRepository;
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
@RequestMapping("/api/fgislkCoomonInfo")
@RequiredArgsConstructor
public class FgislkCommonInfoController {

    private final FgislkCommonInfoImportService importService;
    private final FgislkCommonInfoRepository repository;

    /**
     * Загрузка CSV файла
     */
    @PostMapping("/import_common_info_csv")
    public ResponseEntity<Map<String, Object>> importCommonInfoCsv(@RequestParam("file") MultipartFile file) {
        log.info("Получен запрос на импорт: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Файл не выбран или пустой"
            ));
        }

        // Проверка расширения
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Поддерживаются только CSV файлы"
            ));
        }

        try {
            FgislkCommonInfoImportService.ImportResult result = importService.importCsv(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("importedCount", result.getImportedCount());
            response.put("processingTimeMs", result.getProcessingTimeMs());

            if (result.isSuccess()) {
                response.put("message", "Данные успешно загружены");
                response.put("totalInTable", importService.getCount());
            } else {
                response.put("error", result.getErrorMessage());
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

    /**
     * Получение всех записей
     */
    @GetMapping("/all")
    public ResponseEntity<List<FgislkCommonInfo>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    /**
     * Получение записи по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<FgislkCommonInfo> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Получение статистики
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", importService.getCount());
        stats.put("hasData", importService.hasData());

        Object[] totalStats = importService.getTotalStatistics();
        if (totalStats != null && totalStats.length >= 4) {
            stats.put("totalCount", totalStats[0]);
            stats.put("regionsCount", totalStats[1]);
            stats.put("districtsCount", totalStats[2]);
            stats.put("totalArea", totalStats[3]);
        }

        stats.put("regions", importService.getRegionStatistics());

        return ResponseEntity.ok(stats);
    }

    /**
     * Поиск по региону
     */
    @GetMapping("/region/{regionCode}")
    public ResponseEntity<List<FgislkCommonInfo>> findByRegion(@PathVariable String regionCode) {
        return ResponseEntity.ok(repository.findByRegionCode(regionCode));
    }

    /**
     * Очистка таблицы
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearTable() {
        try {
            repository.truncateTable();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Таблица очищена"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Получение количества записей
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount() {
        return ResponseEntity.ok(Map.of("count", importService.getCount()));
    }
}