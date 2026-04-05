package com.DiscountMarket.model;

import java.io.Serializable;

public class DiscountProduct implements Serializable {
    private String id;
    private String storeName;
    private String storeLogoRes;
    private String productName;
    private double originalPrice;
    private double discountPrice;
    private double discountPercent;
    private String imageUrl;
    private String productUrl;
    private boolean isBestPrice;
    private String category;
    private String unit; // KG, AD, ML vb.

    public DiscountProduct() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getStoreLogoRes() { return storeLogoRes; }
    public void setStoreLogoRes(String storeLogoRes) { this.storeLogoRes = storeLogoRes; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public double getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(double discountPrice) { this.discountPrice = discountPrice; }

    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getProductUrl() { return productUrl; }
    public void setProductUrl(String productUrl) { this.productUrl = productUrl; }

    public boolean isBestPrice() { return isBestPrice; }
    public void setBestPrice(boolean bestPrice) { isBestPrice = bestPrice; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}