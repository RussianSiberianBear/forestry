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
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FgislkCommonInfoImportService {

    private static final char CSV_DELIMITER = ';';

    private final FgislkCommonInfoRepository repository;
    private final SecurityHelper securityHelper;

    @Value("${import.batch-size:1000}")
    private int batchSize;

    @Value("${import.encoding:UTF-8}")
    private String defaultEncoding;

    /**
     * Полностью заменяет staging-данные текущего пользователя данными из CSV.
     * <p>
     * DTO и entity содержат одинаковые 83 строковых поля. Сопоставление выполняется
     * явно, чтобы изменение структуры одного из классов обнаруживалось при компиляции.
     */
    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        long startedAt = System.currentTimeMillis();
        String fileName = file != null ? file.getOriginalFilename() : null;
        log.info("Начало импорта файла: {}", fileName);

        try {
            validateFile(file);
            validateBatchSize();

            Charset charset = detectCharset(file);
            log.info("Кодировка CSV: {}, разделитель: '{}'", charset, CSV_DELIMITER);

            List<FgislkCsvRow> rows = parseCsv(file, charset);
            validateParsedRows(rows);

            Long userId = securityHelper.getCurrentUserId();
            if (userId == null) {
                throw new IllegalStateException("Не удалось определить текущего пользователя");
            }

            List<FgislkCommonInfo> entities = convertToEntities(rows, userId);

            // Старые записи удаляются только после успешного чтения, проверки
            // и преобразования всего CSV-файла.
            repository.truncateTable(userId);
            log.info("Предыдущие данные пользователя {} удалены", userId);

            int importedCount = saveInBatches(entities);
            long processingTime = System.currentTimeMillis() - startedAt;

            log.info("Импорт завершён: {} записей за {} мс",
                    importedCount, processingTime);

            return ImportResult.success(importedCount, processingTime);
        } catch (Exception e) {
            // Метод возвращает ImportResult, а не пробрасывает исключение. Поэтому
            // транзакцию необходимо явно пометить на откат, иначе truncate/часть
            // батчей могли бы сохраниться при ошибке.
            markTransactionRollbackOnly();

            log.error("Ошибка импорта CSV-файла {}", fileName, e);
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
                    .withSkipLines(1)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .withThrowExceptions(true)
                    .build()
                    .parse();

            if (!rows.isEmpty()) {
                FgislkCsvRow first = rows.get(0);
                log.debug(
                        "Первая строка CSV: regionCode={}, regionName={}, forestDistrict={}",
                        first.getRegionCode(),
                        first.getRegionName(),
                        first.getForestDistrictAccountingNumber()
                );
            }

            return rows;
        }
    }

    /**
     * Определяет UTF-8/UTF-16 по BOM. Если BOM отсутствует, сначала строго
     * проверяет UTF-8, затем использует кодировку из import.encoding.
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

    private void validateBatchSize() {
        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "Параметр import.batch-size должен быть больше нуля");
        }
    }

    private void validateParsedRows(List<FgislkCsvRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("CSV-файл не содержит строк данных");
        }

        FgislkCsvRow first = rows.get(0);
        if (isBlank(first.getRegionCode())
                && isBlank(first.getRegionName())
                && isBlank(first.getForestDistrictAccountingNumber())) {
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
            int csvLine = i + 2;

            try {
                entities.add(convertToEntity(row, userId));
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Ошибка в строке CSV " + csvLine + ": " + rootMessage(e), e);
            }
        }

        return entities;
    }

    /**
     * Явное сопоставление всех 83 полей DTO с колонками staging-entity.
     */
    private FgislkCommonInfo convertToEntity(FgislkCsvRow row, Long userId) {
        FgislkCommonInfo entity = new FgislkCommonInfo();
        entity.setUserId(userId);
        entity.setRegionCode(cleanString(row.getRegionCode()));
        entity.setRegionName(cleanString(row.getRegionName()));
        entity.setForestDistrictAccountingNumber(cleanString(row.getForestDistrictAccountingNumber()));
        entity.setForestDistrictName(cleanString(row.getForestDistrictName()));
        entity.setLocalForestDistrictAccountingNumber(cleanString(row.getLocalForestDistrictAccountingNumber()));
        entity.setLocalForestDistrictName(cleanString(row.getLocalForestDistrictName()));
        entity.setQuarterRegistrationNumber(cleanString(row.getQuarterRegistrationNumber()));
        entity.setQuarterForestManagementNumber(cleanString(row.getQuarterForestManagementNumber()));
        entity.setTract(cleanString(row.getTract()));
        entity.setPlotId(cleanString(row.getPlotId()));
        entity.setPlotRegistrationNumber(cleanString(row.getPlotRegistrationNumber()));
        entity.setPlotForestManagementNumber(cleanString(row.getPlotForestManagementNumber()));
        entity.setPlotStatus(cleanString(row.getPlotStatus()));
        entity.setPlotArea(cleanString(row.getPlotArea()));
        entity.setTotalGrowingStock(cleanString(row.getTotalGrowingStock()));
        entity.setLandTypeCode(cleanString(row.getLandTypeCode()));
        entity.setLandTypeName(cleanString(row.getLandTypeName()));
        entity.setForestLandTypeCode(cleanString(row.getForestLandTypeCode()));
        entity.setForestLandTypeName(cleanString(row.getForestLandTypeName()));
        entity.setNonForestLandTypeCode(cleanString(row.getNonForestLandTypeCode()));
        entity.setNonForestLandTypeName(cleanString(row.getNonForestLandTypeName()));
        entity.setForestTypeCode(cleanString(row.getForestTypeCode()));
        entity.setForestTypeName(cleanString(row.getForestTypeName()));
        entity.setForestSiteConditionsTypeName(cleanString(row.getForestSiteConditionsTypeName()));
        entity.setDeadwoodStock(cleanString(row.getDeadwoodStock()));
        entity.setNaturalOpenForestStock(cleanString(row.getNaturalOpenForestStock()));
        entity.setSingleTreesStock(cleanString(row.getSingleTreesStock()));
        entity.setNonCommercialWoodStock(cleanString(row.getNonCommercialWoodStock()));
        entity.setForestPlantationCreationYear(cleanString(row.getForestPlantationCreationYear()));
        entity.setForestStandConditionName(cleanString(row.getForestStandConditionName()));
        entity.setTargetSpeciesCode(cleanString(row.getTargetSpeciesCode()));
        entity.setTargetSpeciesNsi(cleanString(row.getTargetSpeciesNsi()));
        entity.setTargetSpeciesName(cleanString(row.getTargetSpeciesName()));
        entity.setRadionuclidePollutionName(cleanString(row.getRadionuclidePollutionName()));
        entity.setLastForestInventoryDate(cleanString(row.getLastForestInventoryDate()));
        entity.setForestSeedProductionObjectName(cleanString(row.getForestSeedProductionObjectName()));
        entity.setSpecialProtectiveAreas(cleanString(row.getSpecialProtectiveAreas()));
        entity.setEconomicCategory(cleanString(row.getEconomicCategory()));
        entity.setProtectionCategory(cleanString(row.getProtectionCategory()));
        entity.setProtectiveForestCategoryCode(cleanString(row.getProtectiveForestCategoryCode()));
        entity.setProtectiveForestCategoryName(cleanString(row.getProtectiveForestCategoryName()));
        entity.setProtectiveForestSubcategoryCode(cleanString(row.getProtectiveForestSubcategoryCode()));
        entity.setProtectiveForestSubcategoryName(cleanString(row.getProtectiveForestSubcategoryName()));
        entity.setAdministrativeDistrictMnemonic(cleanString(row.getAdministrativeDistrictMnemonic()));
        entity.setAdministrativeDistrictName(cleanString(row.getAdministrativeDistrictName()));
        entity.setPlotFeatures(cleanString(row.getPlotFeatures()));
        entity.setTappingInformation(cleanString(row.getTappingInformation()));
        entity.setRecreationalCharacteristic(cleanString(row.getRecreationalCharacteristic()));
        entity.setSelectionAssessment(cleanString(row.getSelectionAssessment()));
        entity.setClutterStockPerHectare(cleanString(row.getClutterStockPerHectare()));
        entity.setMerchantableStock(cleanString(row.getMerchantableStock()));
        entity.setDominantSpeciesCode(cleanString(row.getDominantSpeciesCode()));
        entity.setDominantSpeciesName(cleanString(row.getDominantSpeciesName()));
        entity.setBonitetClassCode(cleanString(row.getBonitetClassCode()));
        entity.setBonitetClassName(cleanString(row.getBonitetClassName()));
        entity.setEconomicSection(cleanString(row.getEconomicSection()));
        entity.setLoggingAge(cleanString(row.getLoggingAge()));
        entity.setCuttingAgeCode(cleanString(row.getCuttingAgeCode()));
        entity.setCuttingAgeName(cleanString(row.getCuttingAgeName()));
        entity.setForestInventoryArea(cleanString(row.getForestInventoryArea()));
        entity.setPopdArea(cleanString(row.getPopdArea()));
        entity.setErosionType(cleanString(row.getErosionType()));
        entity.setErosionDegree(cleanString(row.getErosionDegree()));
        entity.setSlopeExposureCode(cleanString(row.getSlopeExposureCode()));
        entity.setSlopeExposureName(cleanString(row.getSlopeExposureName()));
        entity.setSlopeSteepnessCode(cleanString(row.getSlopeSteepnessCode()));
        entity.setSlopeSteepnessName(cleanString(row.getSlopeSteepnessName()));
        entity.setReliefTypeName(cleanString(row.getReliefTypeName()));
        entity.setElevationAboveSeaLevel(cleanString(row.getElevationAboveSeaLevel()));
        entity.setStumpsPerHectare(cleanString(row.getStumpsPerHectare()));
        entity.setAverageStumpDiameter(cleanString(row.getAverageStumpDiameter()));
        entity.setCuttingGroupCategoryNsi(cleanString(row.getCuttingGroupCategoryNsi()));
        entity.setCuttingTypeName(cleanString(row.getCuttingTypeName()));
        entity.setCuttingYear(cleanString(row.getCuttingYear()));
        entity.setPineStumpsCount(cleanString(row.getPineStumpsCount()));
        entity.setVegetationTypeCode(cleanString(row.getVegetationTypeCode()));
        entity.setVegetationTypeName(cleanString(row.getVegetationTypeName()));
        entity.setBogTypeCode(cleanString(row.getBogTypeCode()));
        entity.setBogTypeName(cleanString(row.getBogTypeName()));
        entity.setSpeciesNsiCode(cleanString(row.getSpeciesNsiCode()));
        entity.setSpeciesNsiName(cleanString(row.getSpeciesNsiName()));
        entity.setOvergrowthPercentage(cleanString(row.getOvergrowthPercentage()));
        entity.setPeatLayerThickness(cleanString(row.getPeatLayerThickness()));
        return entity;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void markTransactionRollbackOnly() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (Exception ignored) {
            log.warn("Не удалось пометить транзакцию импорта на откат");
        }
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

    /**
     * Удаляет только первый символ BOM после декодирования потока.
     */
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
