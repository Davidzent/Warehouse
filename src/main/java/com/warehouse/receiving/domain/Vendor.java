package com.warehouse.receiving.domain;

public class Vendor {

    private long vendorId;
    private String vendorCode;
    private String name;
    private boolean active;

    public long getVendorId() { return vendorId; }
    public void setVendorId(long vendorId) { this.vendorId = vendorId; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
