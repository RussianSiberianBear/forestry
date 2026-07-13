package com.alhrb.forestry.common.specification;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public final class FilterValueConverter {

    private FilterValueConverter() {
    }

    public static Object convert(Object value, Class<?> targetType) {

        if (value == null) {
            return null;
        }

        Class<?> effectiveType = wrapPrimitive(targetType);

        if (effectiveType.isInstance(value)) {
            return value;
        }

        String text = value.toString().trim();

        if (effectiveType == String.class) {
            return text;
        }

        if (text.isEmpty()) {
            return null;
        }

        try {
            if (effectiveType == Long.class) {
                return Long.valueOf(text);
            }

            if (effectiveType == Integer.class) {
                return Integer.valueOf(text);
            }

            if (effectiveType == Short.class) {
                return Short.valueOf(text);
            }

            if (effectiveType == Byte.class) {
                return Byte.valueOf(text);
            }

            if (effectiveType == Double.class) {
                return Double.valueOf(text);
            }

            if (effectiveType == Float.class) {
                return Float.valueOf(text);
            }

            if (effectiveType == BigDecimal.class) {
                return new BigDecimal(text);
            }

            if (effectiveType == BigInteger.class) {
                return new BigInteger(text);
            }

            if (effectiveType == Boolean.class) {
                return parseBoolean(text);
            }

            if (effectiveType == Character.class) {
                if (text.length() != 1) {
                    throw new IllegalArgumentException(
                            "Ожидался один символ, получено: " + text
                    );
                }

                return text.charAt(0);
            }

            if (effectiveType == LocalDate.class) {
                return LocalDate.parse(text);
            }

            if (effectiveType == LocalDateTime.class) {
                return parseLocalDateTime(text);
            }

            if (effectiveType == LocalTime.class) {
                return LocalTime.parse(text);
            }

            if (effectiveType == Instant.class) {
                return Instant.parse(text);
            }

            if (effectiveType == OffsetDateTime.class) {
                return OffsetDateTime.parse(text);
            }

            if (effectiveType == ZonedDateTime.class) {
                return ZonedDateTime.parse(text);
            }

            if (effectiveType == UUID.class) {
                return UUID.fromString(text);
            }

            if (effectiveType.isEnum()) {
                return convertEnum(text, effectiveType);
            }

        } catch (NumberFormatException |
                 DateTimeParseException exception) {

            throw new IllegalArgumentException(
                    "Невозможно преобразовать значение '%s' в тип %s"
                            .formatted(text, effectiveType.getSimpleName()),
                    exception
            );
        }

        throw new IllegalArgumentException(
                "Тип поля не поддерживается фильтром: "
                        + effectiveType.getName()
        );
    }

    private static Boolean parseBoolean(String value) {

        return switch (value.toLowerCase()) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;

            default -> throw new IllegalArgumentException(
                    "Некорректное логическое значение: " + value
            );
        };
    }

    /*
     * Если клиент передал только дату:
     *
     * 2026-07-13
     *
     * она преобразуется в начало суток:
     *
     * 2026-07-13T00:00:00
     */
    private static LocalDateTime parseLocalDateTime(String value) {

        if (value.length() == 10) {
            return LocalDate.parse(value).atStartOfDay();
        }

        return LocalDateTime.parse(value);
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private static Object convertEnum(
            String value,
            Class<?> enumType
    ) {

        for (Object enumConstant : enumType.getEnumConstants()) {

            Enum enumValue = (Enum) enumConstant;

            if (enumValue.name().equalsIgnoreCase(value)) {
                return enumValue;
            }
        }

        throw new IllegalArgumentException(
                "Неизвестное значение enum %s: %s"
                        .formatted(enumType.getSimpleName(), value)
        );
    }

    private static Class<?> wrapPrimitive(Class<?> type) {

        if (!type.isPrimitive()) {
            return type;
        }

        if (type == long.class) {
            return Long.class;
        }

        if (type == int.class) {
            return Integer.class;
        }

        if (type == short.class) {
            return Short.class;
        }

        if (type == byte.class) {
            return Byte.class;
        }

        if (type == double.class) {
            return Double.class;
        }

        if (type == float.class) {
            return Float.class;
        }

        if (type == boolean.class) {
            return Boolean.class;
        }

        if (type == char.class) {
            return Character.class;
        }

        return type;
    }
}