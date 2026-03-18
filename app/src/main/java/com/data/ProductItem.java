package com.data;

import com.google.firebase.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProductItem {

    private ProductItem copyProduct(ProductItem original) {
        ProductItem copy = new ProductItem();
        copy.setId(original.getId());
        copy.setName(original.getName());
        copy.setCategory(original.getCategory());
        copy.setPrice(original.getPrice());
        copy.setKg(original.getKg());
        copy.setLiter(original.getLiter());
        copy.setQuantity(original.getQuantity());
        copy.setUserId(original.getUserId());
        copy.setCreatedAt(original.getCreatedAt());
        copy.setSelected(original.isSelected());
        copy.setReceiptId(original.getReceiptId());
        copy.setStoreName(original.getStoreName());
        copy.setPurchaseDate(original.getPurchaseDate());
        copy.setTaxAmount(original.getTaxAmount());
        copy.setTaxFree(original.isTaxFree());
        copy.setFiscalCode(original.getFiscalCode());
        copy.setBarcode(original.getBarcode());
        return copy;
    }
    private String id;
    private String name;
    private String category;
    private double price;
    private double kg;
    private double liter;
    private int quantity;
    private String userId;
    private Timestamp createdAt;

    // Məhsulun seçilib-seçilmədiyini göstərir
    private boolean isSelected;

    // Qəbzdən əlavə məlumatlar
    private String receiptId;
    private String storeName;
    private Date purchaseDate;
    private double totalAmount;
    private double taxAmount;
    private boolean isTaxFree;
    private String fiscalCode;
    private String barcode;

    // 1. Boş constructor (Firebase üçün)
    public ProductItem() {
        this.isSelected = false;
        this.price = 0;
        this.quantity = 0;
        this.kg = 0;
        this.liter = 0;
    }



    // 2. Sadə constructor - BURADA PRICE MÜTLƏQ GÖNDƏRİLİR!
    public ProductItem(String name, int quantity, double price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.kg = 0;
        this.liter = 0;
        this.isSelected = false;
        this.totalAmount = price * quantity;
    }

    // 3. Qəbz məhsulu üçün constructor
    public ProductItem(String name, double quantity, double price, double total) {
        this.name = name;
        this.price = price;
        this.totalAmount = total;
        this.isSelected = false;

        // Quantity tipinə görə təyin et
        if (name.contains("KG") || name.toLowerCase().contains("kq") || quantity != Math.floor(quantity)) {
            this.kg = quantity;
            this.quantity = 0;
            this.liter = 0;
        } else {
            this.quantity = (int) Math.round(quantity);
            this.kg = 0;
            this.liter = 0;
        }
    }

    // 4. Tam constructor
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
        this.isSelected = false;
        this.totalAmount = calculateTotal();
    }

    // Ümumi məbləği hesabla
    private double calculateTotal() {
        double total = 0;
        if (kg > 0) {
            total = kg * price;
        } else if (liter > 0) {
            total = liter * price;
        } else {
            total = quantity * price;
        }
        return total;
    }

    // Getter və Setter metodları
    public String getId() {
        return id;
    }

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
        this.totalAmount = calculateTotal();
    }

    public double getKg() {
        return kg;
    }

    public void setKg(double kg) {
        this.kg = kg;
        if (kg > 0) {
            this.quantity = 0;
            this.liter = 0;
        }
        this.totalAmount = calculateTotal();
    }

    public double getLiter() {
        return liter;
    }

    public void setLiter(double liter) {
        this.liter = liter;
        if (liter > 0) {
            this.kg = 0;
            this.quantity = 0;
        }
        this.totalAmount = calculateTotal();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        if (quantity > 0) {
            this.kg = 0;
            this.liter = 0;
        }
        this.totalAmount = calculateTotal();
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

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Date getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Date purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public double getTotalAmount() {
        double total = 0;
        if (kg > 0) {
            total = kg * price;
        } else if (liter > 0) {
            total = liter * price;
        } else {
            total = quantity * price;
        }
        return total;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public boolean isTaxFree() {
        return isTaxFree;
    }

    public void setTaxFree(boolean taxFree) {
        isTaxFree = taxFree;
    }

    public String getFiscalCode() {
        return fiscalCode;
    }

    public void setFiscalCode(String fiscalCode) {
        this.fiscalCode = fiscalCode;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    // Display üçün köməkçi metodlar
    public String getDisplayName() {
        String display = name != null ? name : "Məhsul";
        if (kg > 0) {
            display += " (" + String.format(Locale.getDefault(), "%.3f", kg) + " kq)";
        } else if (liter > 0) {
            display += " (" + String.format(Locale.getDefault(), "%.3f", liter) + " L)";
        } else if (quantity > 0) {
            display += " (" + quantity + " ədəd)";
        }
        return display;
    }

    public String getFormattedPrice() {
        return String.format(Locale.getDefault(), "%.2f AZN", price);
    }

    public String getFormattedTotal() {
        return String.format(Locale.getDefault(), "%.2f AZN", getTotalAmount());
    }

    public String getFormattedKg() {
        if (kg > 0) {
            return String.format(Locale.getDefault(), "%.3f kq", kg);
        }
        return "";
    }

    // Məhsul tipini qaytar
    public String getProductType() {
        if (kg > 0) return "Çəki";
        if (liter > 0) return "Maye";
        return "Ədəd";
    }

    @Override
    public String toString() {
        return getDisplayName() + " - " + getFormattedTotal();
    }

    // Firebase Firestore üçün map-ə çevir
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("category", category);
        map.put("price", price);
        map.put("kg", kg);
        map.put("liter", liter);
        map.put("quantity", quantity);
        map.put("userId", userId);
        map.put("createdAt", createdAt != null ? createdAt : Timestamp.now());
        map.put("receiptId", receiptId);
        map.put("storeName", storeName);
        map.put("purchaseDate", purchaseDate);
        map.put("taxAmount", taxAmount);
        map.put("isTaxFree", isTaxFree);
        map.put("fiscalCode", fiscalCode);
        map.put("barcode", barcode);
        return map;
    }
}