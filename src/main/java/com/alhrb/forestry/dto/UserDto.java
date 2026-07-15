package com.alhrb.forestry.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        @JsonProperty("__clientId")  // ← сохраняем имя поля для JSON
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String clientId,
        String username,
        String email,
        String fullName,
        String role,
        String phone,
        Boolean isActive,
        Boolean isLocked,
        String lockReason,
        LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {

    // Кастомный метод для обратной совместимости
    @JsonIgnore
    public String __clientId() {
        return clientId;
    }
}