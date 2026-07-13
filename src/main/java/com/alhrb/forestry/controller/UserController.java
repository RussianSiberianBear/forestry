package com.alhrb.forestry.controller;

import com.alhrb.forestry.dto.UserCreateDto;
import com.alhrb.forestry.dto.UserLockDto;
import com.alhrb.forestry.user.User;
import com.alhrb.forestry.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    // ===== РЕГИСТРАЦИЯ =====
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserCreateDto dto) {
        try {
            User user = userService.register(
                    dto.getUsername(),
                    dto.getEmail(),
                    dto.getPassword(),
                    dto.getFullName(),
                    dto.getPhone()
            );
            log.info("✅ Зарегистрирован пользователь: {}", user.getUsername());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("❌ Ошибка регистрации: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===== МЕТОД LOGIN УДАЛЕН! Spring Security сам его обрабатывает =====

    // ===== ВЫХОД (Spring Security сам обрабатывает /logout) =====
    // Этот метод тоже не нужен, но можно оставить для API-выхода
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Выход выполнен");
    }

    // ===== ТЕКУЩИЙ ПОЛЬЗОВАТЕЛЬ =====
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.ok(null);
        }
        return userService.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ===== ВСЕ ПОЛЬЗОВАТЕЛИ =====
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    // ===== БЛОКИРОВКА =====
    @PostMapping("/lock")
    public ResponseEntity<?> lockUser(@Valid @RequestBody UserLockDto dto, HttpSession session) {
        try {
            Long currentUserId = (Long) session.getAttribute("userId");
            if (currentUserId == null) {
                return ResponseEntity.badRequest().body("Пользователь не авторизован");
            }

            User user = userService.lockUser(
                    dto.getUserId(),
                    dto.getReason(),
                    dto.getMinutes(),
                    currentUserId
            );
            log.info("🔒 Заблокирован пользователь: {} администратором ID: {}", user.getUsername(), currentUserId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("❌ Ошибка блокировки: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===== РАЗБЛОКИРОВКА =====
    @PostMapping("/unlock/{userId}")
    public ResponseEntity<?> unlockUser(@PathVariable Long userId, HttpSession session) {
        try {
            Long currentUserId = (Long) session.getAttribute("userId");
            if (currentUserId == null) {
                return ResponseEntity.badRequest().body("Пользователь не авторизован");
            }

            User user = userService.unlockUser(userId, currentUserId);
            log.info("🔓 Разблокирован пользователь: {} администратором ID: {}", user.getUsername(), currentUserId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("❌ Ошибка разблокировки: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===== АКТИВАЦИЯ =====
    @PostMapping("/activate/{userId}")
    public ResponseEntity<?> activateUser(@PathVariable Long userId) {
        try {
            User user = userService.activate(userId);
            log.info("✅ Активирован пользователь: {}", user.getUsername());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("❌ Ошибка активации: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===== ДЕАКТИВАЦИЯ =====
    @PostMapping("/deactivate/{userId}")
    public ResponseEntity<?> deactivateUser(@PathVariable Long userId) {
        try {
            User user = userService.deactivate(userId);
            log.info("⛔ Деактивирован пользователь: {}", user.getUsername());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("❌ Ошибка деактивации: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===== ПРОВЕРКА АВТОРИЗАЦИИ =====
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkAuth(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return ResponseEntity.ok(userId != null);
    }
}
