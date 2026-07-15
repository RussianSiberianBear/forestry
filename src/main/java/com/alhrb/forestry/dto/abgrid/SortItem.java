package com.alhrb.forestry.dto.abgrid;

import com.fasterxml.jackson.annotation.JsonAlias;

public class SortItem {

    @JsonAlias({"alias", "field"})
    private String alias;

    @JsonAlias({"dir", "direction"})
    private String dir;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}