package com.alhrb.forestry.files;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FileUploadService {
    FileUploadResponseDto uploadFile(Long userId, MultipartFile file) throws IOException;
    ZipExtractResultDto uploadAndExtractZip(Long userId, MultipartFile file) throws IOException;
    List<FileUploadResponseDto> getUserFiles(Long userId);
    byte[] getFileData(Long fileId);
    byte[] getFileData(Long fileId, Long userId);
    void deleteFile(Long fileId, Long userId);
    FileUploadResponseDto updateFileStatus(Long fileId, String status);
    FileUploadResponseDto markProcessed(Long fileId, Long userId);
    FileUploadResponseDto markProcessingError(Long fileId, Long userId, String errorMessage);
    List<FileUploadResponseDto> getFilesByStatus(String status);
    String getFileExtension(String filename);
    boolean isZipFile(MultipartFile file);
    boolean isZipFile(Path path);
    Path getPhysicalFilePath(Long fileId, Long userId) throws IOException;
    Path savePhysicalFile(MultipartFile file, Long fileId, Long userId) throws IOException;
}
