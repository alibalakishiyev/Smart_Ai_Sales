package com.dashboard;


import java.util.ArrayList;
import java.util.List;

public class ProductAnalysis {
    public String productName;
    public String category;
    public double totalAmount;
    public double totalQuantity;
    public int transactionCount;
    public double avgAmount;
    public double maxAmount;
    public double minAmount;
    public List<Double> recentAmounts = new ArrayList<>();
    public List<Long> recentDates = new ArrayList<>();
    public double growthRate;
    public String trend;
    public double predictedNextAmount;

    public ProductAnalysis(String productName, String category) {
        this.productName = productName;
        this.category = category;
        this.totalAmount = 0;
        this.totalQuantity = 0;
        this.transactionCount = 0;
        this.maxAmount = Double.MIN_VALUE;
        this.minAmount = Double.MAX_VALUE;
    }

    public void addTransaction(double amount, double quantity, long date) {
        this.totalAmount += amount;
        this.totalQuantity += quantity;
        this.transactionCount++;
        this.avgAmount = this.totalAmount / this.transactionCount;

        if (amount > this.maxAmount) this.maxAmount = amount;
        if (amount < this.minAmount) this.minAmount = amount;

        this.recentAmounts.add(amount);
        this.recentDates.add(date);
    }

    public void calculateGrowthRate() {
        if (recentAmounts.size() < 2) {
            this.growthRate = 0;
            this.trend = "📊 Məlumat az";
            return;
        }

        int size = Math.min(5, recentAmounts.size());
        double firstAvg = 0, lastAvg = 0;
        int halfSize = size / 2;

        for (int i = 0; i < size; i++) {
            if (i < halfSize) {
                firstAvg += recentAmounts.get(i);
            } else {
                lastAvg += recentAmounts.get(i);
            }
        }

        firstAvg = firstAvg / halfSize;
        lastAvg = lastAvg / (size - halfSize);

        if (firstAvg > 0) {
            this.growthRate = ((lastAvg - firstAvg) / firstAvg) * 100;
        } else {
            this.growthRate = 0;
        }

        if (growthRate > 20) {
            this.trend = "🚀 Sürətli artım";
        } else if (growthRate > 5) {
            this.trend = "📈 Yavaş artım";
        } else if (growthRate > -5) {
            this.trend = "📊 Stabil";
        } else if (growthRate > -20) {
            this.trend = "📉 Yavaş eniş";
        } else {
            this.trend = "⚠️ Sürətli eniş";
        }
    }

    public void predictNextAmount() {
        if (recentAmounts.size() < 3) {
            this.predictedNextAmount = this.avgAmount;
            return;
        }

        double sum = 0;
        int count = 0;
        for (int i = 1; i < recentAmounts.size(); i++) {
            sum += recentAmounts.get(i) - recentAmounts.get(i-1);
            count++;
        }

        double avgChange = count > 0 ? sum / count : 0;
        this.predictedNextAmount = recentAmounts.get(recentAmounts.size() - 1) + avgChange;
        if (this.predictedNextAmount < 0) this.predictedNextAmount = 0;
    }

    // Getter metodları
    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public double getTotalAmount() { return totalAmount; }
    public double getTotalQuantity() { return totalQuantity; }
    public int getTransactionCount() { return transactionCount; }
    public double getAvgAmount() { return avgAmount; }
    public double getMaxAmount() { return maxAmount; }
    public double getMinAmount() { return minAmount; }
    public List<Double> getRecentAmounts() { return recentAmounts; }
    public List<Long> getRecentDates() { return recentDates; }
    public double getGrowthRate() { return growthRate; }
    public String getTrend() { return trend; }
    public double getPredictedNextAmount() { return predictedNextAmount; }
}
