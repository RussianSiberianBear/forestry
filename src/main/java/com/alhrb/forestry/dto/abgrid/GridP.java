package com.alhrb.forestry.dto.abgrid;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class GridP {

    private Integer page;
    private Integer rpp;

    private String oper;
    private String opId;

    private List<Map<String, Object>> rows = new ArrayList<>();

    private Map<String, Object> row = new LinkedHashMap<>();

    private Map<String, Object> data = new LinkedHashMap<>();

    private List<Long> rowIds = new ArrayList<>();

    private String __clientId;

    private Map<String, Object> extData = new LinkedHashMap<>();

    private List<SortItem> sortOrder = new ArrayList<>();

    private Map<String, Object> filter = new LinkedHashMap<>();

    public static Map<String, Object> firstRowOrNull(GridP params) {

        if (params == null
                || params.getRows() == null
                || params.getRows().isEmpty()) {

            return null;
        }

        return params.getRows().get(0);
    }
}