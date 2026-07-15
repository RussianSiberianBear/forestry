package com.alhrb.forestry.user.repository;

import com.alhrb.forestry.user.User;
import com.alhrb.forestry.user.UserUISettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserUISettingsRepository extends JpaRepository<UserUISettings, Long> {

    // ===== ОСНОВНЫЕ МЕТОДЫ =====
    Optional<UserUISettings> findByUser(User user);

    Optional<UserUISettings> findByUserId(Long userId);

    // ===== ПРОВЕРКА СУЩЕСТВОВАНИЯ =====
    boolean existsByUser(User user);

    boolean existsByUserId(Long userId);

    // ===== УДАЛЕНИЕ =====
    @Modifying
    @Transactional
    void deleteByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserUISettings s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}