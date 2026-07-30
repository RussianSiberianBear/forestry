package com.alhrb.forestry.staging.repository;

import com.alhrb.forestry.staging.ImportStatus;
import com.alhrb.forestry.staging.model.ForestStandKmlImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForestStandKmlImportRepository
        extends JpaRepository<ForestStandKmlImport, Long> {

    long deleteByUploadFileId(Long uploadFileId);

    long countByUploadFileId(Long uploadFileId);

    List<ForestStandKmlImport> findByImportStatus(
            ImportStatus status);
}
