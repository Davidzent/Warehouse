package com.warehouse.receiving.domain;

import java.math.BigDecimal;


public class PurchaseOrderLine {

    private long poLineId;
    private long poId;
    private int lineNumber;
    private long productId;
    private int quantityOrdered;
    private int quantityReceived;

    private BigDecimal unitCost;

    // Joined from product for display; not columns of purchase_order_line.
    private String sku;
    private String productDescription;

    public long getPoLineId() { return poLineId; }
    public void setPoLineId(long poLineId) { this.poLineId = poLineId; }

    public long getPoId() { return poId; }
    public void setPoId(long poId) { this.poId = poId; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }

    public int getQuantityOrdered() { return quantityOrdered; }
    public void setQuantityOrdered(int quantityOrdered) { this.quantityOrdered = quantityOrdered; }

    public int getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(int quantityReceived) { this.quantityReceived = quantityReceived; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }
}
