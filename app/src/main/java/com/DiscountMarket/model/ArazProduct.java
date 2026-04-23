package com.DiscountMarket.model;


import com.google.gson.annotations.SerializedName;

public class ArazProduct {
    @SerializedName("name")
    private String productName;

    @SerializedName("price")
    private double price;

    @SerializedName("discount_percent")
    private double discountPercent;

    @SerializedName("old_price")
    private Double oldPrice;

    @SerializedName("category_id")
    private int categoryId;

    @SerializedName("has_discount")
    private boolean hasDiscount;

    @SerializedName("image")
    private String imageUrl;

    @SerializedName("id")
    private int id;

    @SerializedName("slug")
    private String slug;

    @SerializedName("unit")
    private String unit;

    // Getters
    public String getProductName() { return productName; }
    public double getPrice() { return price; }
    public double getDiscountPercent() { return discountPercent; }
    public Double getOldPrice() { return oldPrice; }
    public int getCategoryId() { return categoryId; }
    public boolean hasDiscount() { return hasDiscount; }
    public String getImageUrl() { return imageUrl; }
    public int getId() { return id; }
    public String getSlug() { return slug; }
    public String getUnit() { return unit; }
}
