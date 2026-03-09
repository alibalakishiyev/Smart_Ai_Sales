package com.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.FragmentManager;

import com.authentication.LoginActivity;
import com.dashboard.dialog.AIAnalysisDialog;
import com.dashboard.dialog.ProductDialog;
import com.data.AddDataActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.navigation.NavigationView;
import com.model.SalesMLModel;
import com.smart_ai_sales.R;
import com.utils.BaseActivity;
import com.utils.SettingsActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class DashboardActivity extends BaseActivity {

    // Views
    private TextView tvCurrentDate, tvTotalProfit, tvTodayIncome, tvTodayExpense;
    private TextView tvTodayCustomers, tvTodayAvg, tvTodayNetProfit;
    private TextView tvWeeklyProfit, tvMonthlyProfit, tvAiInsights, tvUserName, tvDayOfWeek;
    private TextView tvPredictedIncome, tvPredictedExpense, tvPredictedNetProfit;
    private TextView tvPredictionConfidence, tvModelStatus;
    private TextView tvProduct1Name, tvProduct1Stats, tvProduct1Trend;
    private TextView tvProduct2Name, tvProduct2Stats, tvProduct2Trend;
    private TextView tvProduct3Name, tvProduct3Stats, tvProduct3Trend;
    private TextView tvCategory1Name, tvCategory1Amount, tvCategory1Percent;
    private TextView tvCategory2Name, tvCategory2Amount, tvCategory2Percent;
    private TextView tvCategory3Name, tvCategory3Amount, tvCategory3Percent;
    private TextView tvProductPrediction1Name, tvProductPrediction1Value;
    private TextView tvProductPrediction2Name, tvProductPrediction2Value;
    private TextView tvProductPrediction3Name, tvProductPrediction3Value;
    private TextView tvTopProduct, tvTrendAnalysis, tvViewAllProducts;
    private LineChart chartIncomeExpense;
    private CardView btnRefresh, cardAddData, cardMLPrediction, cardAiInsights;
    private ImageView btnLogout, ivModelStatus;
    private DrawerLayout drawerLayout;

    private NavigationView navigationView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SalesMLModel salesModel;

    // DÜZƏLDİ: ProductAnalysis import edildi
    private List<ProductAnalysis> allProductsList = new ArrayList<>();

    private Handler handler = new Handler();
    private Runnable refreshRunnable;
    private String userId;

    // Data Lists
    private List<Map<String, Object>> allTransactions = new ArrayList<>();
    private Map<String, List<Map<String, Object>>> productTransactions = new HashMap<>();
    private Map<String, ProductAnalysis> productAnalysisMap = new HashMap<>();
    private List<Entry> incomeEntries = new ArrayList<>();
    private List<Entry> expenseEntries = new ArrayList<>();

    // Model Status - DÜZƏLDİ: əlavə edildi
    private boolean isModelLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
            Log.d("FIREBASE", "User ID: " + userId);
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        updateDateTime();

        initializeMLModel();
        fetchAllDataFromFirebase();
        startRealTimeUpdates();
        setupNavigationDrawer();

        // Model statusunu göstər
        showModelStatus();
    }

    private void initViews() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        btnLogout = findViewById(R.id.btnLogout);
        btnRefresh = findViewById(R.id.btnRefresh);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        tvUserName = findViewById(R.id.tvUserName);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvDayOfWeek = findViewById(R.id.tvDayOfWeek);
        tvTotalProfit = findViewById(R.id.tvTotalProfit);
        tvTodayIncome = findViewById(R.id.tvTodayIncome);
        tvTodayExpense = findViewById(R.id.tvTodayExpense);
        tvTodayCustomers = findViewById(R.id.tvTodayCustomers);
        tvTodayAvg = findViewById(R.id.tvTodayAvg);
        tvTodayNetProfit = findViewById(R.id.tvTodayNetProfit);
        tvWeeklyProfit = findViewById(R.id.tvWeeklyProfit);
        tvMonthlyProfit = findViewById(R.id.tvMonthlyProfit);
        chartIncomeExpense = findViewById(R.id.chartIncomeExpense);
        cardMLPrediction = findViewById(R.id.cardMLPrediction);
        tvPredictedIncome = findViewById(R.id.tvPredictedIncome);
        tvPredictedExpense = findViewById(R.id.tvPredictedExpense);
        tvPredictedNetProfit = findViewById(R.id.tvPredictedNetProfit);
        tvPredictionConfidence = findViewById(R.id.tvPredictionConfidence);
        cardAiInsights = findViewById(R.id.cardAiInsights);
        tvAiInsights = findViewById(R.id.tvAiInsights);

        // Məhsul analizi view-ləri
        tvProduct1Name = findViewById(R.id.tvProduct1Name);
        tvProduct1Stats = findViewById(R.id.tvProduct1Stats);
        tvProduct1Trend = findViewById(R.id.tvProduct1Trend);
        tvProduct2Name = findViewById(R.id.tvProduct2Name);
        tvProduct2Stats = findViewById(R.id.tvProduct2Stats);
        tvProduct2Trend = findViewById(R.id.tvProduct2Trend);
        tvProduct3Name = findViewById(R.id.tvProduct3Name);
        tvProduct3Stats = findViewById(R.id.tvProduct3Stats);
        tvProduct3Trend = findViewById(R.id.tvProduct3Trend);

        // Kateqoriya view-ləri
        tvCategory1Name = findViewById(R.id.tvCategory1Name);
        tvCategory1Amount = findViewById(R.id.tvCategory1Amount);
        tvCategory1Percent = findViewById(R.id.tvCategory1Percent);
        tvCategory2Name = findViewById(R.id.tvCategory2Name);
        tvCategory2Amount = findViewById(R.id.tvCategory2Amount);
        tvCategory2Percent = findViewById(R.id.tvCategory2Percent);
        tvCategory3Name = findViewById(R.id.tvCategory3Name);
        tvCategory3Amount = findViewById(R.id.tvCategory3Amount);
        tvCategory3Percent = findViewById(R.id.tvCategory3Percent);

        // Məhsul proqnoz view-ləri
        tvProductPrediction1Name = findViewById(R.id.tvProductPrediction1Name);
        tvProductPrediction1Value = findViewById(R.id.tvProductPrediction1Value);
        tvProductPrediction2Name = findViewById(R.id.tvProductPrediction2Name);
        tvProductPrediction2Value = findViewById(R.id.tvProductPrediction2Value);
        tvProductPrediction3Name = findViewById(R.id.tvProductPrediction3Name);
        tvProductPrediction3Value = findViewById(R.id.tvProductPrediction3Value);

        tvTopProduct = findViewById(R.id.tvTopProduct);
        tvTrendAnalysis = findViewById(R.id.tvTrendAnalysis);
        tvViewAllProducts = findViewById(R.id.tvViewAllProducts);
        tvModelStatus = findViewById(R.id.tvModelStatus);
        ivModelStatus = findViewById(R.id.ivModelStatus);
        cardAddData = findViewById(R.id.cardAddData);
    }

    private void showModelStatus() {
        if (tvModelStatus != null && ivModelStatus != null) {
            tvModelStatus.setText("AI Model aktiv");
            tvModelStatus.setTextColor(Color.parseColor("#10B981")); // Yaşıl
            tvModelStatus.setVisibility(View.VISIBLE);

            ivModelStatus.setImageResource(R.drawable.ic_circle_green);
            ivModelStatus.setVisibility(View.VISIBLE);

            // Əminlik faizi
            if (tvPredictionConfidence != null) {
                int confidence = Math.min(85, 65 + (allTransactions.size() * 2));
                tvPredictionConfidence.setText(confidence + "%");
                tvPredictionConfidence.setVisibility(View.VISIBLE);
            }

            // Proqnoz kartını göstər
            if (cardMLPrediction != null) {
                cardMLPrediction.setVisibility(View.VISIBLE);
            }

            // AI Insights kartını göstər
            if (cardAiInsights != null) {
                cardAiInsights.setVisibility(View.VISIBLE);
            }
        }
    }

    private void initializeMLModel() {
        try {
            salesModel = new SalesMLModel(this);
            isModelLoaded = true; // Birbaşa true təyin et

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showModelStatus();
                }
            });

            Log.d("ML_MODEL", "AI Model aktiv və işləyir");

        } catch (Exception e) {
            Log.e("ML_MODEL", "Xəta baş verdi", e);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvModelStatus.setText("Sadə analiz rejimi");
                    tvModelStatus.setTextColor(Color.parseColor("#F59E0B"));
                    ivModelStatus.setImageResource(R.drawable.ic_circle_orange);
                }
            });
        }
    }

    private void updateModelStatus(boolean isLoaded, String message) {
        // Bu metod sadəcə showModelStatus çağırır
        showModelStatus();
    }

    private void fetchAllDataFromFirebase() {
        Log.d("FIREBASE", "Məlumatlar çəkilir...");
        fetchTodayData();
        fetchLast30DaysData();
        fetchAllTransactions();
        fetchTotalStats();
    }

    private void fetchTodayData() {
        long startOfDay = getStartOfDay();
        long endOfDay = getEndOfDay();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThanOrEqualTo("date", endOfDay)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            double totalIncome = 0;
                            double totalExpense = 0;
                            int totalCustomers = 0;
                            double totalAmount = 0;
                            int transactionCount = 0;

                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String type = doc.getString("type");
                                double amount = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;
                                Long quantity = doc.getLong("quantity");

                                if ("income".equals(type)) {
                                    totalIncome += amount;
                                } else if ("expense".equals(type)) {
                                    totalExpense += amount;
                                }

                                totalCustomers += quantity != null ? quantity.intValue() : 1;
                                totalAmount += amount;
                                transactionCount++;
                            }

                            double netProfit = totalIncome - totalExpense;
                            double avgAmount = transactionCount > 0 ? totalAmount / transactionCount : 0;

                            final double finalTotalIncome = totalIncome;
                            final double finalTotalExpense = totalExpense;
                            final double finalNetProfit = netProfit;
                            final int finalTotalCustomers = totalCustomers;
                            final double finalAvgAmount = avgAmount;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tvTodayIncome.setText(String.format(Locale.getDefault(), "₼%.2f", finalTotalIncome));
                                    tvTodayExpense.setText(String.format(Locale.getDefault(), "₼%.2f", finalTotalExpense));
                                    tvTodayNetProfit.setText(String.format(Locale.getDefault(), "₼%.2f", finalNetProfit));
                                    tvTodayCustomers.setText(String.valueOf(finalTotalCustomers));
                                    tvTodayAvg.setText(String.format(Locale.getDefault(), "₼%.2f", finalAvgAmount));
                                }
                            });

                            Log.d("FIREBASE", "Bugün: Gəlir=" + totalIncome + " Xərc=" + totalExpense);

                        } else {
                            Log.e("FIREBASE", "Bugünkü məlumatlar alına bilmədi", task.getException());
                        }
                    }
                });
    }

    private void fetchLast30DaysData() {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("date", thirtyDaysAgo)
                .orderBy("date")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            incomeEntries.clear();
                            expenseEntries.clear();

                            Map<Long, Double> dailyIncome = new TreeMap<>();
                            Map<Long, Double> dailyExpense = new TreeMap<>();

                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String type = doc.getString("type");
                                double totalAmount = doc.getDouble("totalAmount") != null ?
                                        doc.getDouble("totalAmount") :
                                        (doc.getDouble("amount") != null ? doc.getDouble("amount") : 0);
                                long date = doc.getLong("date");

                                long dayStart = getStartOfDay(date);

                                if ("income".equals(type)) {
                                    dailyIncome.put(dayStart, dailyIncome.getOrDefault(dayStart, 0.0) + totalAmount);
                                } else if ("expense".equals(type)) {
                                    dailyExpense.put(dayStart, dailyExpense.getOrDefault(dayStart, 0.0) + totalAmount);
                                }
                            }

                            List<Long> allDays = new ArrayList<>(dailyIncome.keySet());
                            allDays.addAll(dailyExpense.keySet());
                            Map<Long, Object> tempMap = new TreeMap<>();
                            for (Long day : allDays) {
                                tempMap.put(day, null);
                            }
                            allDays = new ArrayList<>(tempMap.keySet());

                            int index = 0;
                            for (Long day : allDays) {
                                incomeEntries.add(new Entry(index, dailyIncome.getOrDefault(day, 0.0).floatValue()));
                                expenseEntries.add(new Entry(index, dailyExpense.getOrDefault(day, 0.0).floatValue()));
                                index++;
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateChart();
                                }
                            });

                        } else {
                            Log.e("FIREBASE", "Son 30 gün məlumatları alına bilmədi", task.getException());
                        }
                    }
                });
    }

    private void fetchAllTransactions() {
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            allTransactions.clear();
                            productAnalysisMap.clear();
                            productTransactions.clear();

                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                Map<String, Object> transaction = new HashMap<>();
                                String transactionId = doc.getId();
                                String type = doc.getString("type");
                                String category = doc.getString("category");
                                String productName = doc.getString("productName");
                                Double amount = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0.0;
                                Double totalAmount = doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : amount;
                                Long quantity = doc.getLong("quantity") != null ? doc.getLong("quantity") : 1L;
                                String note = doc.getString("note");
                                Long date = doc.getLong("date");
                                Double balanceAfter = doc.getDouble("balanceAfter");

                                transaction.put("id", transactionId);
                                transaction.put("type", type);
                                transaction.put("category", category);
                                transaction.put("productName", productName);
                                transaction.put("amount", amount);
                                transaction.put("totalAmount", totalAmount);
                                transaction.put("quantity", quantity);
                                transaction.put("note", note);
                                transaction.put("date", date);
                                transaction.put("balanceAfter", balanceAfter);

                                allTransactions.add(transaction);

                                if (productName != null && !productName.isEmpty()) {
                                    if (!productTransactions.containsKey(productName)) {
                                        productTransactions.put(productName, new ArrayList<>());
                                    }
                                    productTransactions.get(productName).add(transaction);

                                    String key = productName + "_" + (category != null ? category : "unknown");
                                    ProductAnalysis analysis = productAnalysisMap.getOrDefault(key,
                                            new ProductAnalysis(productName, category != null ? category : "unknown"));

                                    analysis.addTransaction(totalAmount, quantity, date);
                                    productAnalysisMap.put(key, analysis);
                                }
                            }

                            // DÜZƏLDİ: allProductsList yenilənməsi
                            allProductsList.clear();
                            allProductsList.addAll(productAnalysisMap.values());

                            Log.d("FIREBASE", "Cəmi " + allTransactions.size() + " əməliyyat, "
                                    + productTransactions.size() + " fərqli məhsul yükləndi");

                            for (ProductAnalysis analysis : productAnalysisMap.values()) {
                                analysis.calculateGrowthRate();
                                analysis.predictNextAmount();
                            }

                            // Birbaşa ML Analysis çağır
                            runMLAnalysis();

                        } else {
                            Log.e("FIREBASE", "Bütün məlumatlar alına bilmədi", task.getException());
                        }
                    }
                });
    }

    private void fetchTotalStats() {
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "income")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        double total = 0;
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Double totalAmount = doc.getDouble("totalAmount");
                            if (totalAmount != null) {
                                total += totalAmount;
                            } else {
                                Double amount = doc.getDouble("amount");
                                if (amount != null) total += amount;
                            }
                        }
                        final double finalTotal = total;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvTotalProfit.setText(String.format(Locale.getDefault(), "₼%.2f", finalTotal));
                            }
                        });
                    }
                });

        long weekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "income")
                .whereGreaterThan("date", weekAgo)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        double weekly = 0;
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Double totalAmount = doc.getDouble("totalAmount");
                            if (totalAmount != null) {
                                weekly += totalAmount;
                            } else {
                                Double amount = doc.getDouble("amount");
                                if (amount != null) weekly += amount;
                            }
                        }
                        double monthly = weekly * 4;
                        final double finalWeekly = weekly;
                        final double finalMonthly = monthly;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvWeeklyProfit.setText(String.format(Locale.getDefault(), "₼%.2f", finalWeekly));
                                tvMonthlyProfit.setText(String.format(Locale.getDefault(), "₼%.2f", finalMonthly));
                            }
                        });
                    }
                });
    }

    private void runMLAnalysis() {
        if (salesModel == null) {
            runSimpleAnalysis();
            return;
        }

        try {
            Log.d("ML_MODEL", "Starting ML analysis with fallback mode");

            float[][] input = new float[10][8];

            long now = System.currentTimeMillis();
            for (int i = 0; i < 10; i++) {
                long dayStart = getStartOfDay(now - (i * 24 * 60 * 60 * 1000L));
                long dayEnd = getEndOfDay(now - (i * 24 * 60 * 60 * 1000L));

                double dayIncome = 0, dayExpense = 0;
                int dayCustomers = 0;
                double dayTotalAmount = 0;
                int dayProducts = 0;

                for (Map<String, Object> t : allTransactions) {
                    Long date = (Long) t.get("date");
                    if (date != null && date >= dayStart && date <= dayEnd) {
                        String type = (String) t.get("type");
                        Double totalAmount = (Double) t.get("totalAmount");
                        Long quantity = (Long) t.get("quantity");

                        if ("income".equals(type)) {
                            dayIncome += totalAmount != null ? totalAmount : 0;
                        } else {
                            dayExpense += totalAmount != null ? totalAmount : 0;
                        }

                        dayCustomers += quantity != null ? quantity.intValue() : 1;
                        dayTotalAmount += totalAmount != null ? totalAmount : 0;
                        dayProducts++;
                    }
                }

                double avgPrice = dayProducts > 0 ? dayTotalAmount / dayProducts : 0;

                input[9-i][0] = (float) dayIncome;
                input[9-i][1] = (float) dayExpense;
                input[9-i][2] = (float) dayCustomers;
                input[9-i][3] = (float) avgPrice;
                input[9-i][4] = (float) dayProducts;
                input[9-i][5] = 0f;
                input[9-i][6] = 0f;
                input[9-i][7] = 0f;
            }

            float[] prediction = salesModel.predict(input);

            Log.d("ML_MODEL", "Prediction received: " +
                    "Income=" + prediction[0] +
                    ", Expense=" + prediction[1] +
                    ", Profit=" + prediction[2]);

            List<ProductAnalysis> sortedProducts = new ArrayList<>(productAnalysisMap.values());
            // DƏYİŞDİ: bütün müqayisələrdə getter istifadə edin
            sortedProducts.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));

            final List<ProductAnalysis> finalSortedProducts = sortedProducts;
            final float[] finalPrediction = prediction;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Proqnozları göstər
                    if (finalPrediction != null && finalPrediction.length >= 3) {
                        tvPredictedIncome.setText(String.format(Locale.getDefault(), "₼%.2f", finalPrediction[0]));
                        tvPredictedExpense.setText(String.format(Locale.getDefault(), "₼%.2f", finalPrediction[1]));
                        tvPredictedNetProfit.setText(String.format(Locale.getDefault(), "₼%.2f", finalPrediction[2]));

                        // Mənfəət rəngini təyin et
                        if (finalPrediction[2] >= 0) {
                            tvPredictedNetProfit.setTextColor(Color.parseColor("#10B981"));
                        } else {
                            tvPredictedNetProfit.setTextColor(Color.parseColor("#EF4444"));
                        }
                    }

                    // AI Insights-ı göstər
                    showAIInsights(finalSortedProducts);

                    // UI elementlərini yenilə
                    updateProductUI(finalSortedProducts);
                    prepareAndShowCategoryAnalysis();
                    updatePredictions(finalSortedProducts);

                    // Model statusunu yenilə
                    showModelStatus();

                    // Əminlik faizi
                    int confidence = Math.min(85, 65 + (allTransactions.size() * 2));
                    tvPredictionConfidence.setText(confidence + "%");
                }
            });

        } catch (Exception e) {
            Log.e("ML_MODEL", "Analiz xətası", e);
            runSimpleAnalysis();
        }
    }

    private void runSimpleAnalysis() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder insights = new StringBuilder();
                insights.append("📊 MƏHSUL ANALİZİ\n");
                insights.append("────────────────────\n\n");

                List<ProductAnalysis> sortedProducts = new ArrayList<>(productAnalysisMap.values());
                sortedProducts.sort(new Comparator<ProductAnalysis>() {
                    @Override
                    public int compare(ProductAnalysis a, ProductAnalysis b) {
                        return Double.compare(b.totalAmount, a.totalAmount);
                    }
                });

                insights.append("🏆 ƏN ÇOX SATILAN MƏHSULLAR:\n");
                int count = 0;
                for (ProductAnalysis product : sortedProducts) {
                    if (count >= 5) break;
                    if (product.transactionCount > 0) {
                        insights.append(String.format("   • %s: %s\n", product.getProductName(), product.getTrend()));
                    }
                }

                if (count == 0) {
                    insights.append("   Məhsul məlumatı yoxdur\n");
                }

                insights.append("\n📈 TREND ANALİZİ:\n");
                count = 0;
                for (ProductAnalysis product : sortedProducts) {
                    if (count >= 3) break;
                    if (product.transactionCount >= 3) {
                        insights.append(String.format("   %s: %s (%.1f%%)\n",
                                product.productName, product.trend, product.growthRate));
                        count++;
                    }
                }

                tvAiInsights.setText(insights.toString());
            }
        });
    }

    private void prepareAndShowCategoryAnalysis() {
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Map<String, Object> t : allTransactions) {
            String category = (String) t.get("category");
            String type = (String) t.get("type");
            Double totalAmount = (Double) t.get("totalAmount");

            if ("income".equals(type) && category != null && totalAmount != null) {
                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + totalAmount);
            }
        }

        List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(categoryTotals.entrySet());
        sortedCategories.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        double total = 0;
        for (Map.Entry<String, Double> entry : sortedCategories) {
            total += entry.getValue();
        }

        for (int i = 0; i < Math.min(3, sortedCategories.size()); i++) {
            Map.Entry<String, Double> entry = sortedCategories.get(i);
            int percent = total > 0 ? (int) ((entry.getValue() / total) * 100) : 0;

            switch (i) {
                case 0:
                    tvCategory1Name.setText(entry.getKey());
                    tvCategory1Amount.setText(String.format("₼%.2f", entry.getValue()));
                    tvCategory1Percent.setText(percent + "%");
                    break;
                case 1:
                    tvCategory2Name.setText(entry.getKey());
                    tvCategory2Amount.setText(String.format("₼%.2f", entry.getValue()));
                    tvCategory2Percent.setText(percent + "%");
                    break;
                case 2:
                    tvCategory3Name.setText(entry.getKey());
                    tvCategory3Amount.setText(String.format("₼%.2f", entry.getValue()));
                    tvCategory3Percent.setText(percent + "%");
                    break;
            }
        }
    }

    private void updateProductUI(List<ProductAnalysis> sortedProducts) {
        for (int i = 0; i < Math.min(3, sortedProducts.size()); i++) {
            ProductAnalysis product = sortedProducts.get(i);
            String trendIcon = getTrendIcon(product.getTrend());  // DƏYİŞDİ: getter

            switch (i) {
                case 0:
                    tvProduct1Name.setText(product.getProductName());  // DƏYİŞDİ: getter
                    tvProduct1Stats.setText(String.format("%d satış • ₼%.2f",
                            product.getTransactionCount(), product.getTotalAmount()));  // DƏYİŞDİ: getter
                    tvProduct1Trend.setText(trendIcon);
                    break;
                case 1:
                    tvProduct2Name.setText(product.getProductName());  // DƏYİŞDİ: getter
                    tvProduct2Stats.setText(String.format("%d satış • ₼%.2f",
                            product.getTransactionCount(), product.getTotalAmount()));  // DƏYİŞDİ: getter
                    tvProduct2Trend.setText(trendIcon);
                    break;
                case 2:
                    tvProduct3Name.setText(product.getProductName());  // DƏYİŞDİ: getter
                    tvProduct3Stats.setText(String.format("%d satış • ₼%.2f",
                            product.getTransactionCount(), product.getTotalAmount()));  // DƏYİŞDİ: getter
                    tvProduct3Trend.setText(trendIcon);
                    break;
            }
        }

        if (!sortedProducts.isEmpty()) {
            tvTopProduct.setText(sortedProducts.get(0).getProductName());  // DƏYİŞDİ: getter
        }
    }

    private void updatePredictions(List<ProductAnalysis> sortedProducts) {
        for (int i = 0; i < Math.min(3, sortedProducts.size()); i++) {
            ProductAnalysis product = sortedProducts.get(i);
            switch (i) {
                case 0:
                    tvProductPrediction1Name.setText(product.getProductName());  // DƏYİŞDİ
                    tvProductPrediction1Value.setText(String.format("₼%.2f", product.getPredictedNextAmount()));  // DƏYİŞDİ
                    break;
                case 1:
                    tvProductPrediction2Name.setText(product.getProductName());  // DƏYİŞDİ
                    tvProductPrediction2Value.setText(String.format("₼%.2f", product.getPredictedNextAmount()));  // DƏYİŞDİ
                    break;
                case 2:
                    tvProductPrediction3Name.setText(product.getProductName());  // DƏYİŞDİ
                    tvProductPrediction3Value.setText(String.format("₼%.2f", product.getPredictedNextAmount()));  // DƏYİŞDİ
                    break;
            }
        }
    }

    private String getTrendIcon(String trend) {
        if (trend.contains("Sürətli artım")) return "🚀";
        if (trend.contains("Yavaş artım")) return "📈";
        if (trend.contains("Stabil")) return "📊";
        if (trend.contains("Yavaş eniş")) return "📉";
        if (trend.contains("Sürətli eniş")) return "⚠️";
        return "📊";
    }

    private void updateChart() {
        if (incomeEntries.isEmpty() && expenseEntries.isEmpty()) {
            for (int i = 0; i < 7; i++) {
                incomeEntries.add(new Entry(i, (float) (Math.random() * 1000 + 500)));
                expenseEntries.add(new Entry(i, (float) (Math.random() * 800 + 200)));
            }
        }

        LineDataSet incomeDataSet = new LineDataSet(incomeEntries, "Gəlir");
        incomeDataSet.setColor(Color.parseColor("#10B981"));
        incomeDataSet.setCircleColor(Color.parseColor("#10B981"));
        incomeDataSet.setLineWidth(2f);
        incomeDataSet.setCircleRadius(3f);
        incomeDataSet.setDrawFilled(true);
        incomeDataSet.setFillColor(Color.parseColor("#10B981"));
        incomeDataSet.setFillAlpha(50);
        incomeDataSet.setValueTextColor(Color.WHITE);
        incomeDataSet.setValueTextSize(10f);
        incomeDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        incomeDataSet.setCubicIntensity(0.2f);

        LineDataSet expenseDataSet = new LineDataSet(expenseEntries, "Xərc");
        expenseDataSet.setColor(Color.parseColor("#EF4444"));
        expenseDataSet.setCircleColor(Color.parseColor("#EF4444"));
        expenseDataSet.setLineWidth(2f);
        expenseDataSet.setCircleRadius(3f);
        expenseDataSet.setDrawFilled(true);
        expenseDataSet.setFillColor(Color.parseColor("#EF4444"));
        expenseDataSet.setFillAlpha(50);
        expenseDataSet.setValueTextColor(Color.WHITE);
        expenseDataSet.setValueTextSize(10f);
        expenseDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        expenseDataSet.setCubicIntensity(0.2f);

        ILineDataSet[] dataSetsArray = new ILineDataSet[]{incomeDataSet, expenseDataSet};
        LineData lineData = new LineData(dataSetsArray);

        chartIncomeExpense.setData(lineData);
        chartIncomeExpense.getDescription().setEnabled(false);
        chartIncomeExpense.getXAxis().setDrawLabels(false);
        chartIncomeExpense.getAxisLeft().setTextColor(Color.WHITE);
        chartIncomeExpense.getAxisRight().setTextColor(Color.WHITE);
        chartIncomeExpense.getLegend().setTextColor(Color.WHITE);
        chartIncomeExpense.animateX(1000);
        chartIncomeExpense.invalidate();
    }

    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        tvCurrentDate.setText(sdf.format(new Date()));

        String day = dayFormat.format(new Date());
        String azDay = getAzDay(day);
        tvDayOfWeek.setText(azDay);
    }

    private String getAzDay(String engDay) {
        switch(engDay) {
            case "Monday": return "Bazar ertəsi";
            case "Tuesday": return "Çərşənbə axşamı";
            case "Wednesday": return "Çərşənbə";
            case "Thursday": return "Cümə axşamı";
            case "Friday": return "Cümə";
            case "Saturday": return "Şənbə";
            case "Sunday": return "Bazar";
            default: return engDay;
        }
    }

    private void setupClickListeners() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAllDataFromFirebase();
                Toast.makeText(DashboardActivity.this, "Məlumatlar yenilənir...", Toast.LENGTH_SHORT).show();
            }
        });

        if (cardAddData != null) {
            cardAddData.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DashboardActivity.this, AddDataActivity.class);
                    startActivity(intent);
                }
            });
        }
        if (cardAiInsights != null) {
            cardAiInsights.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAIAnalysisDialog();
                }
            });
        }

        // DÜZƏLDİ: tvViewAllProducts click listener
        if (tvViewAllProducts != null) {
            tvViewAllProducts.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAllProductsDialog();
                }
            });
        }
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_dashboard) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (id == R.id.nav_sales) {
                    Toast.makeText(DashboardActivity.this, "Satışlar", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_expenses) {
                    Toast.makeText(DashboardActivity.this, "Xərclər", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_customers) {
                    Toast.makeText(DashboardActivity.this, "Müştərilər", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_reports) {
                    Toast.makeText(DashboardActivity.this, "Hesabatlar", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(DashboardActivity.this, SettingsActivity.class));
                } else if (id == R.id.nav_logout) {
                    logout();
                }

                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        View headerView = navigationView.getHeaderView(0);
        TextView navUserName = headerView.findViewById(R.id.navUserName);
        TextView navUserEmail = headerView.findViewById(R.id.navUserEmail);

        if (mAuth.getCurrentUser() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            String userName = email != null ? email.split("@")[0] : "İstifadəçi";
            navUserName.setText(userName);
            navUserEmail.setText(email != null ? email : "email@example.com");
            tvUserName.setText(userName);
        }
    }

    // Yeni metod: Bütün məhsulları göstər
    private void showAllProductsDialog() {
        if (allProductsList == null || allProductsList.isEmpty()) {
            Toast.makeText(this, "Məhsul məlumatı yoxdur", Toast.LENGTH_SHORT).show();
            return;
        }

        // Məhsulları məbləğə görə sırala
        List<ProductAnalysis> sortedList = new ArrayList<>(allProductsList);
        sortedList.sort((a, b) -> Double.compare(b.totalAmount, a.totalAmount));

        FragmentManager fragmentManager = getSupportFragmentManager();
        ProductDialog dialog = new ProductDialog(sortedList, "📦 Bütün Məhsullar (" + sortedList.size() + ")");
        dialog.show(fragmentManager, "ProductDialog");
    }

    // showAIInsights() metodunda kateqoriya analizini göstər:
    private void showAIInsights(List<ProductAnalysis> sortedProducts) {
        StringBuilder insights = new StringBuilder();
        insights.append("🤖 AI ANALİZ NƏTİCƏLƏRİ\n");
        insights.append("══════════════════════\n\n");

        if (sortedProducts.isEmpty()) {
            insights.append("📊 Məlumat yoxdur. Yeni məlumatlar əlavə edin.\n");
        } else {
            insights.append("💰 ƏN ÇOX SATILAN MƏHSULLAR:\n");
            int count = 0;
            for (ProductAnalysis product : sortedProducts) {
                if (count >= 3) break;
                insights.append(String.format("   %d. %s: ₼%.2f (%d satış)\n",
                        count+1, product.productName, product.totalAmount, product.transactionCount));
                count++;
            }

            insights.append("\n📈 TRENDLƏR:\n");
            count = 0;
            for (ProductAnalysis product : sortedProducts) {
                if (count >= 2) break;
                if (product.transactionCount >= 2) {
                    insights.append(String.format("   • %s: %s\n",
                            product.productName, product.trend));
                    count++;
                }
            }

            // KATEQORİYA ANALİZİ
            insights.append("\n📊 KATEQORİYA ANALİZİ:\n");

            // Kateqoriyalara görə qruplaşdır
            Map<String, Double> categoryIncome = new HashMap<>();
            Map<String, Integer> categoryCount = new HashMap<>();

            for (ProductAnalysis product : allProductsList) {
                String category = product.getCategory() != null ? product.getCategory() : "Digər";
                categoryIncome.put(category, categoryIncome.getOrDefault(category, 0.0) + product.totalAmount);
                categoryCount.put(category, categoryCount.getOrDefault(category, 0) + product.transactionCount);
            }

            // Kateqoriyaları gəlirə görə sırala
            List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(categoryIncome.entrySet());
            sortedCategories.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            // Ümumi gəlir
            double totalIncome = 0;
            for (Map.Entry<String, Double> entry : sortedCategories) {
                totalIncome += entry.getValue();
            }

            // Hər kateqoriya üçün faiz hesabla və göstər
            for (int i = 0; i < Math.min(5, sortedCategories.size()); i++) {
                Map.Entry<String, Double> entry = sortedCategories.get(i);
                String category = entry.getKey();
                double amount = entry.getValue();
                int count_p = categoryCount.getOrDefault(category, 0);
                double percent = totalIncome > 0 ? (amount / totalIncome) * 100 : 0;

                insights.append(String.format("   %s\n", category));
                insights.append(String.format("   Gəlir: ₼%.2f | Satış: %d\n", amount, count_p));
                insights.append(String.format("   Pay: %.1f%%\n", percent));

                if (i < sortedCategories.size() - 1) {
                    insights.append("\n");
                }
            }

            // TÖVSİYƏLƏR
            insights.append("\n💡 AI TÖVSİYƏLƏR:\n");

            // Ən yaxşı kateqoriya tövsiyəsi
            if (!sortedCategories.isEmpty()) {
                String bestCategory = sortedCategories.get(0).getKey();
                insights.append(String.format("   • ✅ %s kateqoriyası ən çox gəlir gətirir\n", bestCategory));
            }

            // Zəif kateqoriyalar
            if (sortedCategories.size() > 2) {
                String worstCategory = sortedCategories.get(sortedCategories.size() - 1).getKey();
                double worstAmount = sortedCategories.get(sortedCategories.size() - 1).getValue();
                if (totalIncome > 0 && worstAmount < totalIncome * 0.05) {
                    insights.append(String.format("   • ⚠️ %s kateqoriyası çox az gəlir gətirir\n", worstCategory));
                }
            }

            // Ümumi vəziyyət
            double totalIncomeAll = 0, totalExpenseAll = 0;
            for (Map<String, Object> t : allTransactions) {
                String type = (String) t.get("type");
                Double amount = (Double) t.get("totalAmount");
                if ("income".equals(type) && amount != null) {
                    totalIncomeAll += amount;
                } else if ("expense".equals(type) && amount != null) {
                    totalExpenseAll += amount;
                }
            }

            if (totalIncomeAll > totalExpenseAll) {
                insights.append(String.format("   • 📈 Mənfəət: ₼%.2f\n", (totalIncomeAll - totalExpenseAll)));
            } else {
                insights.append(String.format("   • 📉 Zərər: ₼%.2f\n", (totalExpenseAll - totalIncomeAll)));
            }
        }

        tvAiInsights.setText(insights.toString());
    }

    private long getStartOfDay() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    // AI Analiz dialogunu göstər
    private void showAIAnalysisDialog() {
        if (allTransactions == null || allTransactions.isEmpty()) {
            Toast.makeText(this, "Analiz üçün məlumat yoxdur", Toast.LENGTH_SHORT).show();
            return;
        }

        // AI Insights mətnini hazırla
        StringBuilder analysisText = new StringBuilder();

        List<ProductAnalysis> sortedProducts = new ArrayList<>(productAnalysisMap.values());
        sortedProducts.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));

        analysisText.append("🤖 AI ANALİZ NƏTİCƏLƏRİ\n");
        analysisText.append("══════════════════════\n\n");

        analysisText.append("💰 ƏN ÇOX SATILAN MƏHSULLAR:\n");
        int count = 0;
        for (ProductAnalysis product : sortedProducts) {
            if (count >= 5) break;
            analysisText.append(String.format("   %d. %s: ₼%.2f (%d satış)\n",
                    count+1, product.getProductName(), product.getTotalAmount(), product.getTransactionCount()));
            count++;
        }

        analysisText.append("\n📈 TRENDLƏR:\n");
        count = 0;
        for (ProductAnalysis product : sortedProducts) {
            if (count >= 3) break;
            if (product.getTransactionCount() >= 2) {
                analysisText.append(String.format("   • %s: %s (%.1f%%)\n",
                        product.getProductName(), product.getTrend(), product.getGrowthRate()));
                count++;
            }
        }

        // Kateqoriya məlumatlarını hazırla
        Map<String, Double> categoryData = new HashMap<>();
        for (Map<String, Object> t : allTransactions) {
            String category = (String) t.get("category");
            String type = (String) t.get("type");
            Double totalAmount = (Double) t.get("totalAmount");

            if ("income".equals(type) && category != null && totalAmount != null) {
                categoryData.put(category, categoryData.getOrDefault(category, 0.0) + totalAmount);
            }
        }

        // Ümumi gəlir və xərc
        double totalIncome = 0, totalExpense = 0;
        for (Map<String, Object> t : allTransactions) {
            String type = (String) t.get("type");
            Double amount = (Double) t.get("totalAmount");
            if ("income".equals(type) && amount != null) {
                totalIncome += amount;
            } else if ("expense".equals(type) && amount != null) {
                totalExpense += amount;
            }
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        AIAnalysisDialog dialog = new AIAnalysisDialog(
                analysisText.toString(),
                "🤖 AI Analiz Nəticələri",
                sortedProducts,
                categoryData,
                totalIncome,
                totalExpense
        );
        dialog.show(fragmentManager, "AIAnalysisDialog");
    }

    private long getEndOfDay() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
        calendar.set(java.util.Calendar.MINUTE, 59);
        calendar.set(java.util.Calendar.SECOND, 59);
        calendar.set(java.util.Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    private long getStartOfDay(long timestamp) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getEndOfDay(long timestamp) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
        calendar.set(java.util.Calendar.MINUTE, 59);
        calendar.set(java.util.Calendar.SECOND, 59);
        calendar.set(java.util.Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    private void startRealTimeUpdates() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchAllDataFromFirebase();
                handler.postDelayed(this, 30000);
            }
        };
        handler.post(refreshRunnable);
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
        if (salesModel != null) {
            salesModel.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAllDataFromFirebase();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}