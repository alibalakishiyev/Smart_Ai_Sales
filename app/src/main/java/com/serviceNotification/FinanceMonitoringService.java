package com.serviceNotification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.DiscountMarket.service.PriceComparisonService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.main.MainActivity;
import com.model.FinanceMLModel;
import com.smart_ai_sales.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinanceMonitoringService extends Service {
    private static final String TAG = "FinanceService";
    private static final String CHANNEL_ID = "finance_analytics_channel";
    private static final int NOTIFICATION_ID = 1001;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FinanceMLModel mlModel;
    private Handler handler;
    private ExecutorService executorService;
    private Runnable financeMonitoringRunnable;
    private Runnable discountMonitoringRunnable;
    private String userId;

    private Random random = new Random();

    // Keçmiş məhsulları cache-ləmək üçün
    private List<String> cachedProducts = new ArrayList<>();
    private long lastProductFetchTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("🔄 Analiz başladı", "Məlumatlar yoxlanılır..."));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mlModel = new FinanceMLModel(this);
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        startMonitoring();
    }

    private void startMonitoring() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean financeEnabled = prefs.getBoolean("notification_finance", true);
        boolean discountsEnabled = prefs.getBoolean("notification_discounts", true);

        // 1. Maliyyə monitorinqi (aktivdirsə)
        if (financeEnabled) {
            financeMonitoringRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "🔄 Maliyyə analizi edilir...");
                    fetchAndAnalyze();
                    handler.postDelayed(this, 60 * 60 * 1000);
                }
            };
            handler.post(financeMonitoringRunnable);
        }

        // 2. Endirim monitorinqi (aktivdirsə)
        if (discountsEnabled) {
            discountMonitoringRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "🛍️ Endirim yoxlaması edilir...");
                    checkDiscountsAndNotify();
                    handler.postDelayed(discountMonitoringRunnable, 20 * 60 * 1000);
                }
            };
            handler.post(discountMonitoringRunnable);
        }
    }

    private void fetchAndAnalyze() {
        if (userId == null) {
            Log.e(TAG, "User not logged in");
            return;
        }

        executorService.execute(() -> {
            try {
                long thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;

                db.collection("transactions")
                        .whereEqualTo("userId", userId)
                        .whereGreaterThan("timestamp", thirtyDaysAgo)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(100)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                List<FinanceMLModel.Transaction> transactions = new ArrayList<>();

                                for (QueryDocumentSnapshot doc : task.getResult()) {
                                    FinanceMLModel.Transaction tx = new FinanceMLModel.Transaction();
                                    tx.id = doc.getId();
                                    tx.productName = doc.getString("productName");
                                    tx.storeName = doc.getString("storeName");
                                    tx.category = doc.getString("category");
                                    tx.amount = getDouble(doc, "amount");
                                    tx.total = getDouble(doc, "productTotal");
                                    tx.balanceBefore = getDouble(doc, "balanceBefore");
                                    tx.balanceAfter = getDouble(doc, "balanceAfter");
                                    tx.type = doc.getString("type");

                                    Object timestamp = doc.get("timestamp");
                                    if (timestamp instanceof com.google.firebase.Timestamp) {
                                        tx.timestamp = ((com.google.firebase.Timestamp) timestamp).toDate().getTime();
                                    } else if (timestamp instanceof Long) {
                                        tx.timestamp = (Long) timestamp;
                                    } else {
                                        tx.timestamp = System.currentTimeMillis();
                                    }

                                    transactions.add(tx);
                                }

                                Log.d(TAG, "✅ " + transactions.size() + " transaction yükləndi");

                                mlModel.addTransactions(transactions);
                                FinanceMLModel.AnalysisResult result = mlModel.analyzeRealTime();
                                sendNotification(result);

                            } else {
                                Log.e(TAG, "Firebase error", task.getException());
                            }
                        });

            } catch (Exception e) {
                Log.e(TAG, "Analysis error", e);
            }
        });
    }

    private double getDouble(QueryDocumentSnapshot doc, String field) {
        Double value = doc.getDouble(field);
        return value != null ? value : 0.0;
    }

    private void sendNotification(FinanceMLModel.AnalysisResult result) {
        String title = generateTitle(result);
        String content = generateContent(result);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content));

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private String generateTitle(FinanceMLModel.AnalysisResult result) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = timeFormat.format(new Date());

        if (result.overspendRisk > 0.7) {
            return "⚠️ YÜKSƏK RİSK! " + time;
        } else if (result.overspendRisk > 0.4) {
            return "⚡ Diqqət! Xərc artımı " + time;
        } else if (result.todaySpent > 100) {
            return "💰 Bugün çox xərc! " + time;
        } else if (!result.missingEssentials.isEmpty()) {
            return "🛒 Ehtiyaclarınız var " + time;
        }
        return "📊 Maliyyə Analizi " + time;
    }

    private void checkDiscountsAndNotify() {
        // Əvvəlcə keçmiş məhsulları yenilə (hər 2 saatda bir)
        if (cachedProducts.isEmpty() || (System.currentTimeMillis() - lastProductFetchTime > 2 * 60 * 60 * 1000)) {
            fetchProductsFromHistory();
        }

        PriceComparisonService comparisonService = new PriceComparisonService(this);

        // RANDOM seçim - ya məhsul adı, ya da kateqoriya üzrə
        boolean searchByProductName = random.nextBoolean();

        if (!cachedProducts.isEmpty() && searchByProductName) {
            // RANDOM olaraq 1-3 məhsul seç
            int productCount = random.nextInt(Math.min(3, cachedProducts.size())) + 1;
            List<String> selectedProducts = new ArrayList<>();
            List<String> tempProducts = new ArrayList<>(cachedProducts);
            java.util.Collections.shuffle(tempProducts);

            for (int i = 0; i < productCount; i++) {
                selectedProducts.add(tempProducts.get(i));
            }

            Log.d(TAG, "🔍 Məhsul adı ilə axtarılır: " + selectedProducts);
            checkDiscountsByProducts(comparisonService, selectedProducts);
        } else {
            // Kateqoriyaları random seç
            List<String> randomCategories = getRandomCategories();
            Log.d(TAG, "📁 Kateqoriya ilə axtarılır: " + randomCategories);
            checkDiscountsByCategories(comparisonService, randomCategories);
        }
    }

    private void fetchProductsFromHistory() {
        if (userId == null) return;

        try {
            long thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;

            db.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .whereGreaterThan("timestamp", thirtyDaysAgo)
                    .limit(50)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        cachedProducts.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String productName = doc.getString("productName");
                            if (productName != null && !productName.isEmpty() && !cachedProducts.contains(productName)) {
                                cachedProducts.add(productName);
                            }
                        }
                        lastProductFetchTime = System.currentTimeMillis();
                        Log.d(TAG, "✅ " + cachedProducts.size() + " məhsul tarixçədən yükləndi");

                        // Əgər tarixçədə məhsul yoxdursa, default məhsullar
                        if (cachedProducts.isEmpty()) {
                            cachedProducts.addAll(getDefaultProducts());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Məhsul tarixçəsi yüklənərkən xəta", e);
                        if (cachedProducts.isEmpty()) {
                            cachedProducts.addAll(getDefaultProducts());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error getting products", e);
            if (cachedProducts.isEmpty()) {
                cachedProducts.addAll(getDefaultProducts());
            }
        }
    }

    private List<String> getDefaultProducts() {
        List<String> defaults = new ArrayList<>();
        defaults.add("Süd");
        defaults.add("Çörək");
        defaults.add("Yumurta");
        defaults.add("Düyü");
        defaults.add("Yağ");
        defaults.add("Şəkər");
        defaults.add("Un");
        defaults.add("Makaron");
        defaults.add("Pendir");
        defaults.add("Kərə yağı");
        return defaults;
    }

    private List<String> getRandomCategories() {
        String[] categories = {
                "Qida Məhsulları", "Süd Məhsulları", "Ət və Toyuq", "Tərəvəz", "Meyvə",
                "İçkilər", "Qəlyanaltılar", "Təmizlik Məhsulları", "Şəxsi Qulluq",
                "Dondurulmuş Qidalar", "Konservlər", "Un Məmulatları"
        };

        List<String> selected = new ArrayList<>();
        int categoryCount = random.nextInt(2) + 1; // 1-2 kateqoriya

        List<String> categoryList = new ArrayList<>(java.util.Arrays.asList(categories));
        java.util.Collections.shuffle(categoryList);

        for (int i = 0; i < Math.min(categoryCount, categoryList.size()); i++) {
            selected.add(categoryList.get(i));
        }

        return selected;
    }

    private void checkDiscountsByProducts(PriceComparisonService service, List<String> products) {
        service.comparePrices(new PriceComparisonService.ComparisonCallback() {
            @Override
            public void onComparisonComplete(List<PriceComparisonService.ComparisonResult> results) {
                // Yalnız seçilmiş məhsullara uyğun nəticələri filtrlə
                List<PriceComparisonService.ComparisonResult> filteredResults = new ArrayList<>();
                for (PriceComparisonService.ComparisonResult result : results) {
                    String resultProductName = result.getLocalProduct() != null ?
                            result.getLocalProduct().getName().toLowerCase() : "";

                    for (String product : products) {
                        if (resultProductName.contains(product.toLowerCase()) ||
                                product.toLowerCase().contains(resultProductName)) {
                            filteredResults.add(result);
                            break;
                        }
                    }
                }

                if (!filteredResults.isEmpty()) {
                    double totalSavings = 0;
                    for (PriceComparisonService.ComparisonResult r : filteredResults) {
                        totalSavings += r.getSavings();
                    }
                    sendDiscountAlert(filteredResults, totalSavings, "məhsul");
                } else {
                    Log.d(TAG, "Seçilmiş məhsullar üçün endirim tapılmadı");
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Discount check error: " + error);
            }

            @Override
            public void onProgress(String message) {
                Log.d(TAG, "Progress: " + message);
            }
        });
    }

    private void checkDiscountsByCategories(PriceComparisonService service, List<String> categories) {
        service.comparePrices(new PriceComparisonService.ComparisonCallback() {
            @Override
            public void onComparisonComplete(List<PriceComparisonService.ComparisonResult> results) {
                // Yalnız seçilmiş kateqoriyalara uyğun nəticələri filtrlə
                List<PriceComparisonService.ComparisonResult> filteredResults = new ArrayList<>();
                for (PriceComparisonService.ComparisonResult result : results) {
                    String resultCategory = result.getLocalProduct() != null ?
                            result.getLocalProduct().getCategory() : "";

                    for (String category : categories) {
                        if (resultCategory != null && resultCategory.toLowerCase().contains(category.toLowerCase())) {
                            filteredResults.add(result);
                            break;
                        }
                    }
                }

                if (!filteredResults.isEmpty()) {
                    double totalSavings = 0;
                    for (PriceComparisonService.ComparisonResult r : filteredResults) {
                        totalSavings += r.getSavings();
                    }
                    sendDiscountAlert(filteredResults, totalSavings, "kateqoriya");
                } else {
                    Log.d(TAG, "Seçilmiş kateqoriyalar üçün endirim tapılmadı");
                    // Əgər heç nə tapılmadısa, bütün endirimləri göstər
                    if (!results.isEmpty()) {
                        sendDiscountAlert(results.subList(0, Math.min(3, results.size())),
                                calculateTotalSavings(results), "bütün");
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Discount check error: " + error);
            }

            @Override
            public void onProgress(String message) {
                Log.d(TAG, "Progress: " + message);
            }
        });
    }

    private double calculateTotalSavings(List<PriceComparisonService.ComparisonResult> results) {
        double total = 0;
        for (PriceComparisonService.ComparisonResult r : results) {
            total += r.getSavings();
        }
        return total;
    }

    private void sendDiscountAlert(List<PriceComparisonService.ComparisonResult> results, double totalSavings, String searchType) {
        // Məhsul adlarını göstər
        StringBuilder productNames = new StringBuilder();
        for (int i = 0; i < Math.min(3, results.size()); i++) {
            if (i > 0) productNames.append(", ");
            String productName = results.get(i).getLocalProduct() != null ?
                    results.get(i).getLocalProduct().getName() : "Məhsul";
            productNames.append(productName);
        }

        String searchTypeText = "";
        switch (searchType) {
            case "məhsul":
                searchTypeText = "Məhsul adı";
                break;
            case "kateqoriya":
                searchTypeText = "Kateqoriya";
                break;
            default:
                searchTypeText = "Bütün endirimlər";
        }

        String title = String.format("🛍️ %d məhsul ENDİRİMDƏ!", results.size());
        String content = String.format("📦 %s\n💸 Ümumi qənaət: %.2f AZN\n🔍 Axtarış: %s",
                productNames.toString(), totalSavings, searchTypeText);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_discount)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500});

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID + random.nextInt(1000), builder.build());
        }
    }

    private String generateContent(FinanceMLModel.AnalysisResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("💰 Bugün: %.2f AZN\n", result.todaySpent));
        sb.append(String.format("📈 Həftəlik: %.2f AZN\n", result.weekSpent));
        sb.append(String.format("📊 Trend: %s\n", result.trend));

        if (result.overspendRisk > 0.7) {
            sb.append("⚠️ RİSK: Çox yüksək!\n");
        } else if (result.overspendRisk > 0.4) {
            sb.append("⚡ RİSK: Orta səviyyədə\n");
        }

        if (!result.topStore.equals("Məlumat yoxdur")) {
            sb.append(String.format("🏪 Top mağaza: %s\n", result.topStore));
        }

        if (result.forecast3d > 0) {
            sb.append(String.format("🔮 3 günlük proqnoz: %.2f AZN\n", result.forecast3d));
        }

        if (!result.missingEssentials.isEmpty()) {
            sb.append("🛒 Ehtiyac: ");
            int count = Math.min(3, result.missingEssentials.size());
            for (int i = 0; i < count; i++) {
                sb.append(result.missingEssentials.get(i));
                if (i < count - 1) sb.append(", ");
            }
            sb.append("\n");
        }

        if (!result.storeComparisons.isEmpty()) {
            FinanceMLModel.StoreComparison first = result.storeComparisons.get(0);
            sb.append(String.format("💡 %s ən ucuz: %s (%.2f AZN)\n",
                    first.product, first.cheapestStore, first.cheapestPrice));
        }

        return sb.toString().trim();
    }

    private android.app.Notification createNotification(String title, String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Maliyyə Analizi",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Hər 1 saatdan bir maliyyə analizi, hər 20 dəqiqədən bir endirim yoxlaması");
            channel.enableVibration(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            if (financeMonitoringRunnable != null) {
                handler.removeCallbacks(financeMonitoringRunnable);
            }
            if (discountMonitoringRunnable != null) {
                handler.removeCallbacks(discountMonitoringRunnable);
            }
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        if (mlModel != null) {
            mlModel.close();
        }
        Log.d(TAG, "Service destroyed");
    }
}