package com.alhrb.forestry.dto.abgrid;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.LinkedHashMap;
import java.util.Map;

public class GridRequest {
    private final Map<String, GridP> payloads = new LinkedHashMap<>();

    @JsonAnySetter
    public void put(String key, GridP value) {
        payloads.put(key, value);
    }

    public Map<String, GridP> getPayloads() {
        return payloads;
    }

    public GridP first() {
        return payloads.values().stream().findFirst().orElse(null);
    }

    public GridP get(String key) {
        return payloads.get(key);
    }
}
