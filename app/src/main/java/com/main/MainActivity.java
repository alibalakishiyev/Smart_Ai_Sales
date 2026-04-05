package com.main;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.DiscountMarket.DiscountActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.authentication.LogoutManager;
import com.dashboard.DashboardActivity;
import com.data.AddDataActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.model.FinanceMLModel;
import com.model.ProductMLModel;
import com.ocr_service.ReceiptScannerActivity;
import com.report.ReportActivity;
import com.service.FinanceMonitoringService;
import com.smart_ai_sales.R;
import com.utils.BaseActivity;
import com.utils.SettingsActivity;

import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MAIN_ACTIVITY";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    // UI Elements
    private MaterialCardView cardDashboard, cardDiscounts, cardAddData, cardReports, cardSettings, cardReceiptScanner, cardAIAnalysis;
    private MaterialButton btnDashboard, btnDiscounts, btnAddData, btnReports, btnSettings, btnReceiptScanner, btnAIAnalysis;
    private TextView tvWelcome, tvQuote, tvVersion, tvAIAnalysis;
    private LottieAnimationView animationView;
    private ViewPager2 viewPagerFeatures;
    private TabLayout tabLayout;
    private RecyclerView recyclerViewStats;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // AI Models
    private FinanceMLModel financeModel;
    private boolean isAIActive = false;

    // Animations
    private Animation fadeIn, slideUp;

    // Real Data Lists
    private List<FeatureItem> featureItems;
    private List<StatItem> statItems;
    private FeaturesAdapter featuresAdapter;
    private StatsAdapter statsAdapter;

    // Statistics
    private double totalIncome = 0;
    private double totalExpense = 0;
    private double todayIncome = 0;
    private double todayExpense = 0;
    private int totalTransactions = 0;
    private int totalProducts = 0;
    private double balance = 0;

    // AI Analysis Results
    private String aiInsight = "";
    private float aiRiskScore = 0;
    private double aiPredictedExpense = 0;

    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private long lastAdShownTime = 0;
    private final long AD_INTERVAL = 60000;

    // Formatting
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity started");

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            Log.d(TAG, "User: " + currentUser.getEmail());
        } else {
            Log.d(TAG, "No user logged in");
        }

        // Initialize formatters
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("az", "AZ"));
        currencyFormat.setMaximumFractionDigits(2);
        dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        // Initialize Views
        initViews();

        // Initialize AI Model
        initializeAIModel();

        // Load Animations
        loadAnimations();

        // Setup Click Listeners
        setupClickListeners();

        // Initialize Adapters
        initAdapters();

        loadInterstitialAd();
        showInterstitialAd();

        // Load Real Data
        loadRealData();

        // Animate Elements
        animateElements();

        // Check User
        checkUserStatus();
    }

    private void initViews() {
        // Cards
        cardDashboard = findViewById(R.id.cardDashboard);
        cardAddData = findViewById(R.id.cardAddData);
        cardReports = findViewById(R.id.cardReports);
        cardSettings = findViewById(R.id.cardSettings);
        cardReceiptScanner = findViewById(R.id.cardReceiptScanner);
        cardDiscounts = findViewById(R.id.cardDiscounts);
        cardAIAnalysis = findViewById(R.id.cardAIAnalysis);

        // Buttons
        btnDashboard = findViewById(R.id.btnDashboard);
        btnAddData = findViewById(R.id.btnAddData);
        btnReports = findViewById(R.id.btnReports);
        btnSettings = findViewById(R.id.btnSettings);
        btnReceiptScanner = findViewById(R.id.btnReceiptScanner);
        btnDiscounts = findViewById(R.id.btnDiscounts);
        btnAIAnalysis = findViewById(R.id.btnAIAnalysis);

        // Text
        tvWelcome = findViewById(R.id.tvWelcome);
        tvQuote = findViewById(R.id.tvQuote);
        tvVersion = findViewById(R.id.tvVersion);
        tvAIAnalysis = findViewById(R.id.tvAIAnalysis);

        // Animation
        animationView = findViewById(R.id.animationView);

        // ViewPager
        viewPagerFeatures = findViewById(R.id.viewPagerFeatures);
        tabLayout = findViewById(R.id.tabLayout);

        // RecyclerView
        recyclerViewStats = findViewById(R.id.recyclerViewStats);

        tvVersion.setText("Version 1.0.0");

        Intent serviceIntent = new Intent(this, FinanceMonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        mAdView = findViewById(R.id.adView1);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void initializeAIModel() {
        try {
            financeModel = new FinanceMLModel(this);
            isAIActive = financeModel != null && financeModel.isInitialized();

            if (isAIActive) {
                Log.d(TAG, "✅ AI Model aktivdir!");
                tvAIAnalysis.setText("🤖 AI analiz edilir...");
            } else {
                Log.w(TAG, "AI Model aktiv deyil");
                tvAIAnalysis.setText("📊 AI analiz üçün məlumat toplanır");
            }
        } catch (Exception e) {
            Log.e(TAG, "AI Model yüklənmədi", e);
            isAIActive = false;
            tvAIAnalysis.setText("📊 Statistik məlumatlar");
        }
    }

    private void loadAnimations() {
        fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
    }

    private void initAdapters() {
        featureItems = new ArrayList<>();
        featuresAdapter = new FeaturesAdapter(featureItems);
        viewPagerFeatures.setAdapter(featuresAdapter);

        statItems = new ArrayList<>();
        statsAdapter = new StatsAdapter(statItems);
        recyclerViewStats.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));
        recyclerViewStats.setAdapter(statsAdapter);

        new TabLayoutMediator(tabLayout, viewPagerFeatures,
                (tab, position) -> {}
        ).attach();
    }

    private void loadRealData() {
        if (currentUser == null) {
            Log.d(TAG, "No user, loading default data");
            loadDefaultFeatures();
            loadDefaultStats();
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Loading data for user: " + userId);

        // Load products as features
        loadProductsAsFeatures(userId);

        // Load transactions statistics
        loadTransactionStats(userId);
    }

    private void loadProductsAsFeatures(String userId) {
        Log.d(TAG, "Loading products for features");

        db.collection("products")
                .whereEqualTo("userId", userId)
                .orderBy("name")
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    featureItems.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No products found, loading default features");
                        loadDefaultFeatures();
                        return;
                    }

                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " products");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String category = doc.getString("category");
                        Double price = doc.getDouble("price");
                        Double kg = doc.getDouble("kg");
                        Double liter = doc.getDouble("liter");

                        String description = "";
                        if (kg != null && kg > 0) {
                            description = String.format("%.2f kq", kg);
                        } else if (liter != null && liter > 0) {
                            description = String.format("%.2f L", liter);
                        } else if (price != null && price > 0) {
                            description = currencyFormat.format(price);
                        } else {
                            description = "Qiymət yoxdur";
                        }

                        FeatureItem item = new FeatureItem(
                                name != null ? name : "Məhsul",
                                description,
                                getCategoryIcon(category),
                                doc.getId()
                        );
                        featureItems.add(item);

                        Log.d(TAG, "Added product: " + name);
                    }

                    featuresAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading products: " + e.getMessage());
                    loadDefaultFeatures();
                });
    }

    private void loadDefaultFeatures() {
        featureItems.clear();
        featureItems.add(new FeatureItem(
                "AI Analiz",
                "Süni intellekt analizi",
                R.drawable.ic_ai_feature,
                "default_1"
        ));
        featureItems.add(new FeatureItem(
                "Real-time İzləmə",
                "Anlıq yenilənir",
                R.drawable.ic_realtime,
                "default_2"
        ));
        featureItems.add(new FeatureItem(
                "Firebase Bulud",
                "Təhlükəsiz saxlanılır",
                R.drawable.ic_firebase_feature,
                "default_3"
        ));
        featureItems.add(new FeatureItem(
                "Proqnozlaşdırma",
                "Satış proqnozu",
                R.drawable.ic_chart_feature,
                "default_4"
        ));
        featuresAdapter.notifyDataSetChanged();
    }

    private int getCategoryIcon(String category) {
        if (category == null) return R.drawable.ic_product_default;

        switch (category.toLowerCase()) {
            case "elektronika":
                return R.drawable.ic_electronics;
            case "geyim":
                return R.drawable.ic_clothing;
            case "qida":
                return R.drawable.ic_food;
            case "kitab":
                return R.drawable.ic_books;
            default:
                return R.drawable.ic_product_default;
        }
    }

    private void loadTransactionStats(String userId) {
        Log.d(TAG, "Loading transaction statistics");

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endOfDay = calendar.getTime();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalTransactions = queryDocumentSnapshots.size();
                    totalIncome = 0;
                    totalExpense = 0;
                    todayIncome = 0;
                    todayExpense = 0;

                    Log.d(TAG, "Total transactions: " + totalTransactions);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type");

                        Boolean isBulk = doc.getBoolean("isBulkTransaction");
                        Double amount = null;
                        if (isBulk != null && isBulk) {
                            amount = doc.getDouble("groupTotalAmount");
                        } else {
                            amount = doc.getDouble("totalAmount");
                            if (amount == null) {
                                amount = doc.getDouble("amount");
                            }
                        }

                        Date transactionDate = getDateFromDoc(doc, "timestamp");
                        if (transactionDate == null) {
                            transactionDate = getDateFromDoc(doc, "date");
                        }

                        if (amount != null) {
                            if ("income".equals(type)) {
                                totalIncome += amount;
                            } else if ("expense".equals(type)) {
                                totalExpense += amount;
                            }
                        }

                        if (transactionDate != null &&
                                !transactionDate.before(startOfDay) &&
                                !transactionDate.after(endOfDay)) {
                            if ("income".equals(type) && amount != null) {
                                todayIncome += amount;
                            } else if ("expense".equals(type) && amount != null) {
                                todayExpense += amount;
                            }
                        }
                    }

                    balance = totalIncome - totalExpense;

                    Log.d(TAG, String.format("Stats - Income: %.2f, Expense: %.2f, Balance: %.2f",
                            totalIncome, totalExpense, balance));

                    loadProductsCount(userId);

                    // Run AI Analysis after loading data
                    runAIAnalysis();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading transactions: " + e.getMessage());
                    loadDefaultStats();
                });
    }

    private void runAIAnalysis() {
        if (!isAIActive || financeModel == null) {
            updateAIDisplayWithStats();
            return;
        }

        try {
            // Prepare data for AI analysis
            double weeklyExpense = calculateWeeklyExpense();
            double monthlyExpense = totalExpense;
            double dailyAvg = weeklyExpense / 7;

            // Calculate risk score
            if (monthlyExpense > 0) {
                aiRiskScore = (float) (weeklyExpense / (monthlyExpense / 4));
                if (aiRiskScore > 1.3f) aiRiskScore = 0.8f;
                else if (aiRiskScore > 1.1f) aiRiskScore = 0.6f;
                else if (aiRiskScore > 0.9f) aiRiskScore = 0.4f;
                else aiRiskScore = 0.3f;
            } else {
                aiRiskScore = 0.3f;
            }

            // Predict next 3 days expense
            aiPredictedExpense = dailyAvg * 3;

            // Generate AI insight
            aiInsight = generateAIInsight();

            // Update UI
            updateAIDisplay();

        } catch (Exception e) {
            Log.e(TAG, "AI Analysis failed", e);
            updateAIDisplayWithStats();
        }
    }

    private double calculateWeeklyExpense() {
        long weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        double weeklyExpense = 0;

        // This would need transaction data - simplified for now
        if (totalTransactions > 0 && totalExpense > 0) {
            weeklyExpense = totalExpense / 4; // Approximate
        }

        return weeklyExpense;
    }

    private String generateAIInsight() {
        StringBuilder insight = new StringBuilder();

        // Financial health assessment
        if (balance < 0) {
            insight.append("⚠️ XƏBƏRDARLIQ: Balans mənfidir! Xərclərinizi azaldın.\n");
        } else if (balance < 100) {
            insight.append("⚡ Balans azalır. Diqqətli olun.\n");
        } else {
            insight.append("✅ Maliyyə vəziyyətiniz yaxşıdır.\n");
        }

        // Risk assessment
        if (aiRiskScore > 0.7f) {
            insight.append("🔴 YÜKSƏK RİSK: Həftəlik xərcləriniz çox artıb!\n");
        } else if (aiRiskScore > 0.4f) {
            insight.append("🟡 ORTA RİSK: Xərclərinizə nəzarət edin.\n");
        } else {
            insight.append("🟢 AŞAĞI RİSK: Normal davam edin.\n");
        }

        // Prediction
        insight.append(String.format("📊 3 GÜNLÜK PROQNOZ: ₼%.2f xərc gözlənilir.\n", aiPredictedExpense));

        // Savings recommendation
        double recommendedSavings = aiPredictedExpense * 0.15;
        insight.append(String.format("💡 TÖVSİYƏ: ₼%.2f qənaət etməyə çalışın.\n", recommendedSavings));

        // Expense ratio warning
        if (totalIncome > 0) {
            double expenseRatio = (totalExpense / totalIncome) * 100;
            if (expenseRatio > 70) {
                insight.append("📉 Xərclər gəlirin 70%-dən çoxdur! Büdcə planlaması edin.");
            } else if (expenseRatio > 50) {
                insight.append("📊 Xərclər gəlirin 50%-ni keçir. Diqqətli olun.");
            } else {
                insight.append("🎉 Əla! Xərcləriniz gəlirinizin altında qalır.");
            }
        }

        return insight.toString();
    }

    private void updateAIDisplay() {
        runOnUiThread(() -> {
            StringBuilder display = new StringBuilder();
            display.append("🤖 AI ANALİZİ\n");
            display.append("══════════════════\n\n");
            display.append(aiInsight);

            tvAIAnalysis.setText(display.toString());
        });
    }

    private void updateAIDisplayWithStats() {
        runOnUiThread(() -> {
            StringBuilder display = new StringBuilder();
            display.append("📊 MALİYYƏ STATİSTİKASI\n");
            display.append("══════════════════\n\n");
            display.append(String.format("💰 Balans: %s\n", formatCurrency(balance)));
            display.append(String.format("📈 Ümumi Gəlir: %s\n", formatCurrency(totalIncome)));
            display.append(String.format("📉 Ümumi Xərc: %s\n", formatCurrency(totalExpense)));
            display.append(String.format("📦 Məhsul sayı: %d\n", totalProducts));
            display.append(String.format("🔄 Əməliyyat sayı: %d\n", totalTransactions));

            if (totalIncome > 0) {
                double expenseRatio = (totalExpense / totalIncome) * 100;
                display.append(String.format("\n📊 Xərc/Gəlir nisbəti: %.1f%%", expenseRatio));
                if (expenseRatio > 70) {
                    display.append(" (⚠️ Yüksək)");
                } else if (expenseRatio > 50) {
                    display.append(" (⚡ Orta)");
                } else {
                    display.append(" (✅ Yaxşı)");
                }
            }

            tvAIAnalysis.setText(display.toString());
        });
    }

    private Date getDateFromDoc(DocumentSnapshot doc, String field) {
        try {
            Timestamp timestamp = doc.getTimestamp(field);
            if (timestamp != null) {
                return timestamp.toDate();
            }
        } catch (Exception e) {
            // Ignore
        }

        try {
            Date date = doc.getDate(field);
            if (date != null) {
                return date;
            }
        } catch (Exception e) {
            // Ignore
        }

        try {
            Long timestampLong = doc.getLong(field);
            if (timestampLong != null) {
                return new Date(timestampLong);
            }
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }

    private void loadProductsCount(String userId) {
        db.collection("products")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalProducts = queryDocumentSnapshots.size();
                    Log.d(TAG, "Total products: " + totalProducts);
                    updateStatsDisplay();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading products count: " + e.getMessage());
                    totalProducts = 0;
                    updateStatsDisplay();
                });
    }

    private void loadDefaultStats() {
        statItems.clear();

        statItems.add(new StatItem("Balans", "₼ 0.00", R.drawable.ic_balance, "#4CAF50"));
        statItems.add(new StatItem("Ümumi Gəlir", "₼ 0.00", R.drawable.ic_income, "#4CAF50"));
        statItems.add(new StatItem("Ümumi Xərc", "₼ 0.00", R.drawable.ic_expense, "#F44336"));
        statItems.add(new StatItem("Məhsullar", "0", R.drawable.ic_products, "#2196F3"));
        statItems.add(new StatItem("Əməliyyatlar", "0", R.drawable.ic_transactions, "#9C27B0"));

        statsAdapter.notifyDataSetChanged();
    }

    private void updateStatsDisplay() {
        statItems.clear();

        statItems.add(new StatItem(
                "Balans",
                formatCurrency(balance),
                R.drawable.ic_balance,
                balance >= 0 ? "#4CAF50" : "#F44336"
        ));

        statItems.add(new StatItem(
                "Ümumi Gəlir",
                formatCurrency(totalIncome),
                R.drawable.ic_income,
                "#4CAF50"
        ));

        statItems.add(new StatItem(
                "Ümumi Xərc",
                formatCurrency(totalExpense),
                R.drawable.ic_expense,
                "#F44336"
        ));

        statItems.add(new StatItem(
                "Bugünki Gəlir",
                formatCurrency(todayIncome),
                R.drawable.ic_today_income,
                "#4CAF50"
        ));

        statItems.add(new StatItem(
                "Bugünki Xərc",
                formatCurrency(todayExpense),
                R.drawable.ic_today_expense,
                "#F44336"
        ));

        statItems.add(new StatItem(
                "Məhsullar",
                String.valueOf(totalProducts),
                R.drawable.ic_products,
                "#2196F3"
        ));

        statItems.add(new StatItem(
                "Əməliyyatlar",
                String.valueOf(totalTransactions),
                R.drawable.ic_transactions,
                "#9C27B0"
        ));

        Log.d(TAG, "Stats updated, items: " + statItems.size());
        statsAdapter.notifyDataSetChanged();
        animateStatsCards();
    }

    private String formatCurrency(double amount) {
        return String.format("₼ %,.2f", amount);
    }

    private void animateStatsCards() {
        for (int i = 0; i < recyclerViewStats.getChildCount(); i++) {
            View view = recyclerViewStats.getChildAt(i);
            view.setAlpha(0f);
            view.setTranslationY(50f);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(i * 100)
                    .start();
        }
    }

    private void setupClickListeners() {
        // Dashboard
        View.OnClickListener dashboardClick = v -> {
            animateClick(v);
            new Handler().postDelayed(() -> {
                startActivity(new Intent(this, DashboardActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 200);
        };
        cardDashboard.setOnClickListener(dashboardClick);
        btnDashboard.setOnClickListener(dashboardClick);

        // Add Data
        View.OnClickListener addDataClick = v -> {
            animateClick(v);
            new Handler().postDelayed(() -> {
                startActivity(new Intent(this, AddDataActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 200);
        };
        cardAddData.setOnClickListener(addDataClick);
        btnAddData.setOnClickListener(addDataClick);

        // Reports
        View.OnClickListener reportsClick = v -> {
            animateClick(v);
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(MainActivity.this, ReportActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 200);
        };
        cardReports.setOnClickListener(reportsClick);
        btnReports.setOnClickListener(reportsClick);

        // Receipt Scanner
        View.OnClickListener receiptScannerClick = v -> {
            animateClick(v);
            checkCameraPermissionAndOpenScanner();
        };
        cardReceiptScanner.setOnClickListener(receiptScannerClick);
        btnReceiptScanner.setOnClickListener(receiptScannerClick);

        // Discounts
        View.OnClickListener discountsClick = v -> {
            animateClick(v);
            new Handler().postDelayed(() -> {
                startActivity(new Intent(this, DiscountActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 200);
        };
        cardDiscounts.setOnClickListener(discountsClick);
        btnDiscounts.setOnClickListener(discountsClick);

        // AI Analysis
        View.OnClickListener aiClick = v -> {
            animateClick(v);
            runAIAnalysis();
            Toast.makeText(this, "AI analiz yeniləndi", Toast.LENGTH_SHORT).show();
        };
        cardAIAnalysis.setOnClickListener(aiClick);
        btnAIAnalysis.setOnClickListener(aiClick);

        // Settings
        View.OnClickListener settingsClick = v -> {
            animateClick(v);
            new Handler().postDelayed(() -> {
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }, 200);
        };
        cardSettings.setOnClickListener(settingsClick);
        btnSettings.setOnClickListener(settingsClick);

        // Logout button
        ImageView btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            LogoutManager.getInstance().showLogoutConfirmationDialog(this);
        });
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-5367924704859976/6825294889", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                Log.d("MainActivity", "Interstitial reklamı uğurla yükləndi.");
                showInterstitialAd();

                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d("MainActivity", "Reklam bağlandı. Yeni reklam yüklənir...");
                        mInterstitialAd = null;
                        loadInterstitialAd();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        Log.d("MainActivity", "Reklam göstərilmədi: " + adError.getMessage());
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d("MainActivity", "Reklam göstərilir.");
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitialAd = null;
                Log.d("MainActivity", "Interstitial reklamı yüklənmədi: " + loadAdError.getMessage());
            }
        });
    }

    private void showInterstitialAd() {
        long currentTime = System.currentTimeMillis();
        if (mInterstitialAd != null && (currentTime - lastAdShownTime >= AD_INTERVAL)) {
            Log.d("MainActivity", "Reklam göstərilir...");
            mInterstitialAd.show(MainActivity.this);
            lastAdShownTime = currentTime;
        } else if (mInterstitialAd == null) {
            Log.d("MainActivity", "Reklam hazır deyil.");
        } else {
            Log.d("MainActivity", "Reklam vaxtı tamamlanmayıb. Gözlənilir...");
        }
    }

    private void checkCameraPermissionAndOpenScanner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openReceiptScanner();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
            }
        } else {
            openReceiptScanner();
        }
    }

    private void openReceiptScanner() {
        Intent intent = new Intent(this, ReceiptScannerActivity.class);
        startActivityForResult(intent, 1001);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "OCR tamamlandı, məlumatlar alındı");

            String storeName = data.getStringExtra("store_name");
            String date = data.getStringExtra("date");
            String time = data.getStringExtra("time");
            double totalAmount = data.getDoubleExtra("total_amount", 0);
            String docId = data.getStringExtra("doc_id");
            String fiscalCode = data.getStringExtra("fiscal_code");

            ArrayList<String> productNames = data.getStringArrayListExtra("product_names");
            ArrayList<String> productPrices = data.getStringArrayListExtra("product_prices");
            ArrayList<String> productQuantities = data.getStringArrayListExtra("product_quantities");
            ArrayList<String> productUnits = data.getStringArrayListExtra("product_units");

            Intent addDataIntent = new Intent(this, AddDataActivity.class);
            addDataIntent.putExtra("store_name", storeName);
            addDataIntent.putExtra("date", date);
            addDataIntent.putExtra("time", time);
            addDataIntent.putExtra("total_amount", totalAmount);
            addDataIntent.putExtra("doc_id", docId);
            addDataIntent.putExtra("fiscal_code", fiscalCode);
            addDataIntent.putStringArrayListExtra("product_names", productNames);
            addDataIntent.putStringArrayListExtra("product_prices", productPrices);
            addDataIntent.putStringArrayListExtra("product_quantities", productQuantities);
            addDataIntent.putStringArrayListExtra("product_units", productUnits);
            addDataIntent.putExtra("from_ocr", true);

            startActivity(addDataIntent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openReceiptScanner();
            } else {
                Toast.makeText(this, "Qəbz skan etmək üçün kamera icazəsi lazımdır", Toast.LENGTH_LONG).show();
                new AlertDialog.Builder(this)
                        .setTitle("Kamera İcazəsi")
                        .setMessage("Qəbzləri skan etmək üçün kamera icazəsi lazımdır. İcazəni settings-dən verə bilərsiniz.")
                        .setPositiveButton("Başa düşdüm", null)
                        .show();
            }
        }
    }

    private void animateClick(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void animateElements() {
        animationView.playAnimation();
        tvWelcome.startAnimation(slideUp);
        tvQuote.startAnimation(fadeIn);

        new Handler().postDelayed(() -> cardDashboard.startAnimation(slideUp), 100);
        new Handler().postDelayed(() -> cardAddData.startAnimation(slideUp), 200);
        new Handler().postDelayed(() -> cardReports.startAnimation(slideUp), 300);
        new Handler().postDelayed(() -> cardReceiptScanner.startAnimation(slideUp), 350);
        new Handler().postDelayed(() -> cardDiscounts.startAnimation(slideUp), 400);
        new Handler().postDelayed(() -> cardAIAnalysis.startAnimation(slideUp), 450);
        new Handler().postDelayed(() -> cardSettings.startAnimation(slideUp), 500);
    }

    private void checkUserStatus() {
        if (currentUser != null) {
            String email = currentUser.getEmail();
            if (email != null) {
                String name = email.split("@")[0];
                tvWelcome.setText("Xoş gəldin, " + name + "!");
            } else {
                tvWelcome.setText("Xoş gəldin!");
            }
        } else {
            tvWelcome.setText("Xoş gəldin, Qonaq!");
        }
        tvQuote.setText(getDailyQuote());
    }

    private String getDailyQuote() {
        String[] quotes = {
                "Uğur gündəlik kiçik addımlarla başlayır",
                "Hər satış yeni bir hekayədir",
                "Məlumat gələcəyin ən dəyərli sərvətidir",
                "AI ilə satışlarınızı zirvəyə qaldırın",
                "Bugün dünəndən daha yaxşı olsun",
                "Qəbzləri skan et, vaxta qənaət et"
        };
        return quotes[(int) (System.currentTimeMillis() / (1000 * 60 * 60 * 24)) % quotes.length];
    }

    // Inner classes
    private static class FeatureItem {
        String title, description;
        int imageResId;
        String firebaseId;

        FeatureItem(String title, String description, int imageResId, String firebaseId) {
            this.title = title;
            this.description = description;
            this.imageResId = imageResId;
            this.firebaseId = firebaseId;
        }
    }

    private static class StatItem {
        String title, value;
        int iconResId;
        String color;

        StatItem(String title, String value, int iconResId, String color) {
            this.title = title;
            this.value = value;
            this.iconResId = iconResId;
            this.color = color;
        }
    }

    // Features Adapter
    private class FeaturesAdapter extends RecyclerView.Adapter<FeaturesAdapter.FeatureViewHolder> {
        private List<FeatureItem> items;

        FeaturesAdapter(List<FeatureItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public FeatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_feature, parent, false);
            return new FeatureViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FeatureViewHolder holder, int position) {
            FeatureItem item = items.get(position);
            holder.imageView.setImageResource(item.imageResId);
            holder.textTitle.setText(item.title);
            holder.textDescription.setText(item.description);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class FeatureViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView textTitle, textDescription;

            FeatureViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageFeature);
                textTitle = itemView.findViewById(R.id.textFeatureTitle);
                textDescription = itemView.findViewById(R.id.textFeatureDescription);
            }
        }
    }

    // Stats Adapter
    private class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.StatViewHolder> {
        private List<StatItem> items;

        StatsAdapter(List<StatItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public StatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stat, parent, false);
            return new StatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StatViewHolder holder, int position) {
            StatItem item = items.get(position);
            holder.imageView.setImageResource(item.iconResId);
            holder.textTitle.setText(item.title);
            holder.textValue.setText(item.value);

            try {
                int color = android.graphics.Color.parseColor(item.color);
                holder.textValue.setTextColor(color);
                holder.imageView.setColorFilter(color);
            } catch (Exception e) {
                // Use default color
            }

            if (item.value.contains("₼")) {
                animateNumber(holder.textValue, item.value);
            }
        }

        private void animateNumber(TextView textView, String finalValue) {
            try {
                String clean = finalValue.replace("₼", "").replace(",", "").trim();
                double target = Double.parseDouble(clean);

                ValueAnimator animator = ValueAnimator.ofFloat(0f, (float) target);
                animator.setDuration(1000);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.addUpdateListener(animation -> {
                    float value = (float) animation.getAnimatedValue();
                    textView.setText(String.format("₼ %,.2f", value));
                });
                animator.start();
            } catch (NumberFormatException e) {
                textView.setText(finalValue);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class StatViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView textTitle, textValue;

            StatViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageStat);
                textTitle = itemView.findViewById(R.id.textStatTitle);
                textValue = itemView.findViewById(R.id.textStatValue);
            }
        }
    }
}