package com.alhrb.forestry.controller;

import com.alhrb.forestry.files.StoredFileDto;
import com.alhrb.forestry.files.FileUploadService;
import com.alhrb.forestry.util.SecurityHelper;
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
    private final SecurityHelper securityHelper;

    @PostMapping("/upload")
    public ResponseEntity<StoredFileDto> uploadFile(@RequestParam("file") MultipartFile file) {

        Long userId = securityHelper.getCurrentUserId();
        try {
            StoredFileDto response = fileUploadService.uploadFile(userId, file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Ошибка при чтении файла", e);
            return ResponseEntity.internalServerError().build();
        }

    }

    @PostMapping
    public ResponseEntity<List<StoredFileDto>> getUserFiles() {
        return ResponseEntity.ok(fileUploadService.getUserFiles(securityHelper.getCurrentUserId()));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        byte[] fileData = fileUploadService.getFileData(fileId, securityHelper.getCurrentUserId());
        ByteArrayResource resource = new ByteArrayResource(fileData);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file_" + fileId + "\"")
                .body(resource);
    }
}
