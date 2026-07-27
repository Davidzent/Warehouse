package com.warehouse.receiving.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class PurchaseOrder {

    private long poId;
    private String poNumber;
    private long vendorId;
    private PoStatus status;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private OffsetDateTime createdAt;
    private String createdBy;
    private OffsetDateTime updatedAt;
    private String updatedBy;

    
    /** Populated by the resultMap's <association> from the joined vendor columns. */
    private Vendor vendor;

    /** Populated by the resultMap's <collection> from the joined line columns. */
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    public long getPoId() { return poId; }
    public void setPoId(long poId) { this.poId = poId; }

    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }

    public long getVendorId() { return vendorId; }
    public void setVendorId(long vendorId) { this.vendorId = vendorId; }

    public PoStatus getStatus() { return status; }
    public void setStatus(PoStatus status) { this.status = status; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }

    public List<PurchaseOrderLine> getLines() { return lines; }
    public void setLines(List<PurchaseOrderLine> lines) { this.lines = lines; }
}
