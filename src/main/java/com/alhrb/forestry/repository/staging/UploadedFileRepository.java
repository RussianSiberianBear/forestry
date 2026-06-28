package com.alhrb.forestry.repository.staging;

import com.alhrb.forestry.model.staging.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    List<UploadedFile> findByUserIdOrderByUploadDateDesc(Long userId);

    List<UploadedFile> findByUserIdAndProcessedFalseOrderByUploadDateDesc(Long userId);

    Optional<UploadedFile> findByIdAndUserId(Long id, Long userId);
}