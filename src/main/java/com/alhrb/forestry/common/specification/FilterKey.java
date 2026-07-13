package com.alhrb.forestry.common.specification;

public record FilterKey(
        String field,
        FilterOperation operation
) {

    public static FilterKey parse(String key) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Имя параметра фильтра не может быть пустым"
            );
        }

        int separatorIndex = key.lastIndexOf("__");

        /*
         * Если операция не указана, она будет определена
         * позднее в зависимости от типа поля:
         *
         * String → CONTAINS
         * остальные типы → EQ
         */
        if (separatorIndex < 0) {
            return new FilterKey(key, null);
        }

        String field = key.substring(0, separatorIndex);
        String operationText = key.substring(separatorIndex + 2);

        if (field.isBlank()) {
            throw new IllegalArgumentException(
                    "Не указано поле фильтра: " + key
            );
        }

        FilterOperation operation = switch (operationText) {

            case "eq" -> FilterOperation.EQ;
            case "notEq", "ne" -> FilterOperation.NOT_EQ;

            case "contains" -> FilterOperation.CONTAINS;
            case "startsWith" -> FilterOperation.STARTS_WITH;
            case "endsWith" -> FilterOperation.ENDS_WITH;

            case "gt" -> FilterOperation.GT;
            case "gte" -> FilterOperation.GTE;
            case "lt" -> FilterOperation.LT;
            case "lte" -> FilterOperation.LTE;

            case "from" -> FilterOperation.GTE;
            case "to" -> FilterOperation.LTE;

            case "in" -> FilterOperation.IN;
            case "isNull" -> FilterOperation.IS_NULL;

            default -> throw new IllegalArgumentException(
                    "Неизвестная операция фильтра: " + operationText
            );
        };

        return new FilterKey(field, operation);
    }
}