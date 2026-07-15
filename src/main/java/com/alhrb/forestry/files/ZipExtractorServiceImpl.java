package com.alhrb.forestry.files;

import com.alhrb.forestry.config.DirectoryConfig;
import com.alhrb.forestry.files.model.staging.UploadedFile;
import com.alhrb.forestry.repository.staging.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZipExtractorServiceImpl implements ZipExtractorService {

    private final UploadedFileRepository uploadedFileRepository;
    private final DirectoryConfig directoryConfig;

    private static final Set<String> ALLOWED_ZIP_EXTENSIONS = Set.of(
            "xlsx", "xls", "csv", "xml", "pdf", "doc", "docx", "txt", "json", "jpg", "png"
    );
    private static final int MAX_EXTRACTED_FILES = 100;
    private static final long MAX_TOTAL_SIZE = 500 * 1024 * 1024; // 500MB

    @Override
    @Transactional
    public ZipExtractResultDto extractZip(Path zipPath, Long userId, Long archiveFileId)
            throws IOException {

        log.info("Начало распаковки ZIP: {}", zipPath);

        // 1. Валидация
        validateZipArchive(zipPath);

        // 2. Создаем папку для распакованных файлов
        Path extractDir = createExtractDirectory(userId, archiveFileId);

        // 3. Распаковываем
        List<ZipExtractResultDto.ExtractedFileInfo> extractedFiles = new ArrayList<>();
        long totalSize = 0;
        int fileCount = 0;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                // Проверяем количество файлов
                if (fileCount >= MAX_EXTRACTED_FILES) {
                    throw new IOException(
                            "Превышено максимальное количество файлов в архиве: " +
                                    MAX_EXTRACTED_FILES
                    );
                }

                String entryName = entry.getName();

                // Пропускаем директории
                if (entry.isDirectory()) {
                    continue;
                }

                // Проверяем расширение
                String extension = getFileExtension(entryName);
                if (!ALLOWED_ZIP_EXTENSIONS.contains(extension.toLowerCase())) {
                    log.warn("Пропущен файл с недопустимым расширением: {}", entryName);
                    continue;
                }

                // Проверяем размер
                if (entry.getSize() > 0) {
                    totalSize += entry.getSize();
                    if (totalSize > MAX_TOTAL_SIZE) {
                        throw new IOException(
                                "Превышен максимальный общий размер распакованных файлов: " +
                                        MAX_TOTAL_SIZE / (1024 * 1024) + "MB"
                        );
                    }
                }

                // Создаем безопасное имя файла
                String safeName = sanitizeFilename(entryName);
                Path targetPath = extractDir.resolve(safeName);

                // Убеждаемся, что файл не выходит за пределы папки распаковки
                if (!targetPath.normalize().startsWith(extractDir.normalize())) {
                    log.warn("Попытка обхода директории: {}", entryName);
                    continue;
                }

                // Создаем вложенные папки если нужно
                Files.createDirectories(targetPath.getParent());

                // Сохраняем файл
                try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                }

                // Сохраняем информацию о файле
                ZipExtractResultDto.ExtractedFileInfo fileInfo =
                        ZipExtractResultDto.ExtractedFileInfo.builder()
                                .originalName(entryName)
                                .storedName(safeName)
                                .path(targetPath)
                                .size(entry.getSize())
                                .extension(extension)
                                .build();

                extractedFiles.add(fileInfo);
                fileCount++;

                log.debug("Распакован файл: {} -> {}", entryName, targetPath);

                // Сохраняем информацию в БД (опционально)
                saveExtractedFileInfo(userId, archiveFileId, targetPath, entryName, entry.getSize());
            }
        }

        // 4. Формируем результат
        ZipExtractResultDto result = ZipExtractResultDto.builder()
                .success(true)
                .extractPath(extractDir)
                .totalFiles(extractedFiles.size())
                .totalSize(totalSize)
                .files(extractedFiles)
                .extractTime(LocalDateTime.now())
                .build();

        log.info("Распаковка завершена. Файлов: {}, Размер: {} bytes",
                extractedFiles.size(), totalSize);

        return result;
    }

    @Override
    public ZipExtractResultDto extractZip(MultipartFile zipFile, Long userId)
            throws IOException {

        // Сохраняем временный файл
        Path tempZip = Files.createTempFile("temp_zip_", ".zip");
        try {
            Files.copy(zipFile.getInputStream(), tempZip,
                    StandardCopyOption.REPLACE_EXISTING);
            return extractZip(tempZip, userId, null);
        } finally {
            Files.deleteIfExists(tempZip);
        }
    }

    @Override
    public void validateZipArchive(Path zipPath) throws IOException {
        if (!Files.exists(zipPath)) {
            throw new IOException("ZIP-архив не найден: " + zipPath);
        }

        if (!zipPath.toString().toLowerCase().endsWith(".zip")) {
            throw new IOException("Файл не является ZIP-архивом");
        }

        // Проверяем, что файл действительно ZIP
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            if (zipFile.size() == 0) {
                throw new IOException("ZIP-архив пуст");
            }

            // Проверяем, что внутри нет zip-бомб
            long totalSize = 0;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    totalSize += entry.getSize();
                    if (totalSize > MAX_TOTAL_SIZE) {
                        throw new IOException(
                                "ZIP-архив содержит слишком много данных"
                        );
                    }
                }
            }

            log.info("ZIP-архив прошел валидацию. Файлов: {}", zipFile.size());

        } catch (Exception e) {
            throw new IOException("Неверный ZIP-архив: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getZipContents(Path zipPath) throws IOException {
        List<String> contents = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                contents.add(entry.getName());
            }
        }

        return contents;
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    private Path createExtractDirectory(Long userId, Long archiveFileId) throws IOException {
        Path uploadPath = DirectoryConfig.getAbsoluteFileUploadPath();

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String dirName = String.format("extracted_user_%d_archive_%d_%s",
                userId,
                archiveFileId != null ? archiveFileId : 0,
                timestamp
        );

        Path extractDir = uploadPath.resolve(dirName);
        Files.createDirectories(extractDir);

        log.info("Создана директория для распаковки: {}", extractDir);
        return extractDir;
    }

    private void saveExtractedFileInfo(Long userId, Long archiveFileId,
                                       Path filePath, String originalName, long size) {
        try {
            UploadedFile uploadedFile = new UploadedFile();
            uploadedFile.setUserId(userId);
            uploadedFile.setOriginalFilename(originalName);
            uploadedFile.setFileType(getFileExtension(originalName));
            uploadedFile.setFileData(Files.readAllBytes(filePath));
            uploadedFile.setFileSize(size);
            uploadedFile.setStatus("EXTRACTED");
            uploadedFile.setProcessed(false);
            uploadedFile.setArchiveId(archiveFileId); // нужно добавить поле в модель

            uploadedFileRepository.save(uploadedFile);
            log.debug("Информация о распакованном файле сохранена в БД: {}", originalName);

        } catch (Exception e) {
            log.error("Ошибка сохранения информации о распакованном файле", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf(".");
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        // Удаляем опасные символы и пути
        String name = filename.replaceAll("[^a-zA-Z0-9.\\-]", "_");
        // Убираем возможные обходы директорий
        name = name.replace("../", "").replace("..\\", "");
        return name;
    }
}