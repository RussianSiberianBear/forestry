package com.alhrb.forestry.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserLockDto {
    @NotNull(message = "ID пользователя обязателен")
    private Long userId;

    private String reason;
    private Integer minutes;  // на сколько минут заблокировать (null = бессрочно)
}