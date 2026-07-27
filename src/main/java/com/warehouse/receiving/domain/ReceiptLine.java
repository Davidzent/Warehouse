package com.warehouse.receiving.domain;

public class ReceiptLine {

    private long receiptLineId; 
    private long receiptId;
    private long poLineId;
    private long locationId;
    private int quantityReceived;
    private int quantityDamaged;

    // Joined display fields for GET /api/receipts/{id}.
    private String sku;
    private String locationCode;

    public long getReceiptLineId() { return receiptLineId; }
    public void setReceiptLineId(long receiptLineId) { this.receiptLineId = receiptLineId; }

    public long getReceiptId() { return receiptId; }
    public void setReceiptId(long receiptId) { this.receiptId = receiptId; }

    public long getPoLineId() { return poLineId; }
    public void setPoLineId(long poLineId) { this.poLineId = poLineId; }

    public long getLocationId() { return locationId; }
    public void setLocationId(long locationId) { this.locationId = locationId; }

    public int getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(int quantityReceived) { this.quantityReceived = quantityReceived; }

    public int getQuantityDamaged() { return quantityDamaged; }
    public void setQuantityDamaged(int quantityDamaged) { this.quantityDamaged = quantityDamaged; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
}
