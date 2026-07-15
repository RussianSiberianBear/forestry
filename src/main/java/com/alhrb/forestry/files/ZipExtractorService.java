package com.alhrb.forestry.files;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ZipExtractorService {

    ZipExtractResultDto extractZip(Path zipPath, Long userId, Long archiveFileId)
            throws IOException;

    ZipExtractResultDto extractZip(MultipartFile zipFile, Long userId)
            throws IOException;

    void validateZipArchive(Path zipPath) throws IOException;

    List<String> getZipContents(Path zipPath) throws IOException;
}