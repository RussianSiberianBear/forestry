package com.alhrb.forestry.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class DateTimeUtil {
    public static LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            // Для формата "2024-01-15T14:30"
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            return LocalDateTime.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            // Пробуем другие форматы
            try {
                // Формат "2024-01-15 14:30:00"
                DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return LocalDateTime.parse(dateStr, formatter2);
            } catch (DateTimeParseException e2) {
                // Формат "15.01.2024 14:30"
                DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                return LocalDateTime.parse(dateStr, formatter3);
            }
        }
    }

    public static boolean isValidatePeriod(String dtB, String dtE, Map<String, Object> f) {
        if (f == null) return true;
        if (!f.containsKey(dtB)) return true;
        if (!f.containsKey(dtE)) return true;
        if (f.get(dtB).toString().compareTo(f.get(dtE).toString()) > 0) {
            return false;
        }
        return true;
    }

    public static String toRusFormat(LocalDateTime dt) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        String result = dt.format(formatter);
        return result;
    }
}
