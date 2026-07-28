package com.alhrb.forestry.staging.service;

import com.alhrb.forestry.staging.dto.FgislkCsvRow;
import com.alhrb.forestry.staging.model.FgislkCommonInfo;
import com.alhrb.forestry.staging.repository.FgislkCommonInfoRepository;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FgislkCommonInfoImportService {

    private final FgislkCommonInfoRepository repository;

    @Value("${import.batch-size:1000}")
    private int batchSize;

    @Value("${import.encoding:UTF-8}")
    private String encoding;

    /**
     * Основной метод импорта
     */
    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        log.info("Начало импорта файла: {}", file.getOriginalFilename());
        long startTime = System.currentTimeMillis();

        try {
            // 1. Определяем кодировку
            String detectedEncoding = detectEncoding(file);
            log.info("Определена кодировка: {}", detectedEncoding);

            // 2. Очищаем таблицу
            repository.truncateTable();
            log.info("Таблица staging.fgislk_common_info очищена");

            // 3. Парсим CSV
            List<FgislkCsvRow> csvRows = parseCsv(file, detectedEncoding);
            log.info("Распарсено {} строк из CSV", csvRows.size());

            // 4. Конвертируем в Entity и сохраняем
            List<FgislkCommonInfo> entities = convertToEntities(csvRows);
            List<FgislkCommonInfo> savedEntities = saveInBatches(entities);

            long endTime = System.currentTimeMillis();
            log.info("Импорт завершен. Загружено {} записей за {} мс",
                    savedEntities.size(), (endTime - startTime));

            return ImportResult.success(savedEntities.size(), (endTime - startTime));

        } catch (Exception e) {
            log.error("Ошибка при импорте CSV", e);
            return ImportResult.failure(e.getMessage());
        }
    }

    /**
     * Определение кодировки файла
     */
    private String detectEncoding(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();

        // Проверка BOM
        if (bytes.length >= 3) {
            // UTF-8 BOM
            if (bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return "UTF-8";
            }
            // UTF-16 LE BOM
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                return "UTF-16LE";
            }
            // UTF-16 BE BOM
            if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                return "UTF-16BE";
            }
        }

        // Проверка на Windows-1251 (русские тексты)
        for (byte b : bytes) {
            if (b < 0) {
                return "Windows-1251";
            }
        }

        return "UTF-8";
    }

    /**
     * Парсинг CSV
     */
    private List<FgislkCsvRow> parseCsv(MultipartFile file, String encoding) throws Exception {
        Charset charset = Charset.forName(encoding);

        try (Reader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), charset))) {

            CsvToBean<FgislkCsvRow> csvToBean = new CsvToBeanBuilder<FgislkCsvRow>(reader)
                    .withType(FgislkCsvRow.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .withSkipLines(1) // Пропускаем заголовок
                    .withThrowExceptions(false) // Пропускаем ошибочные строки
                    .build();

            return csvToBean.parse();
        }
    }

    /**
     * Преобразование CSV строк в Entity
     */
    private List<FgislkCommonInfo> convertToEntities(List<FgislkCsvRow> csvRows) {
        List<FgislkCommonInfo> entities = new ArrayList<>();
        int errorCount = 0;

        for (FgislkCsvRow row : csvRows) {
            try {
                FgislkCommonInfo entity = FgislkCommonInfo.builder()
                        .regionCode(cleanString(row.getRegionCode()))
                        .regionName(cleanString(row.getRegionName()))
                        .forestDistrictCode(cleanString(row.getForestDistrictCode()))
                        .forestDistrictName(cleanString(row.getForestDistrictName()))
                        .forestQuarterCode(cleanString(row.getForestQuarterCode()))
                        .forestPlotCode(cleanString(row.getForestPlotCode()))
                        .forestPlotArea(parseArea(row.getForestPlotArea()))
                        .forestPlotCharacteristic(cleanString(row.getForestPlotCharacteristic()))
                        .forestType(cleanString(row.getForestType()))
                        .dominantSpecies(cleanString(row.getDominantSpecies()))
                        .ageClass(cleanString(row.getAgeClass()))
                        .forestGroup(cleanString(row.getForestGroup()))
                        .forestCategory(cleanString(row.getForestCategory()))
                        .protectionCategory(cleanString(row.getProtectionCategory()))
                        .purpose(cleanString(row.getPurpose()))
                        .inventoryDate(row.getInventoryDate())
                        .notes(cleanString(row.getNotes()))
                        .build();

                entities.add(entity);

            } catch (Exception e) {
                errorCount++;
                log.warn("Ошибка при преобразовании строки: {}", row, e);
            }
        }

        if (errorCount > 0) {
            log.warn("Пропущено {} ошибочных строк", errorCount);
        }

        return entities;
    }

    /**
     * Сохранение батчами
     */
    private List<FgislkCommonInfo> saveInBatches(List<FgislkCommonInfo> entities) {
        List<FgislkCommonInfo> saved = new ArrayList<>();

        for (int i = 0; i < entities.size(); i += batchSize) {
            int end = Math.min(i + batchSize, entities.size());
            List<FgislkCommonInfo> batch = entities.subList(i, end);

            List<FgislkCommonInfo> savedBatch = repository.saveAll(batch);
            saved.addAll(savedBatch);

            log.debug("Сохранен батч {}-{} из {}", i + 1, end, entities.size());
        }

        return saved;
    }

    /**
     * Очистка строки от лишних символов
     */
    private String cleanString(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Парсинг площади
     */
    private BigDecimal parseArea(String areaStr) {
        if (areaStr == null || areaStr.trim().isEmpty()) {
            return null;
        }
        try {
            String cleaned = areaStr.trim().replace(",", ".");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Не удалось распарсить площадь: {}", areaStr);
            return null;
        }
    }

    /**
     * Класс результата импорта
     */
    public static class ImportResult {
        private final boolean success;
        private final int importedCount;
        private final long processingTimeMs;
        private final String errorMessage;

        private ImportResult(boolean success, int importedCount, long processingTimeMs, String errorMessage) {
            this.success = success;
            this.importedCount = importedCount;
            this.processingTimeMs = processingTimeMs;
            this.errorMessage = errorMessage;
        }

        public static ImportResult success(int count, long timeMs) {
            return new ImportResult(true, count, timeMs, null);
        }

        public static ImportResult failure(String message) {
            return new ImportResult(false, 0, 0, message);
        }

        public boolean isSuccess() { return success; }
        public int getImportedCount() { return importedCount; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public String getErrorMessage() { return errorMessage; }
    }

    // Методы для проверки данных
    public boolean hasData() {
        return repository.hasData();
    }

    public long getCount() {
        return repository.count();
    }

    public List<Object[]> getRegionStatistics() {
        return repository.getRegionStatistics();
    }

    public Object[] getTotalStatistics() {
        return repository.getTotalStatistics();
    }
}