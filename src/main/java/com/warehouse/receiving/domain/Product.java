package com.warehouse.receiving.domain;

public class Product {

    private long productId;
    private String sku;
    private String description;
    private String unitOfMeasure;

    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
}
