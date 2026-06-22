package com.alhrb.forestry.repository;

import com.alhrb.forestry.model.UserUISettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserUISettingsRepository extends JpaRepository<UserUISettings, Long> {
    Optional<UserUISettings> findByUserId(String userId);
}