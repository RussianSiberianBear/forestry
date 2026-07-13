package com.alhrb.forestry.util;

import com.alhrb.forestry.dto.abgrid.SortItem;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Builds Spring Data Sort from ABGrid sortOrder.
 */
public final class GridSortBuilder {

    /**
     * Whitelist: alias from client -> entity property.
     * Prevents sorting by arbitrary / unknown properties.
     */
    private static final Map<String, String> ALIAS_TO_PROP = Map.ofEntries(
            entry("id", "id"),
            entry("name", "name"),
            entry("blocked", "blocked"),
            entry("password", "password"),
            entry("username", "username"),
            entry("email", "email"),
            entry("role", "role"),
            entry("blockedAt", "blocked_at"),
            entry("registeredAt", "registered_at"),
            entry("reasonBlocking", "reason_blocking"),
            entry("blockingUser", "blockingUser"),
            entry("userId", "userId"),
            entry("createdAt", "createdAt"),
            entry("expiresAt", "expiresAt"),
            entry("usedAt", "usedAt"),
            entry("tokenHash", "tokenHash"),
            entry("topicId", "topicId"),
            entry("body", "body"),
            entry("closed", "closed"),
            entry("title", "title"),
            entry("description", "description"),
            entry("code", "code"),
            entry("active", "active")
    );

    private GridSortBuilder() {
    }

    private static Sort innerBuild(List<SortItem> sortOrder, Map<String, String> whiteList) {
        if (sortOrder == null || sortOrder.isEmpty()) {
            return Sort.by(Sort.Order.asc("id"));
        }
        List<Sort.Order> orders = new ArrayList<>();

        for (SortItem so : sortOrder) {
            if (so == null) continue;

            String alias = so.getAlias();
            if (alias == null || alias.isBlank()) continue;

            String prop = whiteList.get(alias);
            if (prop == null) continue;

            boolean desc = "desc".equalsIgnoreCase(so.getDir());
            orders.add(desc ? Sort.Order.desc(prop) : Sort.Order.asc(prop));
        }

        return orders.isEmpty()
                ? Sort.by(Sort.Order.asc("id"))
                : Sort.by(orders);

    }

    public static Sort build(List<SortItem> sortOrder) {
        return  innerBuild(sortOrder, ALIAS_TO_PROP);
    }
    public static Sort build(List<SortItem> sortOrder,Map<String, String> whiteList) {
        return  innerBuild(sortOrder, whiteList);
    }

}
