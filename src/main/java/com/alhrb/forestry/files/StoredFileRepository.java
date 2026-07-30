package com.alhrb.forestry.files;

import com.alhrb.forestry.files.model.staging.StoredFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    List<StoredFile> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<StoredFile> findByIdAndUserId(Long id, Long userId);

    Optional<StoredFile> findFirstByUserIdAndSha256(Long userId, String sha256);

    List<StoredFile> findByStatus(String status);

    List<StoredFile> findByProcessedFalse();

    Page<StoredFile> findAll(Specification<StoredFile> specification, Pageable pageable);
}
