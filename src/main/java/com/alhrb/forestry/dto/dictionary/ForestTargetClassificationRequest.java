package com.alhrb.forestry.dto.dictionary;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForestTargetClassificationRequest {

    private Long id;

    private Long parentId;

    @NotBlank(message = "Название не может быть пустым")
    @Size(max = 255, message = "Название не должно превышать 255 символов")
    private String name;

    private String fullName;

    @Size(max = 20, message = "Код не должен превышать 20 символов")
    private String code;

    @Min(value = 1, message = "Уровень должен быть от 1 до 3")
    @Max(value = 3, message = "Уровень должен быть от 1 до 3")
    private Short level;
}