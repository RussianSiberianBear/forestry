package com.alhrb.forestry.service.impl;

import com.alhrb.forestry.dto.FileUploadResponseDto;
import com.alhrb.forestry.model.staging.UploadedFile;
import com.alhrb.forestry.repository.staging.UploadedFileRepository;
import com.alhrb.forestry.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    private final UploadedFileRepository uploadedFileRepository;

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

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
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
}