package com.data.location;


import com.google.firebase.Timestamp;

public class LocationItem {
    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private int useCount;
    private Timestamp lastUsed;
    private Timestamp createdAt;
    private String userId;

    // Boş constructor
    public LocationItem() {
    }

    // Tam constructor
    public LocationItem(String id, String userId, String name, String address,
                        double latitude, double longitude, int useCount,
                        Timestamp lastUsed, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.useCount = useCount;
        this.lastUsed = lastUsed;
        this.createdAt = createdAt;
    }

    // Getter və Setter metodları
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address != null ? address : "";
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getUseCount() {
        return useCount;
    }

    public void setUseCount(int useCount) {
        this.useCount = useCount;
    }

    public Timestamp getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Timestamp lastUsed) {
        this.lastUsed = lastUsed;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // Display üçün köməkçi metod
    public String getDisplayName() {
        if (name != null && !name.isEmpty()) {
            return name + " - " + address;
        }
        return address;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}