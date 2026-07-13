package com.alhrb.forestry.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== ОСНОВНЫЕ ПОЛЯ =====
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    // ===== РОЛИ =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role = Role.USER;

    // ===== СТАТУС =====
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_locked")
    private Boolean isLocked = false;

    // ===== БЛОКИРОВКА =====
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "lock_reason", length = 500)
    private String lockReason;

    @Column(name = "locked_by")
    private Long lockedBy;

    // ===== АУДИТ =====
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "login_attempts")
    private Integer loginAttempts = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    // ===== ПРЕДУСТАНОВКИ =====
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (isLocked == null) {
            isLocked = false;
        }
        if (loginAttempts == null) {
            loginAttempts = 0;
        }
        if (role == null) {
            role = Role.USER;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== SPRING SECURITY METHODS =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + getRole().name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (!isLocked) {
            return true;
        }
        // Проверяем, не истекла ли блокировка
        if (lockedUntil != null && lockedUntil.isBefore(LocalDateTime.now())) {
            isLocked = false;
            lockedUntil = null;
            lockReason = null;
            lockedBy = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    public void lock(String reason, Long lockedBy, Integer minutes) {
        this.isLocked = true;
        this.lockReason = reason;
        this.lockedBy = lockedBy;
        this.lockedAt = LocalDateTime.now();
        if (minutes != null && minutes > 0) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(minutes);
        }
    }

    public void unlock(Long unlockedBy) {
        this.isLocked = false;
        this.lockReason = null;
        this.lockedBy = null;
        this.lockedAt = null;
        this.lockedUntil = null;
        this.updatedBy = unlockedBy;
    }

    public void incrementLoginAttempts() {
        this.loginAttempts = (this.loginAttempts == null ? 0 : this.loginAttempts) + 1;
    }

    public void resetLoginAttempts() {
        this.loginAttempts = 0;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return username;
    }
}