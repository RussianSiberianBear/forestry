package com.alhrb.forestry.files;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ZipExtractResultDto {
    private boolean success;
    private Path extractPath;
    private int totalFiles;
    private long totalSize;
    private List<ExtractedFileInfo> files;
    private LocalDateTime extractTime;
    private String errorMessage;

    @Data
    @Builder
    public static class ExtractedFileInfo {
        private String originalName;
        private String storedName;
        private Path path;
        private long size;
        private String extension;
    }
}