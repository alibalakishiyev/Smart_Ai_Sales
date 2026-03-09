package com.data;

import com.google.firebase.Timestamp;

public class ProductItem {
    private String id;
    private String name;
    private String category;
    private double price;
    private double kg;
    private double liter;
    private int quantity;
    private String userId;
    private Timestamp createdAt;

    // 1. Boş constructor (Firebase üçün)
    public ProductItem() {
    }

    // 2. Sadə constructor
    public ProductItem(String name, int quantity, double price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.kg = 0;
        this.liter = 0;
    }

    // 3. Tam constructor
    public ProductItem(String id, String name, String category, double price,
                       double kg, double liter, int quantity, String userId,
                       Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.kg = kg;
        this.liter = liter;
        this.quantity = quantity;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    // Getter metodları
    public String getId() {
        return id;
    }

    // Setter metodları
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category != null ? category : "";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getKg() {
        return kg;
    }

    public void setKg(double kg) {
        this.kg = kg;
    }

    public double getLiter() {
        return liter;
    }

    public void setLiter(double liter) {
        this.liter = liter;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // Display üçün köməkçi metod
    public String getDisplayName() {
        String display = name != null ? name : "Məhsul";
        if (kg > 0) {
            display += " (" + kg + " kq)";
        } else if (liter > 0) {
            display += " (" + liter + " L)";
        }
        return display;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}