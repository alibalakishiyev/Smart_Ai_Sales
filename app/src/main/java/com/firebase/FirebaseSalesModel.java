package com.firebase;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.data.SalesData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseSalesModel {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    public FirebaseSalesModel() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }
    }

    public interface SalesDataCallback {
        void onSuccess(List<SalesData> salesDataList);
        void onError(String error);
    }

    public interface TotalProfitCallback {
        void onSuccess(double totalProfit);
        void onError(String error);
    }

    public interface AddDataCallback {
        void onSuccess();
        void onError(String error);
    }

    // Yeni satış məlumatı əlavə et
    public void addSalesData(SalesData data, AddDataCallback callback) {
        if (userId == null) {
            callback.onError("İstifadəçi tapılmadı");
            return;
        }

        Map<String, Object> salesMap = new HashMap<>();
        salesMap.put("date", data.getDate().getTime());
        salesMap.put("sales", data.getSales());
        salesMap.put("expenses", data.getExpenses());
        salesMap.put("profit", data.getProfit());
        salesMap.put("customerCount", data.getCustomerCount());
        salesMap.put("avgPrice", data.getAvgPrice());
        salesMap.put("userId", userId);
        salesMap.put("timestamp", System.currentTimeMillis());

        db.collection("sales")
                .add(salesMap)
                .addOnSuccessListener(documentReference -> {
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    // Son 10 günün məlumatlarını getir
    public void getLast10DaysData(SalesDataCallback callback) {
        if (userId == null) {
            callback.onError("İstifadəçi tapılmadı");
            return;
        }

        long tenDaysAgo = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000);

        db.collection("sales")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("date", tenDaysAgo)
                .orderBy("date")
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<SalesData> dataList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            SalesData data = new SalesData(
                                    new java.util.Date(document.getLong("date")),
                                    document.getDouble("sales"),
                                    document.getDouble("expenses"),
                                    document.getDouble("profit"),
                                    document.getLong("customerCount").intValue(),
                                    document.getDouble("avgPrice")
                            );
                            dataList.add(data);
                        }
                        callback.onSuccess(dataList);
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    // Ümumi gəliri hesabla
    public void getTotalProfit(TotalProfitCallback callback) {
        if (userId == null) {
            callback.onError("İstifadəçi tapılmadı");
            return;
        }

        db.collection("sales")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double total = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            total += document.getDouble("profit");
                        }
                        callback.onSuccess(total);
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    // Həftəlik gəliri hesabla
    public void getWeeklyProfit(TotalProfitCallback callback) {
        if (userId == null) {
            callback.onError("İstifadəçi tapılmadı");
            return;
        }

        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);

        db.collection("sales")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("date", sevenDaysAgo)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double total = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            total += document.getDouble("profit");
                        }
                        callback.onSuccess(total);
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }
}