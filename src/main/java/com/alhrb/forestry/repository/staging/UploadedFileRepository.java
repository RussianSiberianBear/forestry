package com.alhrb.forestry.repository.staging;

import com.alhrb.forestry.files.model.staging.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    List<UploadedFile> findByUserIdOrderByUploadDateDesc(Long userId);

    List<UploadedFile> findByUserIdAndProcessedFalseOrderByUploadDateDesc(Long userId);

    Optional<UploadedFile> findByIdAndUserId(Long id, Long userId);

    List<UploadedFile> findByStatus(String status);

    List<UploadedFile> findByProcessedFalse();

    @Query("SELECT f FROM UploadedFile f WHERE f.userId = :userId AND f.processed = false")
    List<UploadedFile> findUnprocessedByUserId(@Param("userId") Long userId);
}