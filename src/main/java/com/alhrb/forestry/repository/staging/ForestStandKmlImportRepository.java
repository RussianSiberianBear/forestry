package com.alhrb.forestry.repository.staging;

import com.alhrb.forestry.model.staging.ForestStandKmlImport;
import com.alhrb.forestry.model.staging.ImportStatus;
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
