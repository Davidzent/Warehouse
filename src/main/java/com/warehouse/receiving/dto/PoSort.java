package com.warehouse.receiving.dto;

public enum PoSort {

    PO_NUMBER("po.po_number"),
    ORDER_DATE("po.order_date"),
    STATUS("po.status");

    private final String columnName;

    PoSort(String columnName) {
        this.columnName = columnName;
    }

    /** Referenced from mapper XML as ${criteria.sort.columnName}. */
    public String getColumnName() {
        return columnName;
    }
}
