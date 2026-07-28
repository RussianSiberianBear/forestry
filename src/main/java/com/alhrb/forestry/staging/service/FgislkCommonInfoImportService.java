package com.alhrb.forestry.staging.service;

import com.alhrb.forestry.staging.dto.FgislkCsvRow;
import com.alhrb.forestry.staging.model.FgislkCommonInfo;
import com.alhrb.forestry.staging.repository.FgislkCommonInfoRepository;
import com.alhrb.forestry.util.SecurityHelper;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FgislkCommonInfoImportService {

    private static final char CSV_DELIMITER = ';';

    private static final DateTimeFormatter DATE_DMY_DOT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final DateTimeFormatter DATE_DMY_SLASH =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FgislkCommonInfoRepository repository;
    private final SecurityHelper securityHelper;

    @Value("${import.batch-size:1000}")
    private int batchSize;

    @Value("${import.encoding:UTF-8}")
    private String defaultEncoding;

    /**
     * Импорт CSV, содержащего кириллические заголовки и данные.
     * DTO привязан к колонкам по позиции, поэтому текст заголовков не влияет
     * на сопоставление полей.
     */
    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        long startedAt = System.currentTimeMillis();
        log.info("Начало импорта файла: {}", file.getOriginalFilename());

        try {
            validateFile(file);

            Charset charset = detectCharset(file);
            log.info("Кодировка CSV: {}, разделитель: '{}'", charset, CSV_DELIMITER);

            List<FgislkCsvRow> rows = parseCsv(file, charset);
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("CSV-файл не содержит строк данных");
            }

            validateParsedRows(rows);

            Long userId = securityHelper.getCurrentUserId();
            List<FgislkCommonInfo> entities = convertToEntities(rows, userId);

            // Очищаем прежние данные только после успешного чтения и проверки CSV.
            repository.truncateTable(userId);
            log.info("Предыдущие данные пользователя {} удалены", userId);

            int importedCount = saveInBatches(entities);
            long processingTime = System.currentTimeMillis() - startedAt;

            log.info("Импорт завершён: {} записей за {} мс",
                    importedCount, processingTime);

            return ImportResult.success(importedCount, processingTime);
        } catch (Exception e) {
            log.error("Ошибка импорта CSV-файла {}",
                    file != null ? file.getOriginalFilename() : null, e);
            return ImportResult.failure(rootMessage(e));
        }
    }

    private List<FgislkCsvRow> parseCsv(MultipartFile file, Charset charset)
            throws IOException {

        try (Reader reader = new BomFilterReader(
                new BufferedReader(
                        new InputStreamReader(file.getInputStream(), charset)))) {

            List<FgislkCsvRow> rows = new CsvToBeanBuilder<FgislkCsvRow>(reader)
                    .withType(FgislkCsvRow.class)
                    .withSeparator(CSV_DELIMITER)
                    .withSkipLines(1) // Пропускаем строку кириллических заголовков.
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .withThrowExceptions(true)
                    .build()
                    .parse();

            if (!rows.isEmpty()) {
                FgislkCsvRow first = rows.get(0);
                log.debug(
                        "Первая строка CSV: regionCode={}, regionName={}, districtCode={}",
                        first.getRegionCode(),
                        first.getRegionName(),
                        first.getLocalForestDistrictName()
                );
            }

            return rows;
        }
    }

    /**
     * Определяет UTF-8/UTF-16 по BOM. Без BOM сначала строго проверяет UTF-8,
     * затем использует кодировку из import.encoding.
     */
    private Charset detectCharset(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();

        if (startsWith(bytes, 0xEF, 0xBB, 0xBF)) {
            return StandardCharsets.UTF_8;
        }
        if (startsWith(bytes, 0xFF, 0xFE)) {
            return StandardCharsets.UTF_16LE;
        }
        if (startsWith(bytes, 0xFE, 0xFF)) {
            return StandardCharsets.UTF_16BE;
        }
        if (isValidUtf8(bytes)) {
            return StandardCharsets.UTF_8;
        }

        try {
            return Charset.forName(defaultEncoding);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Неизвестная кодировка import.encoding: " + defaultEncoding, e);
        }
    }

    private boolean isValidUtf8(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((bytes[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV-файл не выбран или пуст");
        }
    }

    private void validateParsedRows(List<FgislkCsvRow> rows) {
        FgislkCsvRow first = rows.get(0);
        if (isBlank(first.getRegionCode())
                && isBlank(first.getRegionName())
                && isBlank(first.getForestDistrictCode())) {
            throw new IllegalArgumentException(
                    "CSV распознан неверно: первые столбцы строки данных пусты. " +
                    "Проверьте разделитель ';' и порядок столбцов"
            );
        }
    }

    private List<FgislkCommonInfo> convertToEntities(
            List<FgislkCsvRow> rows,
            Long userId
    ) {
        List<FgislkCommonInfo> entities = new ArrayList<>(rows.size());

        for (int i = 0; i < rows.size(); i++) {
            FgislkCsvRow row = rows.get(i);
            int csvLine = i + 2; // Первая строка — заголовок.

            try {
                entities.add(FgislkCommonInfo.builder()
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
                        // В выгрузке нет отдельного столбца «Класс возраста».
                        // Сохраняем наиболее близкое по смыслу значение — возраст рубки.
                        .ageClass(cleanString(row.getCuttingAgeName()))
                        .forestGroup(cleanString(row.getForestGroup()))
                        .forestCategory(firstNotBlank(
                                row.getProtectedForestCategoryName(),
                                row.getForestCategory()))
                        .protectionCategory(cleanString(row.getProtectionCategory()))
                        .purpose(firstNotBlank(
                                row.getForestLandTypeName(),
                                row.getLandTypeName()))
                        .inventoryDate(parseDate(row.getInventoryDate()))
                        .notes(buildNotes(row))
                        .build());
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Ошибка в строке CSV " + csvLine + ": " + rootMessage(e), e);
            }
        }

        return entities;
    }

    private int saveInBatches(List<FgislkCommonInfo> entities) {
        int savedCount = 0;

        for (int from = 0; from < entities.size(); from += batchSize) {
            int to = Math.min(from + batchSize, entities.size());
            List<FgislkCommonInfo> batch = entities.subList(from, to);
            repository.saveAll(batch);
            savedCount += batch.size();
            log.debug("Сохранён батч {}-{} из {}", from + 1, to, entities.size());
        }

        return savedCount;
    }

    private String cleanString(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.strip();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private BigDecimal parseArea(String value) {
        String cleaned = cleanString(value);
        if (cleaned == null) {
            return null;
        }

        // Убираем обычные и неразрывные пробелы-разделители тысяч.
        cleaned = cleaned
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace(',', '.');

        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректная площадь: " + value, e);
        }
    }

    private LocalDate parseDate(String value) {
        String cleaned = cleanString(value);
        if (cleaned == null) {
            return null;
        }

        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_LOCAL_DATE,
                DATE_DMY_DOT,
                DATE_DMY_SLASH
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {
                // Пробуем следующий формат.
            }
        }

        throw new IllegalArgumentException("Некорректная дата: " + value);
    }


    private String firstNotBlank(String first, String second) {
        String cleanedFirst = cleanString(first);
        return cleanedFirst != null ? cleanedFirst : cleanString(second);
    }

    private String buildNotes(FgislkCsvRow row) {
        List<String> parts = new ArrayList<>();
        addNote(parts, "Участковое лесничество", row.getLocalForestDistrictName());
        addNote(parts, "Учетный номер участкового лесничества", row.getLocalForestDistrictCode());
        addNote(parts, "Лесоустроительный номер квартала", row.getForestQuarterNumber());
        addNote(parts, "Лесоустроительный номер выдела", row.getForestPlotNumber());
        addNote(parts, "Статус выдела", row.getForestPlotStatus());
        addNote(parts, "Урочище", row.getTract());
        addNote(parts, "Класс бонитета", row.getBonitetClass());
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private void addNote(List<String> parts, String title, String value) {
        String cleaned = cleanString(value);
        if (cleaned != null) {
            parts.add(title + ": " + cleaned);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null
                ? current.getMessage()
                : throwable.getClass().getSimpleName();
    }

    /** Удаляет только первый символ BOM после декодирования потока. */
    private static final class BomFilterReader extends FilterReader {

        private static final int BOM = '\uFEFF';
        private boolean firstCharacter = true;

        private BomFilterReader(Reader reader) {
            super(reader);
        }

        @Override
        public int read() throws IOException {
            int character = super.read();
            if (firstCharacter) {
                firstCharacter = false;
                if (character == BOM) {
                    return super.read();
                }
            }
            return character;
        }

        @Override
        public int read(char[] buffer, int offset, int length) throws IOException {
            if (length == 0) {
                return 0;
            }

            int first = read();
            if (first == -1) {
                return -1;
            }

            buffer[offset] = (char) first;
            int count = 1;

            while (count < length) {
                int read = super.read(buffer, offset + count, length - count);
                if (read == -1) {
                    break;
                }
                count += read;
            }

            return count;
        }
    }

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

    public static class ImportResult {
        private final boolean success;
        private final int importedCount;
        private final long processingTimeMs;
        private final String errorMessage;

        private ImportResult(
                boolean success,
                int importedCount,
                long processingTimeMs,
                String errorMessage
        ) {
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

        public boolean isSuccess() {
            return success;
        }

        public int getImportedCount() {
            return importedCount;
        }

        public long getProcessingTimeMs() {
            return processingTimeMs;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
