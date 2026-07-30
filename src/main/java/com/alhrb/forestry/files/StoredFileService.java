package com.alhrb.forestry.files;

import com.alhrb.forestry.dto.abgrid.GridP;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface StoredFileService {
    StoredFileDto uploadFile(Long userId, MultipartFile file) throws IOException;

    ZipExtractResultDto uploadAndExtractZip(Long userId, MultipartFile file) throws IOException;

    Map<String, Object> getUserFiles(GridP p);

    byte[] getFileData(Long fileId);

    byte[] getFileData(Long fileId, Long userId);

    void deleteFile(Long fileId, Long userId);

    StoredFileDto updateFileStatus(Long fileId, String status);

    StoredFileDto markProcessed(Long fileId, Long userId);

    StoredFileDto markProcessingError(Long fileId, Long userId, String errorMessage);

    List<StoredFileDto> getFilesByStatus(String status);

    String getFileExtension(String filename);

    boolean isZipFile(MultipartFile file);

    boolean isZipFile(Path path);

    Path getPhysicalFilePath(Long fileId, Long userId) throws IOException;

    Path savePhysicalFile(MultipartFile file, Long fileId, Long userId) throws IOException;
}
