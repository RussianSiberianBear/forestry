package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.UserUISettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserUISettingsRepository extends JpaRepository<UserUISettings, Long> {

    // ===== ПОИСК ПО USER_ID =====
    Optional<UserUISettings> findByUserId(Long userId);

    // ===== УДАЛЕНИЕ ПО USER_ID =====
    void deleteByUserId(Long userId);
}