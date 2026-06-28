package com.alhrb.forestry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для результата массового импорта
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultDto {

    /**
     * Всего обработано записей
     */
    private Integer processed;

    /**
     * Успешно импортировано
     */
    private Integer imported;

    /**
     * С ошибками
     */
    private Integer errors;

    /**
     * Сообщения об ошибках
     */
    private String[] errorMessages;

    /**
     * ID партии загрузки
     */
    private String batchId;
}