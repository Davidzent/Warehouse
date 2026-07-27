package com.warehouse.receiving.dto;

import java.time.LocalDate;

import com.warehouse.receiving.domain.PoStatus;

public class PurchaseOrderSummary {

    private long poId;
    private String poNumber;
    private String vendorCode;
    private String vendorName;
    private PoStatus status;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private int lineCount;
    private int totalOrdered;
    private int totalReceived;

    public long getPoId() { return poId; }
    public void setPoId(long poId) { this.poId = poId; }

    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public PoStatus getStatus() { return status; }
    public void setStatus(PoStatus status) { this.status = status; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }

    public int getLineCount() { return lineCount; }
    public void setLineCount(int lineCount) { this.lineCount = lineCount; }

    public int getTotalOrdered() { return totalOrdered; }
    public void setTotalOrdered(int totalOrdered) { this.totalOrdered = totalOrdered; }

    public int getTotalReceived() { return totalReceived; }
    public void setTotalReceived(int totalReceived) { this.totalReceived = totalReceived; }
}
