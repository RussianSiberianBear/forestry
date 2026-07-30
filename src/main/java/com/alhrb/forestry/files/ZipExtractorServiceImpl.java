package com.alhrb.forestry.files;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZipExtractorServiceImpl implements ZipExtractorService {
    private static final Set<String> ALLOWED = Set.of("kml");
    private static final int MAX_FILES = 1000;
    private static final long MAX_TOTAL_SIZE = 500L * 1024 * 1024;
    private static final long MAX_SINGLE_FILE_SIZE = 100L * 1024 * 1024;

    @Override
    public ZipExtractResultDto extractZip(Path zipPath, Long userId, Long archiveFileId) throws IOException {
        validateZipArchive(zipPath);
        Path dir = zipPath.getParent().resolve("extracted").normalize();
        deleteRecursively(dir);
        Files.createDirectories(dir);
        try {
            ZipExtractResultDto result;
            try {
                result = extract(zipPath, dir, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException malformedUtf8) {
                deleteRecursively(dir);
                Files.createDirectories(dir);
                result = extract(zipPath, dir, Charset.forName("CP866"));
            }
            if (result.getTotalFiles() == 0) throw new IOException("В ZIP-архиве не найдено ни одного KML-файла");
            return result;
        } catch (Exception e) {
            deleteRecursively(dir);
            if (e instanceof IOException io) throw io;
            throw new IOException("Не удалось распаковать ZIP-архив: " + e.getMessage(), e);
        }
    }

    @Override
    public ZipExtractResultDto extractZip(MultipartFile file, Long userId) throws IOException {
        Path temp = Files.createTempFile("forest-stand-", ".zip");
        try {
            file.transferTo(temp);
            return extractZip(temp, userId, null);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @Override
    public void validateZipArchive(Path zipPath) throws IOException {
        if (zipPath == null || !Files.isRegularFile(zipPath)) throw new IOException("ZIP-архив не найден");
        try (InputStream in = Files.newInputStream(zipPath)) {
            byte[] sig = in.readNBytes(4);
            boolean zip = sig.length == 4 && sig[0] == 'P' && sig[1] == 'K' && ((sig[2] == 3 && sig[3] == 4) || (sig[2] == 5 && sig[3] == 6) || (sig[2] == 7 && sig[3] == 8));
            if (!zip) throw new IOException("Файл не является ZIP-архивом");
        }
    }

    @Override
    public List<String> getZipContents(Path zipPath) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath), StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                names.add(e.getName());
                zis.closeEntry();
            }
        }
        return names;
    }

    private ZipExtractResultDto extract(Path zip, Path root, Charset charset) throws IOException {
        List<ZipExtractResultDto.ExtractedFileInfo> files = new ArrayList<>();
        long total = 0;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip), charset)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                if (files.size() >= MAX_FILES) throw new IOException("В архиве больше " + MAX_FILES + " файлов");
                String entryName = entry.getName().replace('\\', '/');
                String ext = extension(entryName).toLowerCase();
                if (!ALLOWED.contains(ext)) {
                    zis.closeEntry();
                    continue;
                }

                Path relative = safeRelativePath(entryName);
                Path target = root.resolve(relative).normalize();
                if (!target.startsWith(root)) throw new IOException("Опасный путь в ZIP: " + entryName);
                Files.createDirectories(target.getParent());

                long written = copyEntry(zis, target, total);
                total += written;
                files.add(ZipExtractResultDto.ExtractedFileInfo.builder()
                        .originalName(entryName).storedName(relative.toString().replace('\\', '/')).path(target)
                        .size(written).extension(ext).build());
                zis.closeEntry();
            }
        }
        return ZipExtractResultDto.builder().success(true).extractPath(root).totalFiles(files.size()).totalSize(total).files(files).extractTime(LocalDateTime.now()).build();
    }

    private long copyEntry(ZipInputStream zis, Path target, long alreadyTotal) throws IOException {
        long written = 0;
        try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = zis.read(buf)) != -1) {
                written += n;
                if (written > MAX_SINGLE_FILE_SIZE) throw new IOException("KML-файл в архиве превышает 100 МБ");
                if (alreadyTotal + written > MAX_TOTAL_SIZE)
                    throw new IOException("Распакованный архив превышает 500 МБ");
                out.write(buf, 0, n);
            }
        } catch (Exception e) {
            Files.deleteIfExists(target);
            throw e;
        }
        return written;
    }

    private Path safeRelativePath(String entryName) throws IOException {
        Path raw = Paths.get(entryName).normalize();
        if (raw.isAbsolute() || raw.startsWith("..")) throw new IOException("Опасный путь в ZIP: " + entryName);
        Path result = Paths.get("");
        for (Path part : raw) {
            String s = part.toString().replaceAll("[<>:\"|?*\\p{Cntrl}]", "_");
            if (s.isBlank() || s.equals(".") || s.equals(".."))
                throw new IOException("Некорректное имя в ZIP: " + entryName);
            result = result.resolve(s);
        }
        return result;
    }

    private String extension(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 && i < name.length() - 1 ? name.substring(i + 1) : "";
    }

    private void deleteRecursively(Path root) {
        if (!Files.exists(root)) return;
        try (var w = Files.walk(root)) {
            w.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
