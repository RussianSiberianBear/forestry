package com.alhrb.forestry.service;

import com.alhrb.forestry.dto.FileUploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileUploadService {
    FileUploadResponseDto uploadFile(Long userId, MultipartFile file) throws IOException;
    List<FileUploadResponseDto> getUserFiles(Long userId);
    byte[] getFileData(Long fileId);
    byte[] getFileData(Long fileId, Long userId);
}