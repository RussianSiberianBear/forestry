package com.alhrb.forestry.common.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DynamicSpecificationBuilder {

    private DynamicSpecificationBuilder() {
    }

    public static <T> Specification<T> build(
            Map<String, Object> filter,
            Set<String> allowedFields
    ) {

        return (root, query, criteriaBuilder) -> {

            if (filter == null || filter.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            if (allowedFields == null || allowedFields.isEmpty()) {
                throw new IllegalArgumentException(
                        "Не задан список разрешённых полей фильтрации"
                );
            }

            List<Predicate> predicates = new ArrayList<>();

            for (Map.Entry<String, Object> entry : filter.entrySet()) {

                Object rawValue = entry.getValue();

                /*
                 * Пустое значение фильтра игнорируем.
                 * Для IS NULL значение должно быть true или false,
                 * поэтому оно здесь не потеряется.
                 */
                if (isEmptyValue(rawValue)) {
                    continue;
                }

                FilterKey filterKey = FilterKey.parse(entry.getKey());

                validateAllowedField(
                        filterKey.field(),
                        allowedFields
                );

                Path<?> path = resolvePath(
                        root,
                        filterKey.field()
                );

                Class<?> fieldType = path.getJavaType();

                FilterOperation operation =
                        filterKey.operation() != null
                                ? filterKey.operation()
                                : defaultOperation(fieldType);

                Predicate predicate = createPredicate(
                        criteriaBuilder,
                        path,
                        fieldType,
                        operation,
                        rawValue
                );

                predicates.add(predicate);
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.and(
                    predicates.toArray(Predicate[]::new)
            );
        };
    }

    private static Predicate createPredicate(
            CriteriaBuilder cb,
            Path<?> path,
            Class<?> fieldType,
            FilterOperation operation,
            Object rawValue
    ) {

        return switch (operation) {

            case EQ -> createEqualPredicate(
                    cb,
                    path,
                    fieldType,
                    rawValue
            );

            case NOT_EQ -> createNotEqualPredicate(
                    cb,
                    path,
                    fieldType,
                    rawValue
            );

            case CONTAINS -> createLikePredicate(
                    cb,
                    path,
                    rawValue,
                    LikeMode.CONTAINS
            );

            case STARTS_WITH -> createLikePredicate(
                    cb,
                    path,
                    rawValue,
                    LikeMode.STARTS_WITH
            );

            case ENDS_WITH -> createLikePredicate(
                    cb,
                    path,
                    rawValue,
                    LikeMode.ENDS_WITH
            );

            case GT -> createComparisonPredicate(
                    cb,
                    path,
                    fieldType,
                    rawValue,
                    ComparisonMode.GT
            );

            case GTE -> createComparisonPredicate(
                    cb,
                    path,
                    fieldType,
                    rawValue,
                    ComparisonMode.GTE
            );

            case LT -> createComparisonPredicate(
                    cb,
                    path,
                    fieldType,
                    rawValue,
                    ComparisonMode.LT
            );

            case LTE -> createComparisonPredicate(
                    cb,
                    path,
                    fieldType,
                    rawValue,
                    ComparisonMode.LTE
            );

            case IN -> createInPredicate(
                    path,
                    fieldType,
                    rawValue
            );

            case IS_NULL -> createNullPredicate(
                    cb,
                    path,
                    rawValue
            );
        };
    }

    private static Predicate createEqualPredicate(
            CriteriaBuilder cb,
            Path<?> path,
            Class<?> fieldType,
            Object rawValue
    ) {

        Object value = FilterValueConverter.convert(
                rawValue,
                fieldType
        );

        if (value == null) {
            return cb.isNull(path);
        }

        /*
         * Для String точное сравнение тоже делаем
         * без учёта регистра.
         */
        if (fieldType == String.class) {

            String text = value.toString()
                    .toLowerCase(Locale.ROOT);

            return cb.equal(
                    cb.lower(path.as(String.class)),
                    text
            );
        }

        return cb.equal(path, value);
    }

    private static Predicate createNotEqualPredicate(
            CriteriaBuilder cb,
            Path<?> path,
            Class<?> fieldType,
            Object rawValue
    ) {

        Object value = FilterValueConverter.convert(
                rawValue,
                fieldType
        );

        if (value == null) {
            return cb.isNotNull(path);
        }

        if (fieldType == String.class) {

            String text = value.toString()
                    .toLowerCase(Locale.ROOT);

            return cb.notEqual(
                    cb.lower(path.as(String.class)),
                    text
            );
        }

        return cb.notEqual(path, value);
    }

    private static Predicate createLikePredicate(
            CriteriaBuilder cb,
            Path<?> path,
            Object rawValue,
            LikeMode mode
    ) {

        if (path.getJavaType() != String.class) {
            throw new IllegalArgumentException(
                    "Операция LIKE применима только к String, поле: "
                            + path.getAlias()
            );
        }

        String text = rawValue.toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        String escaped = escapeLike(text);

        String pattern = switch (mode) {
            case CONTAINS -> "%" + escaped + "%";
            case STARTS_WITH -> escaped + "%";
            case ENDS_WITH -> "%" + escaped;
        };

        return cb.like(
                cb.lower(path.as(String.class)),
                pattern,
                '\\'
        );
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private static Predicate createComparisonPredicate(
            CriteriaBuilder cb,
            Path<?> path,
            Class<?> fieldType,
            Object rawValue,
            ComparisonMode mode
    ) {

        Object value = convertComparisonValue(
                rawValue,
                fieldType,
                mode
        );

        if (!(value instanceof Comparable comparable)) {
            throw new IllegalArgumentException(
                    "Поле типа %s нельзя сравнивать"
                            .formatted(fieldType.getSimpleName())
            );
        }

        Expression<? extends Comparable> expression =
                (Expression<? extends Comparable>) path;

        return switch (mode) {
            case GT -> cb.greaterThan(
                    expression,
                    comparable
            );

            case GTE -> cb.greaterThanOrEqualTo(
                    expression,
                    comparable
            );

            case LT -> cb.lessThan(
                    expression,
                    comparable
            );

            case LTE -> cb.lessThanOrEqualTo(
                    expression,
                    comparable
            );
        };
    }

    private static Object convertComparisonValue(
            Object rawValue,
            Class<?> fieldType,
            ComparisonMode mode
    ) {

        /*
         * Для LocalDateTime разрешаем передавать простую дату.
         *
         * createdAt__from = 2026-07-13
         * превращается в 2026-07-13T00:00:00
         *
         * createdAt__to = 2026-07-13
         * превращается в 2026-07-13T23:59:59.999999999
         */
        if (fieldType == LocalDateTime.class
                && isDateOnly(rawValue)) {

            LocalDate date = LocalDate.parse(
                    rawValue.toString().trim()
            );

            return switch (mode) {
                case LT, LTE -> date.atTime(LocalTime.MAX);

                case GT, GTE -> date.atStartOfDay();
            };
        }

        return FilterValueConverter.convert(
                rawValue,
                fieldType
        );
    }

    private static Predicate createInPredicate(
            Path<?> path,
            Class<?> fieldType,
            Object rawValue
    ) {

        Collection<?> rawValues = toCollection(rawValue);

        if (rawValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "Список для операции IN не может быть пустым"
            );
        }

        List<Object> convertedValues = rawValues.stream()
                .map(value ->
                        FilterValueConverter.convert(
                                value,
                                fieldType
                        )
                )
                .toList();

        return path.in(convertedValues);
    }

    private static Predicate createNullPredicate(
            CriteriaBuilder cb,
            Path<?> path,
            Object rawValue
    ) {

        Object converted =
                FilterValueConverter.convert(
                        rawValue,
                        Boolean.class
                );

        boolean shouldBeNull = Boolean.TRUE.equals(converted);

        return shouldBeNull
                ? cb.isNull(path)
                : cb.isNotNull(path);
    }

    private static FilterOperation defaultOperation(
            Class<?> fieldType
    ) {

        return fieldType == String.class
                ? FilterOperation.CONTAINS
                : FilterOperation.EQ;
    }

    /*
     * Поддерживает также вложенные пути:
     *
     * forestryUnit.id
     * organization.name
     */
    private static Path<?> resolvePath(
            Root<?> root,
            String field
    ) {

        String[] parts = field.split("\\.");

        Path<?> path = root;

        try {
            for (String part : parts) {
                path = path.get(part);
            }

            return path;

        } catch (IllegalArgumentException exception) {

            throw new IllegalArgumentException(
                    "Поле '%s' отсутствует в сущности %s"
                            .formatted(
                                    field,
                                    root.getJavaType().getSimpleName()
                            ),
                    exception
            );
        }
    }

    private static void validateAllowedField(
            String field,
            Set<String> allowedFields
    ) {

        if (!allowedFields.contains(field)) {
            throw new IllegalArgumentException(
                    "Фильтрация по полю '%s' запрещена"
                            .formatted(field)
            );
        }
    }

    private static boolean isEmptyValue(Object value) {

        if (value == null) {
            return true;
        }

        if (value instanceof String stringValue) {
            return stringValue.isBlank();
        }

        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }

        return false;
    }

    private static Collection<?> toCollection(Object value) {

        if (value instanceof Collection<?> collection) {
            return collection;
        }

        if (value != null && value.getClass().isArray()) {

            int length = java.lang.reflect.Array.getLength(value);

            List<Object> result = new ArrayList<>(length);

            for (int index = 0; index < length; index++) {
                result.add(
                        java.lang.reflect.Array.get(value, index)
                );
            }

            return result;
        }

        /*
         * Разрешаем также строку:
         *
         * USER,ADMIN
         */
        if (value instanceof String stringValue) {

            return List.of(
                    stringValue.split("\\s*,\\s*")
            );
        }

        return List.of(value);
    }

    private static boolean isDateOnly(Object value) {

        if (value == null) {
            return false;
        }

        return value.toString()
                .trim()
                .matches("\\d{4}-\\d{2}-\\d{2}");
    }

    private static String escapeLike(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private enum LikeMode {
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH
    }

    private enum ComparisonMode {
        GT,
        GTE,
        LT,
        LTE
    }
}
