package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.FileUploadResponseDto;
import com.alhrb.forestry.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileUploadService fileUploadService;

    // TODO: Получать userId из контекста безопасности
    private static final Long TEST_USER_ID = 1L;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponseDto> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            FileUploadResponseDto response = fileUploadService.uploadFile(TEST_USER_ID, file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Ошибка при чтении файла", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<FileUploadResponseDto>> getUserFiles() {
        return ResponseEntity.ok(fileUploadService.getUserFiles(TEST_USER_ID));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        byte[] fileData = fileUploadService.getFileData(fileId, TEST_USER_ID);
        ByteArrayResource resource = new ByteArrayResource(fileData);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file_" + fileId + "\"")
                .body(resource);
    }
}
