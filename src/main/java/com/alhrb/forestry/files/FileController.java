package com.alhrb.forestry.files;

import com.alhrb.forestry.dto.abgrid.GridP;
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

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final StoredFileService storedFileService;
    private final SecurityHelper securityHelper;

    @PostMapping("/upload")
    public ResponseEntity<StoredFileDto> uploadFile(@RequestParam("file") MultipartFile file) {

        Long userId = securityHelper.getCurrentUserId();
        try {
            StoredFileDto response = storedFileService.uploadFile(userId, file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Ошибка при чтении файла", e);
            return ResponseEntity.internalServerError().build();
        }

    }

    @PostMapping
    public ResponseEntity<?> getUserFiles(GridP params) {
        return ResponseEntity.ok(storedFileService.getUserFiles(params));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        byte[] fileData = storedFileService.getFileData(fileId, securityHelper.getCurrentUserId());
        ByteArrayResource resource = new ByteArrayResource(fileData);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file_" + fileId + "\"")
                .body(resource);
    }
}
