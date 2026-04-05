package com.DiscountMarket.store;


public class BazarCategory {
    private String id;
    private String name;
    private String url;
    private String imageUrl;
    private int productCount;

    public BazarCategory(String id, String name, String url) {
        this.id = id;
        this.name = name;
        this.url = url;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }
}