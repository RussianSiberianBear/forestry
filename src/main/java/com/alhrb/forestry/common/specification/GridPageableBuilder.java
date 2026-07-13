package com.alhrb.forestry.common.specification;

import com.alhrb.forestry.dto.abgrid.GridP;
import com.alhrb.forestry.dto.abgrid.SortItem;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class GridPageableBuilder {

    private static final int DEFAULT_RPP = 20;
    private static final int MAX_RPP = 200;

    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Sort.Direction DEFAULT_SORT_DIRECTION =
            Sort.Direction.ASC;

    private GridPageableBuilder() {
    }

    public static Pageable build(
            GridP gridParams,
            Set<String> allowedSortFields
    ) {
        return build(
                gridParams,
                allowedSortFields,
                DEFAULT_SORT_FIELD,
                DEFAULT_SORT_DIRECTION
        );
    }

    public static Pageable build(
            GridP gridParams,
            Set<String> allowedSortFields,
            String defaultSortField,
            Sort.Direction defaultSortDirection
    ) {
        validateArguments(
                allowedSortFields,
                defaultSortField
        );

        int page = resolvePage(gridParams);
        int rpp = resolveRpp(gridParams);

        Sort sort = buildSort(
                gridParams == null
                        ? null
                        : gridParams.getSortOrder(),
                allowedSortFields,
                defaultSortField,
                defaultSortDirection
        );

        return PageRequest.of(
                page,
                rpp,
                sort
        );
    }

    private static int resolvePage(GridP gridParams) {

        if (gridParams == null || gridParams.getPage() == null) {
            return 0;
        }

        /*
         * ABGrid нумерует страницы с единицы:
         *
         * 1, 2, 3...
         *
         * Spring Data:
         *
         * 0, 1, 2...
         */
        return Math.max(gridParams.getPage() - 1, 0);
    }

    private static int resolveRpp(GridP gridParams) {

        if (gridParams == null || gridParams.getRpp() == null) {
            return DEFAULT_RPP;
        }

        return Math.max(
                1,
                Math.min(
                        gridParams.getRpp(),
                        MAX_RPP
                )
        );
    }

    private static Sort buildSort(
            List<SortItem> sortOrder,
            Set<String> allowedSortFields,
            String defaultSortField,
            Sort.Direction defaultSortDirection
    ) {
        if (sortOrder == null || sortOrder.isEmpty()) {
            return defaultSort(
                    defaultSortField,
                    defaultSortDirection
            );
        }

        List<Sort.Order> orders = new ArrayList<>();

        for (SortItem sortItem : sortOrder) {

            if (sortItem == null) {
                continue;
            }

            String field = normalizeField(
                    sortItem.getAlias()
            );

            if (field == null) {
                continue;
            }

            if (!allowedSortFields.contains(field)) {
                /*
                 * Клиент прислал неизвестное либо запрещённое поле.
                 * Просто не включаем его в сортировку.
                 */
                continue;
            }

            Sort.Direction direction =
                    parseDirection(sortItem.getDir());

            orders.add(
                    new Sort.Order(
                            direction,
                            field
                    )
            );
        }

        if (orders.isEmpty()) {
            return defaultSort(
                    defaultSortField,
                    defaultSortDirection
            );
        }

        /*
         * Добавляем id как дополнительное поле сортировки.
         *
         * Это делает порядок строк стабильным, когда значения
         * основного поля одинаковые.
         */
        boolean containsId = orders.stream()
                .anyMatch(order ->
                        DEFAULT_SORT_FIELD.equals(
                                order.getProperty()
                        )
                );

        if (!containsId
                && allowedSortFields.contains(DEFAULT_SORT_FIELD)) {

            orders.add(
                    Sort.Order.asc(DEFAULT_SORT_FIELD)
            );
        }

        return Sort.by(orders);
    }

    private static Sort.Direction parseDirection(
            String direction
    ) {
        return "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
    }

    private static String normalizeField(String field) {

        if (field == null) {
            return null;
        }

        String normalized = field.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private static Sort defaultSort(
            String field,
            Sort.Direction direction
    ) {
        return Sort.by(
                new Sort.Order(
                        direction == null
                                ? DEFAULT_SORT_DIRECTION
                                : direction,
                        field
                )
        );
    }

    private static void validateArguments(
            Set<String> allowedSortFields,
            String defaultSortField
    ) {
        if (allowedSortFields == null
                || allowedSortFields.isEmpty()) {

            throw new IllegalArgumentException(
                    "Список разрешённых полей сортировки не задан"
            );
        }

        if (defaultSortField == null
                || defaultSortField.isBlank()) {

            throw new IllegalArgumentException(
                    "Поле сортировки по умолчанию не задано"
            );
        }

        if (!allowedSortFields.contains(defaultSortField)) {
            throw new IllegalArgumentException(
                    "Поле сортировки по умолчанию '%s' отсутствует в белом списке"
                            .formatted(defaultSortField)
            );
        }
    }
}