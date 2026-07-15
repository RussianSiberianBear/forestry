package com.alhrb.forestry.files;

import com.alhrb.forestry.dto.FileUploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FileUploadService {

    FileUploadResponseDto uploadFile(Long userId, MultipartFile file) throws IOException;

    // Загрузка ZIP с распаковкой
    ZipExtractResultDto uploadAndExtractZip(Long userId, MultipartFile file) throws IOException;

    List<FileUploadResponseDto> getUserFiles(Long userId);

    byte[] getFileData(Long fileId);

    byte[] getFileData(Long fileId, Long userId);

    void deleteFile(Long fileId, Long userId);

    FileUploadResponseDto updateFileStatus(Long fileId, String status);

    List<FileUploadResponseDto> getFilesByStatus(String status);

    String getFileExtension(String filename);

    boolean isZipFile(MultipartFile file);

    boolean isZipFile(Path path);

    // Получение информации о физическом файле
    Path getPhysicalFilePath(Long fileId, Long userId);

    Path savePhysicalFile(MultipartFile file, Long fileId, Long userId) throws IOException;
}