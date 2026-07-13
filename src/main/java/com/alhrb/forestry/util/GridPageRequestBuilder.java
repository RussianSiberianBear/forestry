package com.alhrb.forestry.util;

import com.alhrb.forestry.dto.abgrid.GridP;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts ABGrid paging params (page + rpp + sortOrder) to Spring Data Pageable.
 */
public final class GridPageRequestBuilder {

    private static final int DEFAULT_RPP = 20;
    private static final int MAX_RPP = 200;

    private GridPageRequestBuilder() {
    }

    private static HashMap innerBuild(GridP p) {
        HashMap map = new HashMap();
        int page = 1;
        int rpp = DEFAULT_RPP;

        if (p != null) {
            if (p.getPage() != null && p.getPage() > 0) page = p.getPage();
            if (p.getRpp() != null && p.getRpp() > 0) rpp = p.getRpp();
        }

        rpp = Math.min(Math.max(1, rpp), MAX_RPP);
        map.put("page", page);
        map.put("rpp", rpp);
        return map;
    }

    public static Pageable build(GridP p) {
        HashMap map = innerBuild(p);
        Sort sort = (p != null) ? GridSortBuilder.build(p.getSortOrder()) : Sort.by(Sort.Order.asc("id"));
        // ABGrid uses 1-based pages; Spring Data uses 0-based.
        return PageRequest.of((Integer) map.get("page") - 1, (Integer) map.get("rpp"), sort);
    }

    public static Pageable build(GridP p, Map<String, String> whiteList) {
        HashMap map = innerBuild(p);
        Sort sort = (p != null) ? GridSortBuilder.build(p.getSortOrder(), whiteList) : Sort.by(Sort.Order.asc("id"));
        // ABGrid uses 1-based pages; Spring Data uses 0-based.
        return PageRequest.of((Integer) map.get("page") - 1, (Integer) map.get("rpp"), sort);
    }
}
