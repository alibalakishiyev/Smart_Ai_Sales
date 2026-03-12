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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.ocr_service.ReceiptScannerActivity;
import com.smart_ai_sales.R;
import com.utils.BaseActivity;
import com.utils.SettingsActivity;

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
    private MaterialCardView cardDashboard, cardAddData, cardReports, cardSettings, cardReceiptScanner; // <-- ƏLAVƏ
    private MaterialButton btnDashboard, btnAddData, btnReports, btnSettings, btnReceiptScanner; // <-- ƏLAVƏ
    private ImageView imgDashboard, imgAddData, imgReports, imgSettings, imgReceiptScanner; // <-- ƏLAVƏ
    private TextView tvWelcome, tvQuote, tvVersion;
    private LottieAnimationView animationView;
    private ViewPager2 viewPagerFeatures;
    private TabLayout tabLayout;
    private RecyclerView recyclerViewStats;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

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
        cardReceiptScanner = findViewById(R.id.cardReceiptScanner); // <-- ƏLAVƏ

        // Buttons
        btnDashboard = findViewById(R.id.btnDashboard);
        btnAddData = findViewById(R.id.btnAddData);
        btnReports = findViewById(R.id.btnReports);
        btnSettings = findViewById(R.id.btnSettings);
        btnReceiptScanner = findViewById(R.id.btnReceiptScanner); // <-- ƏLAVƏ

        // Images
        imgDashboard = findViewById(R.id.imgDashboard);
        imgAddData = findViewById(R.id.imgAddData);
        imgReports = findViewById(R.id.imgReports);
        imgSettings = findViewById(R.id.imgSettings);
        imgReceiptScanner = findViewById(R.id.imgReceiptScanner); // <-- ƏLAVƏ

        // Text
        tvWelcome = findViewById(R.id.tvWelcome);
        tvQuote = findViewById(R.id.tvQuote);
        tvVersion = findViewById(R.id.tvVersion);

        // Animation
        animationView = findViewById(R.id.animationView);

        // ViewPager
        viewPagerFeatures = findViewById(R.id.viewPagerFeatures);
        tabLayout = findViewById(R.id.tabLayout);

        // RecyclerView
        recyclerViewStats = findViewById(R.id.recyclerViewStats);

        tvVersion.setText("Version 1.0.0");

        mAdView = findViewById(R.id.adView1);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
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
                            description = kg + " kq";
                        } else if (liter != null && liter > 0) {
                            description = liter + " L";
                        } else if (price != null && price > 0) {
                            description = currencyFormat.format(price);
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

        // Get today's date range
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

        // Load all transactions
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
                        Double amount = doc.getDouble("amount");

                        // Get date safely
                        Date transactionDate = getDateFromDoc(doc, "date");

                        if (amount != null) {
                            if ("income".equals(type)) {
                                totalIncome += amount;
                            } else if ("expense".equals(type)) {
                                totalExpense += amount;
                            }
                        }

                        // Check if from today
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

                    // Load products count
                    loadProductsCount(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading transactions: " + e.getMessage());
                    loadDefaultStats();
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

        // Balance
        statItems.add(new StatItem(
                "Balans",
                formatCurrency(balance),
                R.drawable.ic_balance,
                balance >= 0 ? "#4CAF50" : "#F44336"
        ));

        // Total Income
        statItems.add(new StatItem(
                "Ümumi Gəlir",
                formatCurrency(totalIncome),
                R.drawable.ic_income,
                "#4CAF50"
        ));

        // Total Expense
        statItems.add(new StatItem(
                "Ümumi Xərc",
                formatCurrency(totalExpense),
                R.drawable.ic_expense,
                "#F44336"
        ));

        // Today's Income
        statItems.add(new StatItem(
                "Bugünki Gəlir",
                formatCurrency(todayIncome),
                R.drawable.ic_today_income,
                "#4CAF50"
        ));

        // Today's Expense
        statItems.add(new StatItem(
                "Bugünki Xərc",
                formatCurrency(todayExpense),
                R.drawable.ic_today_expense,
                "#F44336"
        ));

        // Products
        statItems.add(new StatItem(
                "Məhsullar",
                String.valueOf(totalProducts),
                R.drawable.ic_products,
                "#2196F3"
        ));

        // Transactions
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
        cardReports.setOnClickListener(v -> {
            animateClick(v);
            Toast.makeText(this, "Hesabatlar hazırlanır...", Toast.LENGTH_SHORT).show();
        });
        btnReports.setOnClickListener(v -> cardReports.performClick());

        // ===== RECEIPT SCANNER - YENİ ƏLAVƏ =====
        View.OnClickListener receiptScannerClick = v -> {
            animateClick(v);
            checkCameraPermissionAndOpenScanner();
        };
        cardReceiptScanner.setOnClickListener(receiptScannerClick);
        btnReceiptScanner.setOnClickListener(receiptScannerClick);

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
                        mInterstitialAd = null; // Mövcud reklam obyektini null edin.
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
            lastAdShownTime = currentTime; // Son reklam göstərilmə vaxtını yeniləyin
        } else if (mInterstitialAd == null) {
            Log.d("MainActivity", "Reklam hazır deyil.");
        } else {
            Log.d("MainActivity", "Reklam vaxtı tamamlanmayıb. Gözlənilir...");
        }
    }

    // ===== YENİ METOD: Kamera permission yoxlaması =====
    private void checkCameraPermissionAndOpenScanner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // Kamera icazəsi var - scanner aç
                openReceiptScanner();
            } else {
                // Kamera icazəsi yox - istə
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
            }
        } else {
            // Android 6-dan aşağı - birbaşa aç
            openReceiptScanner();
        }
    }

    // ===== YENİ METOD: Receipt scanner aç =====
    private void openReceiptScanner() {
        Intent intent = new Intent(this, ReceiptScannerActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    // ===== Permission nəticəsi =====
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // İcazə verildi
                openReceiptScanner();
            } else {
                // İcazə verilmədi
                Toast.makeText(this, "Qəbz skan etmək üçün kamera icazəsi lazımdır", Toast.LENGTH_LONG).show();

                // İcazə niyə lazımdır izah et
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
        new Handler().postDelayed(() -> cardReceiptScanner.startAnimation(slideUp), 350); // <-- ƏLAVƏ
        new Handler().postDelayed(() -> cardSettings.startAnimation(slideUp), 400);
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
                "Qəbzləri skan et, vaxta qənaət et" // <-- YENİ
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

            // Animate numbers
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