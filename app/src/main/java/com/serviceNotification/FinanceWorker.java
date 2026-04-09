package com.serviceNotification;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FinanceWorker extends Worker {

    private static final String TAG = "FinanceWorker";
    private final Random random = new Random();

    private static final String[] ALL_CATEGORIES = {
            "Qida Məhsulları", "Süd Məhsulları", "Ət və Toyuq",
            "Tərəvəz", "Meyvə", "İçkilər", "Qəlyanaltılar",
            "Təmizlik Məhsulları", "Şəxsi Qulluq",
            "Dondurulmuş Qidalar", "Konservlər", "Un Məmulatları"
    };

    private static final String[] DEFAULT_PRODUCTS = {
            "Süd", "Çörək", "Yumurta", "Düyü", "Yağ",
            "Şəkər", "Un", "Makaron", "Pendir", "Kərə yağı"
    };

    public FinanceWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "=== FinanceWorker başladı ===");

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "İstifadəçi daxil olmayıb, worker dayandı.");
            return Result.success();
        }

        try {
            // 1. Firebase-dən son 30 günün məhsullarını çək
            List<String> firebaseProducts = fetchProductsFromFirebase(
                    auth.getCurrentUser().getUid());

            // 2. Random məhsul/kateqoriya seç
            List<String> selectedItems = selectRandomItems(firebaseProducts);
            Log.d(TAG, "Seçilən məhsullar: " + selectedItems);

            // 3. Maliyyə analizini hesabla
            FinanceAnalysisResult analysis = analyzeFinances(
                    auth.getCurrentUser().getUid());

            // 4. Bildirişləri göndər
            FinanceNotificationHelper helper =
                    new FinanceNotificationHelper(getApplicationContext());

            // Endirim/məhsul bildirişi — həmişə göndər
            helper.sendProductAnalysisNotification(selectedItems, analysis);

            // Risk yüksəkdirsə — əlavə xəbərdarlıq
            if (analysis.riskLevel > 0.65f) {
                helper.sendRiskWarningNotification(analysis);
            }

            // Qənaət imkanı varsa — motivasiya bildirişi
            if (analysis.savingsRate > 20) {
                helper.sendSavingsMotivationNotification(analysis);
            }

            Log.d(TAG, "=== FinanceWorker uğurla tamamlandı ===");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "FinanceWorker xətası", e);
            return Result.retry();
        }
    }

    // -------------------------------------------------------
    // Firebase-dən məhsul adlarını çək
    // -------------------------------------------------------
    private List<String> fetchProductsFromFirebase(String userId) {
        List<String> products = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        long thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;

        FirebaseFirestore.getInstance()
                .collection("transactions")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("timestamp", thirtyDaysAgo)
                .limit(60)
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String name = doc.getString("productName");
                        if (name != null && !name.trim().isEmpty()
                                && !products.contains(name)) {
                            products.add(name.trim());
                        }
                    }
                    Log.d(TAG, "Firebase-dən " + products.size() + " məhsul gəldi.");
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase məhsul fetch xətası", e);
                    latch.countDown();
                });

        try {
            latch.await(12, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return products.isEmpty() ? getDefaultProducts() : products;
    }

    // -------------------------------------------------------
    // Random məhsul/kateqoriya seç
    // -------------------------------------------------------
    private List<String> selectRandomItems(List<String> firebaseProducts) {
        // 50% ehtimalla firebase məhsulları, 50% kateqoriyalar
        boolean useFirebaseProducts = !firebaseProducts.isEmpty() && random.nextBoolean();

        List<String> pool;
        if (useFirebaseProducts) {
            pool = new ArrayList<>(firebaseProducts);
        } else {
            pool = new ArrayList<>(Arrays.asList(ALL_CATEGORIES));
        }

        Collections.shuffle(pool);
        int count = random.nextInt(Math.min(4, pool.size())) + 1; // 1-4 arası
        return new ArrayList<>(pool.subList(0, count));
    }

    // -------------------------------------------------------
    // Maliyyə analizini Firebase-dən hesabla
    // -------------------------------------------------------
    public FinanceAnalysisResult analyzeFinances(String userId) {
        FinanceAnalysisResult result = new FinanceAnalysisResult();
        CountDownLatch latch = new CountDownLatch(1);

        long now = System.currentTimeMillis();
        long weekAgo     = now - 7L  * 24 * 60 * 60 * 1000;
        long twoWeeksAgo = now - 14L * 24 * 60 * 60 * 1000;
        long monthAgo    = now - 30L * 24 * 60 * 60 * 1000;

        FirebaseFirestore.getInstance()
                .collection("transactions")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("timestamp", monthAgo)
                .get()
                .addOnSuccessListener(snapshots -> {
                    double lastWeekExp  = 0, prevWeekExp = 0;
                    double monthIncome  = 0, monthExpense = 0;
                    Map<String, Double> categoryMap = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String type   = doc.getString("type");
                        Double amount = doc.getDouble("amount");
                        String cat    = doc.getString("category");

                        Object tsObj = doc.get("timestamp");
                        long ts = 0;
                        if (tsObj instanceof com.google.firebase.Timestamp) {
                            ts = ((com.google.firebase.Timestamp) tsObj)
                                    .toDate().getTime();
                        } else if (tsObj instanceof Long) {
                            ts = (Long) tsObj;
                        }

                        if (amount == null || type == null) continue;

                        if ("expense".equals(type)) {
                            monthExpense += amount;
                            if (ts >= weekAgo)          lastWeekExp  += amount;
                            else if (ts >= twoWeeksAgo) prevWeekExp  += amount;
                            if (cat != null) {
                                categoryMap.put(cat,
                                        categoryMap.getOrDefault(cat, 0.0) + amount);
                            }
                        } else if ("income".equals(type)) {
                            monthIncome += amount;
                        }
                    }

                    result.totalIncome    = monthIncome;
                    result.totalExpense   = monthExpense;
                    result.lastWeekExpense = lastWeekExp;
                    result.prevWeekExpense = prevWeekExp;

                    // Qənaət dərəcəsi
                    if (monthIncome > 0) {
                        result.savingsRate =
                                ((monthIncome - monthExpense) / monthIncome) * 100;
                    }

                    // Risk hesabla
                    if (prevWeekExp > 0) {
                        double ratio = lastWeekExp / prevWeekExp;
                        if      (ratio > 1.4) result.riskLevel = 0.90f;
                        else if (ratio > 1.2) result.riskLevel = 0.70f;
                        else if (ratio > 1.0) result.riskLevel = 0.50f;
                        else if (ratio > 0.8) result.riskLevel = 0.30f;
                        else                  result.riskLevel = 0.15f;
                    }

                    // Trend mətni
                    if      (lastWeekExp > prevWeekExp * 1.20) result.trend = "Sürətli artım ⚠️";
                    else if (lastWeekExp > prevWeekExp * 1.05) result.trend = "Yavaş artım 📈";
                    else if (lastWeekExp < prevWeekExp * 0.80) result.trend = "Sürətli azalma ✅";
                    else if (lastWeekExp < prevWeekExp * 0.95) result.trend = "Yavaş azalma 📉";
                    else                                        result.trend = "Stabil ➡️";

                    // Ən çox xərc kateqoriyası
                    String topCat    = "";
                    double topAmount = 0;
                    for (Map.Entry<String, Double> e : categoryMap.entrySet()) {
                        if (e.getValue() > topAmount) {
                            topAmount = e.getValue();
                            topCat    = e.getKey();
                        }
                    }
                    result.topCategory       = topCat;
                    result.topCategoryAmount = topAmount;

                    // 3 günlük proqnoz
                    double dailyAvg      = monthExpense / 30.0;
                    result.forecast3d    = dailyAvg * 3;
                    result.savingsPotential = dailyAvg * 0.20 * 3; // 20% azaltma potensialı

                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Analiz Firebase xətası", e);
                    latch.countDown();
                });

        try {
            latch.await(12, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return result;
    }

    private List<String> getDefaultProducts() {
        return new ArrayList<>(Arrays.asList(DEFAULT_PRODUCTS));
    }

    // -------------------------------------------------------
    // Data sinfləri
    // -------------------------------------------------------
    public static class FinanceAnalysisResult {
        public double totalIncome      = 0;
        public double totalExpense     = 0;
        public double lastWeekExpense  = 0;
        public double prevWeekExpense  = 0;
        public double savingsRate      = 0;
        public double forecast3d       = 0;
        public double savingsPotential = 0;
        public float  riskLevel        = 0.3f;
        public String trend            = "Stabil ➡️";
        public String topCategory      = "";
        public double topCategoryAmount = 0;
    }
}
