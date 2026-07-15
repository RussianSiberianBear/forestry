package com.alhrb.forestry.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class DirectoryConfig {

    @Getter
    private static String docsPath;

    @Getter
    private static String reportTemplatesPath;

    @Getter
    private static String reportOutputPath;

    @Getter
    private static String fileUploadPath;

    @Getter
    private static Path absoluteDocsPath;

    @Getter
    private static Path absoluteTemplatesPath;

    @Getter
    private static Path absoluteOutputPath;

    @Getter
    private static Path absoluteFileUploadPath;

    @Value("${app.storage.docs-path}")
    public void setDocsPath(String path) {
        DirectoryConfig.docsPath = path;
        DirectoryConfig.absoluteDocsPath = normalizePath(path);
    }

    @Value("${app.storage.report-templates-path}")
    public void setReportTemplatesPath(String path) {
        DirectoryConfig.reportTemplatesPath = path;
        DirectoryConfig.absoluteTemplatesPath = normalizePath(path);
    }

    @Value("${app.storage.report-out-path}")
    public void setReportOutputPath(String path) {
        DirectoryConfig.reportOutputPath = path;
        DirectoryConfig.absoluteOutputPath = normalizePath(path);
    }

    @Value("${app.storage.file-upload-path:${user.home}/forestry/upload}")
    public void setFileUploadPath(String path) {
        DirectoryConfig.fileUploadPath = path;
        DirectoryConfig.absoluteFileUploadPath = normalizePath(path);
    }

    @PostConstruct
    public void init() throws IOException {
        if (absoluteDocsPath != null) {
            Files.createDirectories(absoluteDocsPath);
        }
        if (absoluteTemplatesPath != null) {
            Files.createDirectories(absoluteTemplatesPath);
        }
        if (absoluteOutputPath != null) {
            Files.createDirectories(absoluteOutputPath);
        }
        if (absoluteFileUploadPath != null) {
            Files.createDirectories(absoluteFileUploadPath);
        }
    }

    private static Path normalizePath(String path) {
        String normalized = path.replace('\\', '/');

        if (isAbsolutePath(normalized)) {
            return Paths.get(normalized).normalize();
        }

        return Paths.get(System.getProperty("user.dir"), normalized).normalize();
    }

    private static boolean isAbsolutePath(String path) {
        if (path.startsWith("/")) {
            return true;
        }
        return path.matches("^[A-Za-z]:[/\\\\].*");
    }
}
