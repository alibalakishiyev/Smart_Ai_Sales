package com.data;

import java.util.Date;

public class SalesData {
    private Date date;
    private double sales;
    private double expenses;
    private double profit;
    private int customerCount;
    private double avgPrice;

    public SalesData(Date date, double sales, double expenses,
                     double profit, int customerCount, double avgPrice) {
        this.date = date;
        this.sales = sales;
        this.expenses = expenses;
        this.profit = profit;
        this.customerCount = customerCount;
        this.avgPrice = avgPrice;
    }

    // Getters and setters
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public double getSales() { return sales; }
    public void setSales(double sales) { this.sales = sales; }

    public double getExpenses() { return expenses; }
    public void setExpenses(double expenses) { this.expenses = expenses; }

    public double getProfit() { return profit; }
    public void setProfit(double profit) { this.profit = profit; }

    public int getCustomerCount() { return customerCount; }
    public void setCustomerCount(int customerCount) { this.customerCount = customerCount; }

    public double getAvgPrice() { return avgPrice; }
    public void setAvgPrice(double avgPrice) { this.avgPrice = avgPrice; }
}