package com.warehouse.receiving.domain;

import java.time.OffsetDateTime;

public class Inventory {

    private long inventoryId;
    private long productId;
    private long locationId;
    private int quantityOnHand;
    private OffsetDateTime updatedAt;

    // Joined display fields for GET /api/inventory.
    private String sku;
    private String productDescription;
    private String locationCode;

    public long getInventoryId() { return inventoryId; }
    public void setInventoryId(long inventoryId) { this.inventoryId = inventoryId; }

    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }

    public long getLocationId() { return locationId; }
    public void setLocationId(long locationId) { this.locationId = locationId; }

    public int getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(int quantityOnHand) { this.quantityOnHand = quantityOnHand; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }
}
