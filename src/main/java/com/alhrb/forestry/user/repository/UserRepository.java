package com.alhrb.forestry.user.repository;

import com.alhrb.forestry.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ===== ОСНОВНЫЕ МЕТОДЫ =====
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // ===== ПОИСК ПО СТАТУСУ =====
    List<User> findByIsActiveTrue();

    List<User> findByIsActiveFalse();

    List<User> findByIsLockedTrue();

    List<User> findByIsLockedFalse();

    // ===== ПОИСК ПО РОЛИ =====
    List<User> findByRole(String role);

    // ===== ПОИСК ПО ЧАСТИ ИМЕНИ =====
    @Query("SELECT u FROM User u WHERE u.username LIKE %:query% OR u.fullName LIKE %:query% OR u.email LIKE %:query%")
    List<User> searchUsers(@Param("query") String query);

    // ===== ПОИСК ЗАБЛОКИРОВАННЫХ С ИСТЕКШЕЙ БЛОКИРОВКОЙ =====
    @Query("SELECT u FROM User u WHERE u.isLocked = true AND u.lockedUntil < CURRENT_TIMESTAMP")
    List<User> findUsersWithExpiredLock();

    // ===== КОЛИЧЕСТВО АКТИВНЫХ ПОЛЬЗОВАТЕЛЕЙ =====
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();

    // ===== КОЛИЧЕСТВО ЗАБЛОКИРОВАННЫХ =====
    @Query("SELECT COUNT(u) FROM User u WHERE u.isLocked = true")
    long countLockedUsers();
}