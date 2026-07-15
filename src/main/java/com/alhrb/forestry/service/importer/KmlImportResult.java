package com.alhrb.forestry.service.importer;

public record KmlImportResult(
        Long uploadFileId,
        int totalPlacemarkCount,
        int importedCount,
        int errorCount
) {
}
