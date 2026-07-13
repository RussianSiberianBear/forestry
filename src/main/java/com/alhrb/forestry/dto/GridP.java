package com.alhrb.forestry.dto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GridP {
    private Integer page;
    private Integer rpp;
    private String oper;
    private String opId;
    private ArrayList rows;
    private  LinkedHashMap row;
    private Map<String, Object> data = new LinkedHashMap<>();
    private ArrayList rowIds;
    private String __clientId;
    private Map<String, Object> extData = new LinkedHashMap<>();
    private List<SortItem> sortOrder;
    private Map<String, Object> filter = new LinkedHashMap<>();

    public void setRowIds(ArrayList rowIds) {
        this.rowIds = rowIds;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public LinkedHashMap getRow() {
        return row;
    }

    public void setRow(LinkedHashMap row) {
        this.row = row;
    }

    public void setRows(ArrayList rows) {
        this.rows = rows;
    }

    public ArrayList getRowIds() { return rowIds; }

    public ArrayList getRows() {
        return rows;
    }

    public Integer getPage() { return page; }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getRpp() { return rpp; }

    public void setRpp(Integer rpp) {
        this.rpp = rpp;
    }

    public List<SortItem> getSortOrder() { return sortOrder; }

    public Map<String, Object> getExtData() { return extData; }

    public Map<String, Object> getFilter() { return filter; }

    public void setFilter(Map<String, Object> filter) { this.filter = filter; }

    public String getClientId() { return __clientId; }

    public String getOper() {
        return oper;
    }

    public void setOper(String oper) {
        this.oper = oper;
    }

    public String getOpId() {
        return opId;
    }

    public void setOpId(String opId) {
        this.opId = opId;
    }

    public void setSortOrder(List<SortItem> sortOrder) {
        this.sortOrder = sortOrder;
    }

    // --- вспомогательное: если когда-то понадобится ---
    public static LinkedHashMap firstRowOrNull(GridP p) {
        ArrayList<LinkedHashMap> rows = p.getRows();
        if (rows == null || rows.isEmpty()) return null;
        return rows.get(0);
    }

}
