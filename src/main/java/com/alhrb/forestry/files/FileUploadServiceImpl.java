package com.alhrb.forestry.files;

import com.alhrb.forestry.config.DirectoryConfig;
import com.alhrb.forestry.dto.FileUploadResponseDto;
import com.alhrb.forestry.files.model.staging.UploadedFile;
import com.alhrb.forestry.repository.staging.UploadedFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    private final UploadedFileRepository uploadedFileRepository;
    private final ZipExtractorService zipExtractorService;

    @Override
    @Transactional
    public FileUploadResponseDto uploadFile(Long userId, MultipartFile file) throws IOException {
        log.info("Загрузка файла для пользователя ID: {}", userId);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename).toUpperCase();

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setUserId(userId);
        uploadedFile.setOriginalFilename(originalFilename);
        uploadedFile.setFileType(fileType);
        uploadedFile.setFileData(file.getBytes());
        uploadedFile.setFileSize(file.getSize());
        uploadedFile.setStatus("UPLOADED");
        uploadedFile.setProcessed(false);

        UploadedFile savedFile = uploadedFileRepository.save(uploadedFile);
        log.info("Файл сохранен с ID: {}", savedFile.getId());

        return mapToDto(savedFile);
    }

    @Override
    @Transactional
    public ZipExtractResultDto uploadAndExtractZip(Long userId, MultipartFile file) throws IOException {
        log.info("Загрузка и распаковка ZIP для пользователя ID: {}", userId);

        // 1. Проверка что это ZIP
        if (!isZipFile(file)) {
            throw new IllegalArgumentException("Файл должен быть ZIP-архивом");
        }

        // 2. Сохраняем архив
        FileUploadResponseDto archiveInfo = uploadFile(userId, file);

        // 3. Получаем путь к физическому файлу
        Path zipPath = getPhysicalFilePath(archiveInfo.getId(), userId);

        // 4. Распаковываем
        ZipExtractResultDto extractResult = zipExtractorService.extractZip(
                zipPath,
                userId,
                archiveInfo.getId()
        );

        // 5. Обновляем статус архива
        updateFileStatus(archiveInfo.getId(), "EXTRACTED");

        log.info("ZIP распакован. Файлов: {}", extractResult.getTotalFiles());
        return extractResult;
    }

    @Override
    public Path getPhysicalFilePath(Long fileId, Long userId) throws IOException {
        Path uploadPath = DirectoryConfig.getAbsoluteFileUploadPath();

        if (userId != null) {
            Path userDir = uploadPath.resolve("user_" + userId);
            // Создаем директорию, если её нет
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir); // createDirectories создает все необходимые папки
                log.info("Created directory: {}", userDir);
            }

            // Проверяем, что директория существует и доступна для записи
            if (!Files.isDirectory(userDir)) {
                throw new IOException("Path is not a directory: " + userDir);
            }
            if (!Files.isWritable(userDir)) {
                throw new IOException("Directory is not writable: " + userDir);
            }

            try (var stream = Files.list(userDir)) {
                return stream
                        .filter(path -> path.getFileName().toString().startsWith(fileId + "_"))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Физический файл не найден"));
            } catch (IOException e) {
                throw new RuntimeException("Ошибка поиска физического файла", e);
            }
        }

        // Если userId не указан, ищем во всех папках
        try (var stream = Files.walk(uploadPath, 2)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(fileId + "_"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Физический файл не найден"));
        } catch (IOException e) {
            throw new RuntimeException("Ошибка поиска физического файла", e);
        }
    }

    @Override
    public List<FileUploadResponseDto> getUserFiles(Long userId) {
        return uploadedFileRepository.findByUserIdOrderByUploadDateDesc(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] getFileData(Long fileId) {
        return uploadedFileRepository.findById(fileId)
                .map(UploadedFile::getFileData)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));
    }

    @Override
    public byte[] getFileData(Long fileId, Long userId) {
        return uploadedFileRepository.findByIdAndUserId(fileId, userId)
                .map(UploadedFile::getFileData)
                .orElseThrow(() -> new RuntimeException("Файл не найден или недоступен"));
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        UploadedFile file = uploadedFileRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new RuntimeException("Файл не найден или недоступен"));

        uploadedFileRepository.delete(file);
        log.info("Файл удален из БД: {}", fileId);
    }

    @Override
    @Transactional
    public FileUploadResponseDto updateFileStatus(Long fileId, String status) {
        UploadedFile file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        file.setStatus(status);
        UploadedFile updated = uploadedFileRepository.save(file);

        return mapToDto(updated);
    }

    @Override
    public List<FileUploadResponseDto> getFilesByStatus(String status) {
        // Добавьте метод в репозиторий
        return uploadedFileRepository.findByStatus(status)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isZipFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) return false;
        return "zip".equalsIgnoreCase(getFileExtension(filename));
    }

    @Override
    public boolean isZipFile(Path path) {
        String filename = path.getFileName().toString();
        return filename.toLowerCase().endsWith(".zip");
    }

    @Override
    public String getFileExtension(String filename) {
        if (filename == null) return "";
        if (filename.lastIndexOf(".") == -1) {
            return "UNKNOWN";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private FileUploadResponseDto mapToDto(UploadedFile file) {
        return new FileUploadResponseDto(
                file.getId(),
                file.getOriginalFilename(),
                file.getFileType(),
                file.getFileSize(),
                file.getUploadDate(),
                file.getStatus(),
                file.getProcessed()
        );
    }

    @Override
    public Path savePhysicalFile(MultipartFile file, Long fileId, Long userId)
            throws IOException {

        Path uploadPath = DirectoryConfig.getAbsoluteFileUploadPath();
        Path userDir = uploadPath.resolve("user_" + userId);
        Files.createDirectories(userDir);

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeFilename = sanitizeFilename(file.getOriginalFilename());
        String physicalFilename = String.format("%d_%s_%s", fileId, timestamp, safeFilename);

        Path targetPath = userDir.resolve(physicalFilename);
        Files.copy(file.getInputStream(), targetPath,
                StandardCopyOption.REPLACE_EXISTING);

        log.info("Физический файл сохранен: {}", targetPath);
        return targetPath;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }
}