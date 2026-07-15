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
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {
    private final UploadedFileRepository repository;
    private final ZipExtractorService zipExtractorService;

    @Override
    @Transactional
    public FileUploadResponseDto uploadFile(Long userId, MultipartFile file) throws IOException {
        validate(userId, file);
        String checksum = sha256(file);
        repository.findFirstByUserIdAndSha256(userId, checksum).ifPresent(existing -> {
            throw new IllegalArgumentException("Этот файл уже загружен (ID " + existing.getId() + ", имя: " + existing.getOriginalFilename() + ")");
        });

        String originalName = safeOriginalName(file.getOriginalFilename());
        String extension = getFileExtension(originalName).toLowerCase();
        UploadedFile entity = UploadedFile.builder()
                .userId(userId)
                .originalFilename(originalName)
                .fileType(extension.toUpperCase())
                .fileSize(file.getSize())
                .sha256(checksum)
                .status("SAVING")
                .processed(false)
                .build();
        entity = repository.saveAndFlush(entity);

        Path target = null;
        try {
            target = savePhysicalFile(file, entity.getId(), userId);
            entity.setRelativePath(toRelativePath(target));
            entity.setStatus("UPLOADED");
            return mapToDto(repository.save(entity));
        } catch (Exception e) {
            if (target != null) Files.deleteIfExists(target);
            repository.delete(entity);
            if (e instanceof IOException io) throw io;
            throw e;
        }
    }

    @Override
    @Transactional
    public ZipExtractResultDto uploadAndExtractZip(Long userId, MultipartFile file) throws IOException {
        if (!isZipFile(file)) throw new IllegalArgumentException("Файл должен быть ZIP-архивом");
        FileUploadResponseDto saved = uploadFile(userId, file);
        try {
            ZipExtractResultDto result = zipExtractorService.extractZip(getPhysicalFilePath(saved.getId(), userId), userId, saved.getId());
            updateFileStatus(saved.getId(), "EXTRACTED");
            return result;
        } catch (Exception e) {
            deleteFile(saved.getId(), userId);
            if (e instanceof IOException io) throw io;
            throw e;
        }
    }

    @Override
    public Path savePhysicalFile(MultipartFile file, Long fileId, Long userId) throws IOException {
        String ext = getFileExtension(file.getOriginalFilename()).toLowerCase();
        Path dir = uploadRoot().resolve("users").resolve(String.valueOf(userId)).resolve("imports").resolve(String.valueOf(fileId));
        Files.createDirectories(dir);
        Path target = dir.resolve(ext.isBlank() || "UNKNOWN".equalsIgnoreCase(ext) ? "source" : "source." + ext);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    @Override
    public Path getPhysicalFilePath(Long fileId, Long userId) throws IOException {
        UploadedFile file = userId == null ? repository.findById(fileId).orElseThrow(() -> new IllegalArgumentException("Файл не найден"))
                : repository.findByIdAndUserId(fileId, userId).orElseThrow(() -> new IllegalArgumentException("Файл не найден или недоступен"));
        if (file.getRelativePath() == null || file.getRelativePath().isBlank()) throw new IOException("Для файла не сохранён путь на диске");
        Path path = uploadRoot().resolve(file.getRelativePath()).normalize();
        if (!path.startsWith(uploadRoot()) || !Files.isRegularFile(path)) throw new IOException("Физический файл не найден: " + path);
        return path;
    }

    @Override public List<FileUploadResponseDto> getUserFiles(Long userId) { return repository.findByUserIdOrderByUploadDateDesc(userId).stream().map(this::mapToDto).toList(); }
    @Override public byte[] getFileData(Long fileId) { try { return Files.readAllBytes(getPhysicalFilePath(fileId, null)); } catch (IOException e) { throw new IllegalStateException(e.getMessage(), e); } }
    @Override public byte[] getFileData(Long fileId, Long userId) { try { return Files.readAllBytes(getPhysicalFilePath(fileId, userId)); } catch (IOException e) { throw new IllegalStateException(e.getMessage(), e); } }

    @Override
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        UploadedFile file = repository.findByIdAndUserId(fileId, userId).orElseThrow(() -> new IllegalArgumentException("Файл не найден или недоступен"));
        Path importDir = uploadRoot().resolve("users").resolve(String.valueOf(userId)).resolve("imports").resolve(String.valueOf(fileId));
        deleteRecursively(importDir);
        repository.delete(file);
    }

    @Override @Transactional public FileUploadResponseDto updateFileStatus(Long fileId, String status) { UploadedFile f = repository.findById(fileId).orElseThrow(() -> new IllegalArgumentException("Файл не найден")); f.setStatus(status); return mapToDto(repository.save(f)); }
    @Override public List<FileUploadResponseDto> getFilesByStatus(String status) { return repository.findByStatus(status).stream().map(this::mapToDto).toList(); }
    @Override public boolean isZipFile(MultipartFile file) { return file != null && "zip".equalsIgnoreCase(getFileExtension(file.getOriginalFilename())); }
    @Override public boolean isZipFile(Path path) { return path != null && "zip".equalsIgnoreCase(getFileExtension(path.getFileName().toString())); }
    @Override public String getFileExtension(String name) { if (name == null) return ""; int i = name.lastIndexOf('.'); return i > -1 && i < name.length()-1 ? name.substring(i+1) : ""; }

    private FileUploadResponseDto mapToDto(UploadedFile f) { return new FileUploadResponseDto(f.getId(), f.getOriginalFilename(), f.getFileType(), f.getFileSize(), f.getUploadDate(), f.getStatus(), f.getProcessed(), f.getSha256(), f.getRelativePath()); }
    private Path uploadRoot() { return DirectoryConfig.getAbsoluteFileUploadPath().toAbsolutePath().normalize(); }
    private String toRelativePath(Path path) { return uploadRoot().relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/'); }
    private void validate(Long userId, MultipartFile file) { if (userId == null) throw new IllegalArgumentException("Не найден ID пользователя"); if (file == null || file.isEmpty()) throw new IllegalArgumentException("Файл не выбран или пуст"); }
    private String safeOriginalName(String name) { if (name == null || name.isBlank()) return "unnamed"; String clean = Paths.get(name).getFileName().toString().replaceAll("[\\p{Cntrl}]", "_"); return clean.length() <= 255 ? clean : clean.substring(clean.length()-255); }

    private String sha256(MultipartFile file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = file.getInputStream()) { byte[] b = new byte[8192]; int n; while ((n = in.read(b)) != -1) md.update(b, 0, n); }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 недоступен", e); }
    }

    private void deleteRecursively(Path root) {
        if (!Files.exists(root)) return;
        try (var walk = Files.walk(root)) { walk.sorted((a,b) -> b.getNameCount()-a.getNameCount()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ex) { log.warn("Не удалось удалить {}", p, ex); } }); }
        catch (IOException e) { log.warn("Не удалось очистить {}", root, e); }
    }
}
