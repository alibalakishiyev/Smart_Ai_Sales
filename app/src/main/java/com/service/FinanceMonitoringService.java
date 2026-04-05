package com.service;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.DiscountMarket.service.PriceComparisonService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.main.MainActivity;
import com.model.FinanceMLModel;
import com.smart_ai_sales.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FinanceMonitoringService extends Service {
    private static final String TAG = "FinanceService";
    private static final String CHANNEL_ID = "finance_analytics_channel";
    private static final int NOTIFICATION_ID = 1001;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FinanceMLModel mlModel;
    private Handler handler;
    private ExecutorService executorService;
    private Runnable monitoringRunnable;
    private String userId;

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
        monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "🔄 Analiz edilir...");
                fetchAndAnalyze();
                handler.postDelayed(this, 20000); // HƏR 1 DƏQİQƏ
            }
        };
        handler.post(monitoringRunnable);
    }

    private void fetchAndAnalyze() {
        if (userId == null) {
            Log.e(TAG, "User not logged in");
            return;
        }

        executorService.execute(() -> {
            try {
                // Firebase-dən son 30 günün datalarını çək
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

                                // Modelə dataları əlavə et
                                mlModel.addTransactions(transactions);

                                // Analiz et
                                FinanceMLModel.AnalysisResult result = mlModel.analyzeRealTime();

                                // Notification göndər
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

        checkDiscountsAndNotify(result);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content));

        // Notification-a klikləyəndə app açılsın
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

    private void checkDiscountsAndNotify(FinanceMLModel.AnalysisResult result) {
        // BazarStore endirimlərini yoxla
        PriceComparisonService comparisonService = new PriceComparisonService(this);

        comparisonService.comparePrices(new PriceComparisonService.ComparisonCallback() {
            @Override
            public void onComparisonComplete(List<PriceComparisonService.ComparisonResult> results) {
                if (!results.isEmpty()) {
                    double totalSavings = 0;
                    for (PriceComparisonService.ComparisonResult r : results) {
                        totalSavings += r.getSavings();
                    }

                    // ƏLAVƏ BİLDİRİŞ GÖNDƏR
                    sendDiscountAlert(results, totalSavings);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Discount check error: " + error);
            }

            @Override
            public void onProgress(String message) {
                // İsteğe bağlı
            }
        });
    }

    private void sendDiscountAlert(List<PriceComparisonService.ComparisonResult> results, double totalSavings) {
        String title = String.format("🛍️ %d məhsul ENDİRİMDƏ!", results.size());
        String content = String.format("BazarStore-da %d məhsulunuz UCUZ! Ümumi qənaət: %.2f AZN",
                results.size(), totalSavings);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_discount)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500});

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID + 1, builder.build());
        }
    }

    private String generateContent(FinanceMLModel.AnalysisResult result) {
        StringBuilder sb = new StringBuilder();

        // Bugünkü xərc
        sb.append(String.format("💰 Bugün: %.2f AZN\n", result.todaySpent));

        // Həftəlik xərc
        sb.append(String.format("📈 Həftəlik: %.2f AZN\n", result.weekSpent));

        // Trend
        sb.append(String.format("📊 Trend: %s\n", result.trend));

        // Risk
        if (result.overspendRisk > 0.7) {
            sb.append("⚠️ RİSK: Çox yüksək!\n");
        } else if (result.overspendRisk > 0.4) {
            sb.append("⚡ RİSK: Orta səviyyədə\n");
        }

        // Ən çox xərc edilən mağaza
        if (!result.topStore.equals("Məlumat yoxdur")) {
            sb.append(String.format("🏪 Top mağaza: %s\n", result.topStore));
        }

        // 3 günlük proqnoz
        if (result.forecast3d > 0) {
            sb.append(String.format("🔮 3 günlük proqnoz: %.2f AZN\n", result.forecast3d));
        }

        // Ehtiyaclar
        if (!result.missingEssentials.isEmpty()) {
            sb.append("🛒 Ehtiyac: ");
            int count = Math.min(3, result.missingEssentials.size());
            for (int i = 0; i < count; i++) {
                sb.append(result.missingEssentials.get(i));
                if (i < count - 1) sb.append(", ");
            }
            sb.append("\n");
        }

        // Mağaza müqayisəsi (əgər varsa)
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
            channel.setDescription("Hər 1 dəqiqədən bir maliyyə analizi");
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
        if (handler != null && monitoringRunnable != null) {
            handler.removeCallbacks(monitoringRunnable);
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
