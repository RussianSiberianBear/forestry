package com.alhrb.forestry.staging.service;

import com.alhrb.forestry.staging.dto.FgislkCsvRow;
import com.alhrb.forestry.staging.model.FgislkCommonInfo;
import com.alhrb.forestry.staging.repository.FgislkCommonInfoRepository;
import com.alhrb.forestry.util.SecurityHelper;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FgislkCommonInfoImportService {

    private final FgislkCommonInfoRepository repository;
    private final SecurityHelper securityHelper;

    @Value("${import.batch-size:1000}")
    private int batchSize;

    @Value("${import.encoding:UTF-8}")
    private String defaultEncoding;

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
            repository.truncateTable(securityHelper.getCurrentUserId());
            log.info("Таблица staging.fgislk_common_info очищена");
/*
            // 3. Парсим CSV с BOM-фильтром
            List<FgislkCsvRow> csvRows = parseCsv(file, detectedEncoding);
            log.info("Распарсено {} строк из CSV", csvRows.size());

            // 4. Если OpenCSV не дал результатов, пробуем ручной парсинг
            if (csvRows.isEmpty() || allFieldsAreNull(csvRows)) {
                log.warn("OpenCSV не распарсил данные, пробуем ручной парсинг");
                csvRows = parseCsvManual(file, detectedEncoding);
                log.info("Ручной парсинг дал {} строк", csvRows.size());
            }
*/
            List<FgislkCsvRow> csvRows = parseCsvManual(file, detectedEncoding);

            // 5. Конвертируем в Entity и сохраняем
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
     * ===================================================
     * КЛАСС-ФИЛЬТР ДЛЯ ПРОПУСКА BOM (Byte Order Mark)
     * ===================================================
     */
    private static class BomFilterReader extends FilterReader {
        private boolean haveReadBOM = false;
        private static final char BOM = '\uFEFF';

        public BomFilterReader(Reader in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int c = super.read();
            if (!haveReadBOM && c == BOM) {
                haveReadBOM = true;
                return super.read();
            }
            haveReadBOM = true;
            return c;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            // Читаем по одному символу, чтобы корректно обрабатывать BOM
            int count = 0;
            for (int i = 0; i < len; i++) {
                int c = read();
                if (c == -1) {
                    return count > 0 ? count : -1;
                }
                cbuf[off + i] = (char) c;
                count++;
            }
            return count;
        }
    }

    /**
     * ИСПРАВЛЕННЫЙ: Парсинг CSV с BOM-фильтром
     */
    private List<FgislkCsvRow> parseCsv(MultipartFile file, String encoding) throws Exception {
        Charset charset = StandardCharsets.UTF_8;
        log.info("Парсинг CSV с кодировкой: {} и BOM-фильтром", charset);

        try (Reader reader = new BomFilterReader(
                new BufferedReader(
                        new InputStreamReader(file.getInputStream(), charset)))) {

            // Дополнительная проверка: скипаем пустые строки в начале
            CsvToBean<FgislkCsvRow> csvToBean = new CsvToBeanBuilder<FgislkCsvRow>(reader)
                    .withType(FgislkCsvRow.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .withSkipLines(1)
                    .withThrowExceptions(false)
                    .build();

            List<FgislkCsvRow> rows = csvToBean.parse();

            // Логируем первую строку для отладки
            if (!rows.isEmpty()) {
                FgislkCsvRow first = rows.get(0);
                log.debug("Первая строка после парсинга: regionCode={}, regionName={}, districtCode={}",
                        first.getRegionCode(), first.getRegionName(), first.getForestDistrictCode());
            }

            return rows;
        }
    }

    /**
     * Определение кодировки файла
     */
    private String detectEncoding(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();

        // 1. Проверка BOM
        if (bytes.length >= 3) {
            if (bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                log.debug("Обнаружен UTF-8 BOM");
                return "UTF-8";
            }
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                log.debug("Обнаружен UTF-16 LE BOM");
                return "UTF-16LE";
            }
            if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                log.debug("Обнаружен UTF-16 BE BOM");
                return "UTF-16BE";
            }
        }

        // 2. Пробуем прочитать как UTF-8
        try {
            String testString = new String(bytes, StandardCharsets.UTF_8);
            if (isValidUtf8(bytes)) {
                log.debug("Файл валидный UTF-8");
                return "UTF-8";
            }
        } catch (Exception e) {
            log.debug("Не удалось прочитать как UTF-8");
        }

        // 3. Проверяем наличие русских символов в Windows-1251
        boolean hasRussian1251 = false;
        for (byte b : bytes) {
            if (b >= (byte) 0xC0 && b <= (byte) 0xFF) {
                hasRussian1251 = true;
                break;
            }
        }

        if (hasRussian1251) {
            log.debug("Обнаружены русские символы в Windows-1251");
            return "Windows-1251";
        }

        // 4. Проверяем содержимое
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (content.contains("region_code") || content.contains("лесничество")) {
            return "UTF-8";
        }

        log.debug("Используем кодировку по умолчанию: {}", defaultEncoding);
        return defaultEncoding;
    }

    /**
     * Проверка валидности UTF-8
     */
    private boolean isValidUtf8(byte[] bytes) {
        try {
            String test = new String(bytes, StandardCharsets.UTF_8);
            byte[] roundTrip = test.getBytes(StandardCharsets.UTF_8);
            if (roundTrip.length == bytes.length) {
                for (int i = 0; i < bytes.length; i++) {
                    if (bytes[i] != roundTrip[i]) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * РУЧНОЙ ПАРСИНГ CSV (с BOM-фильтром)
     */
    private List<FgislkCsvRow> parseCsvManual(MultipartFile file, String encoding) throws Exception {
        List<FgislkCsvRow> rows = new ArrayList<>();
        Charset charset = Charset.forName(encoding);

        try (BufferedReader reader = new BufferedReader(
                new BomFilterReader(
                        new InputStreamReader(file.getInputStream(), charset)))) {

            // Читаем заголовки (пропускаем)
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return rows;
            }
            log.debug("Заголовок: {}", headerLine);

            // Читаем данные
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    String[] columns = splitCsvLine(line);
                    if (columns.length >= 17) {
                        FgislkCsvRow row = new FgislkCsvRow();
                        row.setRegionCode(cleanString(columns[0]));
                        row.setRegionName(cleanString(columns[1]));
                        row.setForestDistrictCode(cleanString(columns[2]));
                        row.setForestDistrictName(cleanString(columns[3]));
                        row.setForestQuarterCode(cleanString(columns[4]));
                        row.setForestPlotCode(cleanString(columns[5]));
                        row.setForestPlotArea(cleanString(columns[6]));
                        row.setForestPlotCharacteristic(cleanString(columns[7]));
                        row.setForestType(cleanString(columns[8]));
                        row.setDominantSpecies(cleanString(columns[9]));
                        row.setAgeClass(cleanString(columns[10]));
                        row.setForestGroup(cleanString(columns[11]));
                        row.setForestCategory(cleanString(columns[12]));
                        row.setProtectionCategory(cleanString(columns[13]));
                        row.setPurpose(cleanString(columns[14]));
                        row.setInventoryDate(parseDate(cleanString(columns[15])));
                        row.setNotes(cleanString(columns[16]));
                        rows.add(row);
                    } else {
                        log.warn("Строка {} имеет {} колонок (ожидается 17)", lineNumber, columns.length);
                    }
                } catch (Exception e) {
                    log.warn("Ошибка парсинга строки {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        return rows;
    }

    /**
     * Разбивка CSV строки с учётом кавычек
     */
    private String[] splitCsvLine(String line) {
        if (line == null || line.isEmpty()) {
            return new String[0];
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // Проверяем двойные кавычки внутри строки
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
                continue;
            }

            current.append(c);
        }

        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    /**
     * Проверка, что все поля в списке null
     */
    private boolean allFieldsAreNull(List<FgislkCsvRow> rows) {
        if (rows.isEmpty()) {
            return true;
        }
        FgislkCsvRow first = rows.get(0);
        return first.getRegionCode() == null &&
                first.getRegionName() == null &&
                first.getForestDistrictCode() == null;
    }

    /**
     * Парсинг даты
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            String cleaned = value.trim();
            if (cleaned.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(cleaned);
            }
            if (cleaned.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                return LocalDate.parse(cleaned, formatter);
            }
            if (cleaned.matches("\\d{2}/\\d{2}/\\d{4}")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return LocalDate.parse(cleaned, formatter);
            }
            return LocalDate.parse(cleaned);
        } catch (Exception e) {
            log.warn("Не удалось распарсить дату: {}", value);
            return null;
        }
    }

    /**
     * Преобразование CSV строк в Entity
     */
    private List<FgislkCommonInfo> convertToEntities(List<FgislkCsvRow> csvRows) {
        List<FgislkCommonInfo> entities = new ArrayList<>();
        int errorCount = 0;
        Long userId = securityHelper.getCurrentUserId();

        for (FgislkCsvRow row : csvRows) {
            try {
                FgislkCommonInfo entity = FgislkCommonInfo.builder()
                        .userId(userId)
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
                log.warn("Ошибка при преобразовании строки", e);
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
        if (entities.isEmpty()) {
            return new ArrayList<>();
        }

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
        if (cleaned.isEmpty()) {
            return null;
        }
        // Удаляем кавычки в начале и конце
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        cleaned = cleaned.replaceAll("^\"|\"$", "");
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

    // ===== Вспомогательные методы =====

    public boolean hasData() {
        return repository.hasData(securityHelper.getCurrentUserId());
    }

    public long getCount() {
        return repository.getCount(securityHelper.getCurrentUserId());
    }

    public List<Object[]> getRegionStatistics() {
        return repository.getRegionStatistics(securityHelper.getCurrentUserId());
    }

    public Object[] getTotalStatistics() {
        return repository.getTotalStatistics(securityHelper.getCurrentUserId());
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
}