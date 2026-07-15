package com.alhrb.forestry.repository.staging;

import com.alhrb.forestry.model.staging.ForestStandKmlImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForestStandKmlImportRepository
        extends JpaRepository<ForestStandKmlImport, Long> {

    long deleteByUploadFileId(Long uploadFileId);

    long countByUploadFileId(Long uploadFileId);
}
