package com.data;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.DiscountMarket.service.PriceComparisonService;
import com.airbnb.lottie.LottieAnimationView;
import com.data.list.ProductListAdapter;
import com.data.location.LocationItem;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.main.MainActivity;
import com.ocr_service.ReceiptScannerActivity;
import com.smart_ai_sales.R;

import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


public class AddDataActivity extends AppCompatActivity {

    private static final String TAG = "ADD_DATA";

    // UI Elements
    private MaterialCardView cardBalance, cardDateTime, cardType, cardCategory,
            cardProduct, cardLocation, cardNote, cardPreview;
    private TextView tvCurrentBalance, tvBalanceChange, tvCurrentDate, tvCurrentTime,
            tvDayOfWeek, tvLocation, tvPreviewAmount, tvPreviewCategory,
            tvPreviewType, tvTotalInfo, tvPreviewProduct, tvPreviewTotal, tvOfflineStatus;
    private TextInputEditText etProductName, etAmount, etQuantity, etNote, etLiter, etKg;
    private Spinner spinnerCategory, spinnerSavedLocations, spinnerProducts;
    private ChipGroup chipGroupType;

    private NestedScrollView mainScrollView;

    private Chip chipIncome, chipExpense;
    private MaterialButton btnSave, btnGetLocation, btnCancel, btnAddLocation;
    private ImageView btnBack, ivOfflineStatus;
    private LinearProgressIndicator progressIndicator;
    private LottieAnimationView animationView;
    private FloatingActionButton fabScan;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private double liter = 0;
    private double kg = 0;
    private boolean isLocationLoaded = false;
    private String address = "Konum alınmadı";
    private List<String> savedLocations = new ArrayList<>();
    private ArrayAdapter<String> locationAdapter;

    // Firebase Lists
    private ArrayAdapter<String> productsAdapter;
    private List<ProductItem> firebaseProducts = new ArrayList<>();
    private List<LocationItem> firebaseLocations = new ArrayList<>();

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;
    private DocumentReference userRef;

    // Data
    private double currentBalance = 0.0;
    private double previousBalance = 0.0;
    private String transactionType = "expense";
    private String selectedCategory = "";
    private double amount = 0.0;
    private int quantity = 1;
    private String productName = "";
    private String note = "";

    // Formatting
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat, timeFormat, dayFormat;

    // Categories
    private final String[] expenseCategories = {
            "🛒 Ərzaq", "🚗 Nəqliyyat", "🏠 Kirayə", "💡 Kommunal",
            "🎮 Əyləncə", "🏥 Səhiyyə", "📚 Təhsil", "👕 Geyim",
            "📱 Texnologiya", "🍽️ Restoran", "✈️ Səyahət", "💼 Digər"
    };

    private final String[] incomeCategories = {
            "💰 Maaş", "💼 Bonus", "📈 İnvestisiya", "🛍️ Satış",
            "🎁 Hədiyyə", "💵 Freelance", "🏢 Biznes", "📊 Digər"
    };

    // Validation states
    private boolean isProductNameValid = false;
    private boolean isAmountValid = false;
    private boolean isCategoryValid = false;

    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private long lastAdShownTime = 0;
    private final long AD_INTERVAL = 60000;

    // Offline support
    private SQLiteDatabase localDB;
    private LocalDatabaseHelper dbHelper;
    private static final String PENDING_TRANSACTIONS_TABLE = "pending_transactions";
    private static final String LOCAL_TRANSACTIONS_TABLE = "local_transactions";
    private static final String LOCATIONS_TABLE = "saved_locations";
    private Handler syncHandler = new Handler();
    private Runnable syncRunnable;
    private boolean isOfflineMode = false;

    // RecyclerView və Adapter
    private RecyclerView recyclerViewProducts;
    private ProductListAdapter productListAdapter;
    private TextView tvSelectedCount, tvTotalSelectedAmount;
    private MaterialButton btnAddToList;
    private List<ProductItem> tempProductList = new ArrayList<>();
    private boolean isBulkTransaction = false;

    // Local Database Helper
    private static class LocalDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "SmartSalesLocal.db";
        private static final int DATABASE_VERSION = 2;

        LocalDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createPendingTable = "CREATE TABLE IF NOT EXISTS " + PENDING_TRANSACTIONS_TABLE + " ("
                    + "id TEXT PRIMARY KEY, "
                    + "userId TEXT, "
                    + "type TEXT, "
                    + "category TEXT, "
                    + "productName TEXT, "
                    + "amount REAL, "
                    + "quantity INTEGER, "
                    + "totalAmount REAL, "
                    + "note TEXT, "
                    + "date LONG, "
                    + "dateString TEXT, "
                    + "timeString TEXT, "
                    + "location TEXT, "
                    + "latitude REAL, "
                    + "longitude REAL, "
                    + "balanceBefore REAL, "
                    + "balanceAfter REAL, "
                    + "createdAt LONG)";
            db.execSQL(createPendingTable);

            String createLocalTable = "CREATE TABLE IF NOT EXISTS " + LOCAL_TRANSACTIONS_TABLE + " ("
                    + "id TEXT PRIMARY KEY, "
                    + "userId TEXT, "
                    + "type TEXT, "
                    + "category TEXT, "
                    + "productName TEXT, "
                    + "amount REAL, "
                    + "quantity INTEGER, "
                    + "totalAmount REAL, "
                    + "note TEXT, "
                    + "date LONG, "
                    + "dateString TEXT, "
                    + "timeString TEXT, "
                    + "location TEXT, "
                    + "latitude REAL, "
                    + "longitude REAL, "
                    + "balanceBefore REAL, "
                    + "balanceAfter REAL, "
                    + "syncedAt LONG)";
            db.execSQL(createLocalTable);

            String createLocationsTable = "CREATE TABLE IF NOT EXISTS " + LOCATIONS_TABLE + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "userId TEXT, "
                    + "address TEXT, "
                    + "latitude REAL, "
                    + "longitude REAL, "
                    + "lastUsed LONG, "
                    + "useCount INTEGER DEFAULT 1)";
            db.execSQL(createLocationsTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + LOCATIONS_TABLE + " ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "userId TEXT, "
                        + "address TEXT, "
                        + "latitude REAL, "
                        + "longitude REAL, "
                        + "lastUsed LONG, "
                        + "useCount INTEGER DEFAULT 1)");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_data);

        initializeLocalDatabase();
        initializeFirebase();
        initializeLocation();
        initializeFormatters();
        initViews();
        setupListeners();
        loadCurrentBalance();
        loadProductsFromFirebase();
        loadLocationsFromFirebase();
        setCurrentDateTime();
        setupCategories();
        setupTextWatchers();
        loadSavedLocations();
        animateViews();
        checkConnectivityAndSync();
        startConnectivityMonitoring();

        loadInterstitialAd();
        showInterstitialAd();

        if (getIntent() != null && getIntent().getBooleanExtra("from_ocr", false)) {
            // Bir az gözlə ki, view-lər hazır olsun
            new Handler().postDelayed(() -> {
                handleOCRDataFromIntent();
            }, 500);
        }

    }

    private void handleOCRDataFromIntent() {
        Intent intent = getIntent();

        String storeName = intent.getStringExtra("store_name");
        String date = intent.getStringExtra("date");
        String time = intent.getStringExtra("time");
        double totalAmount = intent.getDoubleExtra("total_amount", 0);
        String docId = intent.getStringExtra("doc_id");
        String fiscalCode = intent.getStringExtra("fiscal_code");

        ArrayList<String> productNames = intent.getStringArrayListExtra("product_names");
        ArrayList<String> productPrices = intent.getStringArrayListExtra("product_prices");
        ArrayList<String> productQuantities = intent.getStringArrayListExtra("product_quantities");
        ArrayList<String> productUnits = intent.getStringArrayListExtra("product_units");

        Log.d(TAG, "📱 OCR məlumatları intent-dən alındı");

        // Tarix və saatı yenilə
        if (date != null && !date.isEmpty() && !date.equals("Mağaza adı tapılmadı")) {
            tvCurrentDate.setText(date);
        }
        if (time != null && !time.isEmpty()) {
            tvCurrentTime.setText(time);
        }

        // Məhsulları listə əlavə et
        if (productNames != null && !productNames.isEmpty()) {
            if (productListAdapter.getItemCount() > 0) {
                showProductMergeDialog(storeName, date, time, totalAmount, docId, fiscalCode,
                        productNames, productPrices, productQuantities, productUnits);
            } else {
                addOCRProductsToList(productNames, productPrices, productQuantities,
                        productUnits, storeName, docId, fiscalCode);
            }
        } else {
            Toast.makeText(this, "⚠️ OCR-dan məhsul gəlmədi", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeLocalDatabase() {
        dbHelper = new LocalDatabaseHelper(this);
        localDB = dbHelper.getWritableDatabase();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
            userRef = db.collection("users").document(userId);
        } else {
            Toast.makeText(this, "İstifadəçi tapılmadı", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initializeFormatters() {
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("az", "AZ"));
        currencyFormat.setMaximumFractionDigits(2);

        dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("az"));
        timeFormat = new SimpleDateFormat("HH:mm", new Locale("az"));
        dayFormat = new SimpleDateFormat("EEEE", new Locale("az"));
    }

    private void initViews() {
        // Cards
        cardBalance = findViewById(R.id.cardBalance);
        cardDateTime = findViewById(R.id.cardDateTime);
        cardType = findViewById(R.id.cardType);
        cardCategory = findViewById(R.id.cardCategory);
        cardLocation = findViewById(R.id.cardLocation);
        cardNote = findViewById(R.id.cardNote);
        cardPreview = findViewById(R.id.cardPreview);
        cardProduct = findViewById(R.id.cardProduct);

        mainScrollView = findViewById(R.id.nestedScrollView); // və ya ScrollView-in id-si nədirsə

        // TextViews
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        tvBalanceChange = findViewById(R.id.tvBalanceChange);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvDayOfWeek = findViewById(R.id.tvDayOfWeek);
        tvLocation = findViewById(R.id.tvLocation);
        tvPreviewAmount = findViewById(R.id.tvPreviewAmount);
        tvPreviewCategory = findViewById(R.id.tvPreviewCategory);
        tvPreviewType = findViewById(R.id.tvPreviewType);
        tvPreviewProduct = findViewById(R.id.tvPreviewProduct);
        tvPreviewTotal = findViewById(R.id.tvPreviewTotal);
        tvTotalInfo = findViewById(R.id.tvTotalInfo);

        // Offline status views
        tvOfflineStatus = findViewById(R.id.tvOfflineStatus);
        ivOfflineStatus = findViewById(R.id.ivOfflineStatus);

        // EditTexts
        etAmount = findViewById(R.id.etAmount);
        etQuantity = findViewById(R.id.etQuantity);
        etNote = findViewById(R.id.etNote);
        etProductName = findViewById(R.id.etProductName);
        etLiter = findViewById(R.id.etLiter);
        etKg = findViewById(R.id.etKg);

        // Spinners
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerSavedLocations = findViewById(R.id.spinnerSavedLocations);
        spinnerProducts = findViewById(R.id.spinnerProducts);

        // ChipGroup
        chipGroupType = findViewById(R.id.chipGroupType);
        chipIncome = findViewById(R.id.chipIncome);
        chipExpense = findViewById(R.id.chipExpense);

        // Buttons
        btnSave = findViewById(R.id.btnSave);
        btnAddLocation = findViewById(R.id.btnAddLocation);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnCancel = findViewById(R.id.btnCancel);
        btnBack = findViewById(R.id.btnBack);
        fabScan = findViewById(R.id.fabScan);


        // Chip-lərin ilkin vəziyyəti
        chipExpense.setChecked(true);
        chipExpense.setChipBackgroundColorResource(R.color.expense_red);
        chipExpense.setChipStrokeWidth(0);
        chipExpense.setClickable(true);
        chipExpense.setFocusable(true);
        chipExpense.setCheckable(true);

        chipIncome.setChipBackgroundColorResource(R.color.card_dark);
        chipIncome.setChipStrokeColorResource(R.color.income_green);
        chipIncome.setChipStrokeWidth(2);
        chipIncome.setClickable(true);
        chipIncome.setFocusable(true);
        chipIncome.setCheckable(true);

        // Progress
        progressIndicator = findViewById(R.id.progressIndicator);

        // Animation
        animationView = findViewById(R.id.animationView);

        // Default quantity
        etQuantity.setText("1");

        // Yeni view-lər
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        tvTotalSelectedAmount = findViewById(R.id.tvTotalSelectedAmount);
        btnAddToList = findViewById(R.id.btnAddToList);

        // RecyclerView setup
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        productListAdapter = new ProductListAdapter(this, new ProductListAdapter.OnProductSelectedListener() {
            @Override
            public void onProductSelected(ProductItem product, boolean isSelected) {
                // Məhsul seçildikdə
                updateSelectedProductsInfo();
            }

            @Override
            public void onSelectionChanged(int selectedCount, double totalAmount) {
                tvSelectedCount.setText(selectedCount + " məhsul seçilib");
                tvTotalSelectedAmount.setText("Seçilmiş ümumi: " + currencyFormat.format(totalAmount));

                // Save düyməsini aktiv/deaktiv et
                btnSave.setEnabled(selectedCount > 0 && isCategoryValid);
            }
        });
        recyclerViewProducts.setAdapter(productListAdapter);

        // btnBack-i tap


        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(AddDataActivity.this, com.main.MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        mAdView = findViewById(R.id.adView3);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        updateSaveButtonState();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnCancel.setOnClickListener(v -> showExitConfirmationDialog());
        btnAddToList.setOnClickListener(v -> addCurrentProductToList());
        // Logout button listener
        ImageView btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        chipGroupType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipIncome)) {
                transactionType = "income";
                // Gəlir seçildikdə
                chipIncome.setChipBackgroundColorResource(R.color.income_green);
                chipIncome.setChipStrokeWidth(0);
                chipExpense.setChipBackgroundColorResource(R.color.card_dark);
                chipExpense.setChipStrokeColorResource(R.color.expense_red);
                chipExpense.setChipStrokeWidth(2);
                Log.d(TAG, "Income selected");
            } else if (checkedIds.contains(R.id.chipExpense)) {
                transactionType = "expense";
                // Xərc seçildikdə
                chipExpense.setChipBackgroundColorResource(R.color.expense_red);
                chipExpense.setChipStrokeWidth(0);
                chipIncome.setChipBackgroundColorResource(R.color.card_dark);
                chipIncome.setChipStrokeColorResource(R.color.income_green);
                chipIncome.setChipStrokeWidth(2);
                Log.d(TAG, "Expense selected");
            }
            updateCategorySpinner();
            updatePreview();
            animateBalanceChange();
        });

        btnGetLocation.setOnClickListener(v -> getCurrentLocation());

        btnSave.setOnClickListener(v -> saveTransaction());

        // AddDataActivity-də fabScan klik hadisəsi
        fabScan.setOnClickListener(v -> {
            Intent intent = new Intent(AddDataActivity.this, ReceiptScannerActivity.class);

            // Əgər userId lazımdırsa göndər
            intent.putExtra("userId", userId);

            startActivityForResult(intent, 1001);

            // Animasiya
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        btnAddLocation.setOnClickListener(v -> showAddLocationDialog());

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
                isCategoryValid = !selectedCategory.isEmpty();
                updatePreview();
                updateSaveButtonState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                isCategoryValid = false;
                updateSaveButtonState();
            }
        });

        spinnerSavedLocations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String selectedLocation = parent.getItemAtPosition(position).toString();
                    useSavedLocation(selectedLocation);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerProducts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0 && !firebaseProducts.isEmpty()) {
                    ProductItem selectedProduct = firebaseProducts.get(position - 1);
                    etProductName.setText(selectedProduct.getName());
                    etQuantity.setText(String.valueOf(selectedProduct.getQuantity()));
                    etAmount.setText(String.valueOf(selectedProduct.getPrice()));
                    etKg.setText(String.valueOf(selectedProduct.getKg()));
                    etLiter.setText(String.valueOf(selectedProduct.getLiter()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        etLiter.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    liter = s.toString().isEmpty() ? 0 : Double.parseDouble(s.toString());
                } catch (NumberFormatException e) {
                    liter = 0;
                }
            }
        });

        etKg.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    kg = s.toString().isEmpty() ? 0 : Double.parseDouble(s.toString());
                } catch (NumberFormatException e) {
                    kg = 0;
                }
            }
        });
    }

    private void showLogoutConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🚪 Çıxış")
                .setMessage("Hesabdan çıxmaq istədiyinizə əminsiniz?")
                .setPositiveButton("Bəli", (dialog, which) -> {
                    // Firebase-dən çıxış et
                    FirebaseAuth.getInstance().signOut();

                    // Login activity-ə yönləndir
                    Intent intent = new Intent(AddDataActivity.this, com.authentication.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                    Toast.makeText(this, "Çıxış edildi", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Xeyr", null)
                .show();
    }
    // Cari məhsulu listə əlavə et

    private void addCurrentProductToList() {
        // Inputları yoxla
        if (!validateProductInput()) {
            return;
        }

        // Məhsul məlumatlarını al - FINAL dəyişənlər
        final String name = etProductName.getText().toString().trim();

        final String priceText = etAmount.getText().toString().trim();
        final double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            return; // Əgər qiymət parse olunmursa, funksiyadan çıx
        }

        final int quantity;
        try {
            quantity = Integer.parseInt(etQuantity.getText().toString());
        } catch (NumberFormatException e) {
            return;
        }

        final double kg;
        try {
            kg = Double.parseDouble(etKg.getText().toString());
        } catch (NumberFormatException e) {
            return;
        }

        final double liter;
        try {
            liter = Double.parseDouble(etLiter.getText().toString());
        } catch (NumberFormatException e) {
            return;
        }

        // DEBUG - Qiyməti yoxla
        Log.d(TAG, "=== MƏHSUL ƏLAVƏ EDİLİR ===");
        Log.d(TAG, "Məhsul adı: " + name);
        Log.d(TAG, "Qiymət (rəqəm): " + price);

        // Yeni məhsul obyekti yarat
        final ProductItem newProduct = new ProductItem();
        newProduct.setId(UUID.randomUUID().toString());
        newProduct.setName(name);
        newProduct.setPrice(price);
        newProduct.setQuantity(quantity);
        newProduct.setKg(kg);
        newProduct.setLiter(liter);
        newProduct.setUserId(userId);
        newProduct.setCreatedAt(new Timestamp(new Date()));
        newProduct.setSelected(true);

        Log.d(TAG, "Məhsul yaradıldı - price: " + newProduct.getPrice());

        // Listə əlavə et - FINAL dəyişənlər istifadə olunur
        runOnUiThread(() -> {
            productListAdapter.addProduct(newProduct);
            tempProductList.add(newProduct);

            updateSelectedProductsInfo();
            clearProductInputs();

            // FINAL price dəyişəni istifadə olunur
            Snackbar.make(cardProduct,
                    "✅ " + name + " listə əlavə edildi (" + price + " AZN)",
                    Snackbar.LENGTH_SHORT).show();

            cardPreview.setVisibility(View.GONE);
            hideKeyboard();
        });
    }

    private boolean validateProductInput() {
        String name = etProductName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etProductName.setError("Məhsul adı daxil edin");
            etProductName.requestFocus();
            return false;
        }

        // Qiyməti yoxla
        String priceText = etAmount.getText().toString().trim();
        if (TextUtils.isEmpty(priceText)) {
            etAmount.setError("Məbləğ daxil edin");
            etAmount.requestFocus();
            return false;
        }

        double price = 0;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            etAmount.setError("Düzgün məbləğ daxil edin");
            etAmount.requestFocus();
            return false;
        }

        if (price <= 0) {
            etAmount.setError("Məbləğ 0-dan böyük olmalıdır");
            etAmount.requestFocus();
            return false;
        }

        // Miqdar, kiloqram və ya litr yoxlaması
        String quantityText = etQuantity.getText().toString();
        String kgText = etKg.getText().toString();
        String literText = etLiter.getText().toString();

        boolean hasQuantity = !TextUtils.isEmpty(quantityText) && !quantityText.equals("0");
        boolean hasKg = !TextUtils.isEmpty(kgText) && !kgText.equals("0");
        boolean hasLiter = !TextUtils.isEmpty(literText) && !literText.equals("0");

        if (!hasQuantity && !hasKg && !hasLiter) {
            Snackbar.make(cardProduct, "Miqdar, kiloqram və ya litr daxil edin", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void updateSelectedProductsInfo() {
        int selectedCount = productListAdapter.getSelectedCount();
        double totalAmount = productListAdapter.getSelectedTotalAmount();

        // DEBUG - seçilmiş məhsulların cəmini yoxla
        Log.d(TAG, "Seçilmiş məhsul sayı: " + selectedCount);
        Log.d(TAG, "Seçilmiş ümumi məbləğ: " + totalAmount);

        // Seçilmiş məhsulları siyahıya al və hər birinin qiymətini yoxla
        List<ProductItem> selected = productListAdapter.getSelectedProducts();
        for (ProductItem p : selected) {
            Log.d(TAG, "  - " + p.getName() + ": " + p.getPrice() + " AZN x " +
                    (p.getQuantity() > 0 ? p.getQuantity() + " ədəd" :
                            p.getKg() > 0 ? p.getKg() + " kq" : p.getLiter() + " L") +
                    " = " + p.getTotalAmount() + " AZN");
        }

        tvSelectedCount.setText(selectedCount + " məhsul seçilib");
        tvTotalSelectedAmount.setText("Seçilmiş ümumi: " + currencyFormat.format(totalAmount));

        // Save düyməsini aktiv/deaktiv et
        btnSave.setEnabled(selectedCount > 0 && isCategoryValid);
    }

    private void clearProductInputs() {
        etProductName.setText("");
        etAmount.setText("");
        etQuantity.setText("1");
        etKg.setText("0");
        etLiter.setText("0");

        // Spinner-i default vəziyyətə qaytar
        if (spinnerProducts != null) {
            spinnerProducts.setSelection(0);
        }
    }

    // Note-dan mağaza adını çıxar
    private String extractStoreNameFromNote() {
        if (note == null || note.isEmpty()) return "";

        // Sadə regex ilə mağaza adını tapmağa çalış
        String[] lines = note.split("\n");
        for (String line : lines) {
            if (line.contains("🏪")) {
                return line.replace("🏪", "").trim();
            }
        }
        return "";
    }

    // Klaviaturanı bağla
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void loadProductsFromFirebase() {
        firebaseProducts.clear();
        Log.d(TAG, "Loading products for user: " + userId);

        db.collection("products")
                .whereEqualTo("userId", userId)
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Products loaded: " + queryDocumentSnapshots.size());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ProductItem item = new ProductItem();
                        item.setId(doc.getId());
                        item.setName(doc.getString("name"));
                        item.setCategory(doc.getString("category"));

                        Double kg = doc.getDouble("kg");
                        Double liter = doc.getDouble("liter");
                        Double price = doc.getDouble("price");

                        item.setKg(kg != null ? kg : 0);
                        item.setLiter(liter != null ? liter : 0);
                        item.setPrice(price != null ? price : 0);
                        item.setUserId(doc.getString("userId"));
                        item.setCreatedAt(doc.getTimestamp("createdAt"));

                        firebaseProducts.add(item);
                    }

                    updateProductsSpinner();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading products", e);
                    List<String> errorList = new ArrayList<>();
                    errorList.add("❌ Yükləmə xətası");

                    productsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, errorList);
                    spinnerProducts.setAdapter(productsAdapter);
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "🎯 OCR tamamlandı, məlumatlar alındı");

            // OCR-dan gələn məlumatları al
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

            // Məlumatları göstər
            if (productNames != null && !productNames.isEmpty()) {
                showOCRData(storeName, date, time, totalAmount, docId, fiscalCode,
                        productNames, productPrices, productQuantities, productUnits);
            } else {
                Toast.makeText(this, "⚠️ OCR-dan məhsul gəlmədi", Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void showOCRData(String storeName, String date, String time, double totalAmount,
                             String docId, String fiscalCode,
                             ArrayList<String> productNames,
                             ArrayList<String> productPrices,
                             ArrayList<String> productQuantities,
                             ArrayList<String> productUnits) {

        // Tarix və saatı yenilə
        if (date != null && !date.isEmpty() && !date.equals("Mağaza adı tapılmadı")) {
            tvCurrentDate.setText(date);
        }
        if (time != null && !time.isEmpty()) {
            tvCurrentTime.setText(time);
        }

        // Əgər məhsul varsa
        if (productNames != null && !productNames.isEmpty()) {

            // Əvvəlki məhsulları təmizləmək istəyir?
            boolean shouldClear = productListAdapter.getItemCount() > 0;

            if (shouldClear) {
                // Dialog göstər - StringBuilder parametri YOXDUR!
                showProductMergeDialog(storeName, date, time, totalAmount, docId, fiscalCode,
                        productNames, productPrices, productQuantities, productUnits); // <-- StringBuilder YOX
            } else {
                // List boşdursa birbaşa əlavə et - StringBuilder parametri YOXDUR!
                addOCRProductsToList(productNames, productPrices, productQuantities,
                        productUnits, storeName, docId, fiscalCode); // <-- StringBuilder YOX
            }
        } else {
            Toast.makeText(this, "⚠️ OCR-dan məhsul məlumatı gəlmədi", Toast.LENGTH_SHORT).show();
        }
    }



    private void showProductMergeDialog(String storeName, String date, String time,
                                        double totalAmount, String docId, String fiscalCode,
                                        ArrayList<String> productNames,
                                        ArrayList<String> productPrices,
                                        ArrayList<String> productQuantities,
                                        ArrayList<String> productUnits) {  // StringBuilder yox!

        int existingCount = productListAdapter.getItemCount();
        int newCount = productNames.size();

        new MaterialAlertDialogBuilder(this)
                .setTitle("📋 Məhsulları əlavə et")
                .setMessage(String.format(
                        "Mövcud listdə %d məhsul var.\nOCR-dan %d yeni məhsul gəldi.\n\nNecə əlavə etmək istəyirsiniz?",
                        existingCount, newCount))
                .setPositiveButton("Listə əlavə et", (dialog, which) -> {
                    addOCRProductsToList(productNames, productPrices, productQuantities,
                            productUnits, storeName, docId, fiscalCode);
                })
                .setNegativeButton("Listi təmizlə", (dialog, which) -> {
                    productListAdapter.clearProducts();
                    tempProductList.clear();
                    addOCRProductsToList(productNames, productPrices, productQuantities,
                            productUnits, storeName, docId, fiscalCode);
                })
                .setNeutralButton("Ləğv et", null)
                .show();
    }

    private void addOCRProductsToList(ArrayList<String> productNames,
                                      ArrayList<String> productPrices,
                                      ArrayList<String> productQuantities,
                                      ArrayList<String> productUnits,
                                      String storeName, String docId, String fiscalCode) {

        Log.d(TAG, "=== OCR MƏHSULLARI LİSTƏ ƏLAVƏ EDİLİR ===");
        Log.d(TAG, "Store: " + storeName);
        Log.d(TAG, "Receipt ID: " + docId);
        Log.d(TAG, "Fiscal Code: " + fiscalCode);

        if (productNames == null || productNames.isEmpty()) {
            Log.e(TAG, "OCR-dan məhsul gəlmədi!");
            Toast.makeText(this, "OCR-dan məhsul məlumatı gəlmədi", Toast.LENGTH_SHORT).show();
            return;
        }

        // OCR-dan gələn ÜMUMİ tarix (əgər varsa)
        Timestamp purchaseDate = null;
        String ocrDate = getIntent().getStringExtra("date");
        if (ocrDate != null && !ocrDate.isEmpty() && !ocrDate.equals("Mağaza adı tapılmadı")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("az"));
                Date date = sdf.parse(ocrDate);
                purchaseDate = new Timestamp(date);
                Log.d(TAG, "📅 OCR Tarix: " + ocrDate + " -> " + purchaseDate);
            } catch (Exception e) {
                Log.e(TAG, "Date parse error: " + e.getMessage());
            }
        }

        // ⭐ AtomicInteger istifadə edək (lambda üçün)
        AtomicInteger addedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        StringBuilder noteBuilder = new StringBuilder();
        if (storeName != null && !storeName.isEmpty() && !storeName.equals("Mağaza adı tapılmadı")) {
            noteBuilder.append("🏪 ").append(storeName);
            if (docId != null && !docId.isEmpty()) {
                noteBuilder.append(" (№").append(docId).append(")");
            }
            noteBuilder.append("\n");
        }

        for (int i = 0; i < productNames.size(); i++) {
            try {
                String name = productNames.get(i);
                if (name == null || name.trim().isEmpty()) {
                    errorCount.incrementAndGet();
                    continue;
                }
                name = name.trim();

                double price = 0;
                if (productPrices != null && i < productPrices.size()) {
                    String priceStr = productPrices.get(i);
                    if (priceStr != null && !priceStr.trim().isEmpty()) {
                        priceStr = priceStr.trim()
                                .replace("AZN", "")
                                .replace("₼", "")
                                .replace(" ", "")
                                .replace(",", ".");
                        StringBuilder cleanPrice = new StringBuilder();
                        for (char c : priceStr.toCharArray()) {
                            if (Character.isDigit(c) || c == '.') {
                                cleanPrice.append(c);
                            }
                        }
                        priceStr = cleanPrice.toString();
                        try {
                            if (!priceStr.isEmpty()) {
                                price = Double.parseDouble(priceStr);
                            }
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Price parse error: " + e.getMessage());
                        }
                    }
                }

                double quantity = 1;
                if (productQuantities != null && i < productQuantities.size()) {
                    String qtyStr = productQuantities.get(i);
                    if (qtyStr != null && !qtyStr.trim().isEmpty()) {
                        qtyStr = qtyStr.trim().replace(",", ".");
                        try {
                            quantity = Double.parseDouble(qtyStr);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Quantity parse error: " + e.getMessage());
                        }
                    }
                }

                String unit = "ədəd";
                if (productUnits != null && i < productUnits.size()) {
                    unit = productUnits.get(i);
                    if (unit == null || unit.trim().isEmpty()) unit = "ədəd";
                    unit = unit.trim().toLowerCase();
                }

                Log.d(TAG, "Məhsul " + i + ": " + name + ", qiymət: " + price + ", miqdar: " + quantity + ", vahid: " + unit);

                if (price <= 0) {
                    Log.w(TAG, "Məhsul " + name + " üçün qiymət 0 və ya mənfidir!");
                }

                ProductItem product = new ProductItem();
                product.setId(UUID.randomUUID().toString());
                product.setName(name);
                product.setPrice(price);

                // HƏR MƏHSULUN ÖZ MƏLUMATLARI
                product.setStoreName(storeName);
                product.setReceiptId(docId);
                product.setFiscalCode(fiscalCode);
                product.setPurchaseDate(purchaseDate != null ? purchaseDate.toDate() : null);
                product.setBarcode(null);
                product.setTaxAmount(0);
                product.setTaxFree(false);

                product.setUserId(userId);
                product.setCreatedAt(new Timestamp(new Date()));
                product.setSelected(true);

                if (unit.contains("kg") || unit.contains("kq") || unit.contains("kilo") || quantity != Math.floor(quantity)) {
                    product.setKg(quantity);
                    product.setQuantity(0);
                    product.setLiter(0);
                } else if (unit.contains("l") || unit.contains("litr") || unit.contains("lt")) {
                    product.setLiter(quantity);
                    product.setQuantity(0);
                    product.setKg(0);
                } else {
                    product.setQuantity((int) Math.round(quantity));
                    product.setKg(0);
                    product.setLiter(0);
                }

                final ProductItem finalProduct = ProductItem.copyProduct(product);

                runOnUiThread(() -> {
                    productListAdapter.addProduct(finalProduct);
                    tempProductList.add(finalProduct);
                });

                double productTotal = product.getTotalAmount();
                if (product.getKg() > 0) {
                    noteBuilder.append(String.format(Locale.getDefault(),
                            "• %s - %.3f kq x %.2f AZN = %.2f AZN\n",
                            name, product.getKg(), price, productTotal));
                } else if (product.getLiter() > 0) {
                    noteBuilder.append(String.format(Locale.getDefault(),
                            "• %s - %.3f L x %.2f AZN = %.2f AZN\n",
                            name, product.getLiter(), price, productTotal));
                } else {
                    noteBuilder.append(String.format(Locale.getDefault(),
                            "• %s - %d ədəd x %.2f AZN = %.2f AZN\n",
                            name, product.getQuantity(), price, productTotal));
                }

                addedCount.incrementAndGet();

            } catch (Exception e) {
                Log.e(TAG, "Məhsul " + i + " əlavə edilərkən xəta: " + e.getMessage());
                errorCount.incrementAndGet();
            }
        }

        final String finalNoteText = noteBuilder.toString().trim();

        // ⭐ final dəyişənlər yaradırıq (AtomicInteger-dən dəyərləri alırıq)
        final int finalAddedCount = addedCount.get();
        final int finalErrorCount = errorCount.get();

        runOnUiThread(() -> {
            if (!finalNoteText.isEmpty()) {
                String currentNote = etNote.getText().toString();
                if (currentNote.isEmpty()) {
                    etNote.setText(finalNoteText);
                    note = finalNoteText;
                } else {
                    etNote.setText(currentNote + "\n\n" + finalNoteText);
                    note = currentNote + "\n\n" + finalNoteText;
                }
            }
            updateSelectedProductsInfo();

            String message = String.format("✅ %d məhsul OCR-dan listə əlavə edildi", finalAddedCount);
            if (finalErrorCount > 0) {
                message += String.format(" (%d xəta)", finalErrorCount);
            }
            Toast.makeText(AddDataActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    // Product-i kopyalamaq üçün köməkçi metod
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
        copy.setTotalAmount(original.getTotalAmount());
        copy.setTaxAmount(original.getTaxAmount());
        copy.setTaxFree(original.isTaxFree());
        copy.setFiscalCode(original.getFiscalCode());
        copy.setBarcode(original.getBarcode());
        return copy;
    }

    private void loadLocationsFromFirebase() {
        Log.d(TAG, "Loading locations from Firebase");

        db.collection("locations")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Loaded " + queryDocumentSnapshots.size() + " locations");

                    savedLocations.clear();
                    savedLocations.add("🆕 Yeni konum al");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String address = doc.getString("address");
                        if (address != null) {
                            savedLocations.add(address);
                        }
                    }

                    // Spinner-i yenilə
                    locationAdapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, savedLocations);
                    locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSavedLocations.setAdapter(locationAdapter);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading locations: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    private void saveLocationToFirebase(String name, String address, double lat, double lng) {
        Log.d(TAG, "Saving location: " + address);

        Map<String, Object> location = new HashMap<>();
        location.put("userId", userId);
        location.put("address", address);
        location.put("timestamp", System.currentTimeMillis());

        db.collection("locations")
                .add(location)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✅ Location saved: " + documentReference.getId());

                    runOnUiThread(() -> {
                        Toast.makeText(this, "✅ Konum əlavə edildi", Toast.LENGTH_SHORT).show();
                        loadLocationsFromFirebase(); // Yenidən yüklə
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firebase error: " + e.getMessage());
                    e.printStackTrace();

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Firebase xətası: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                });
    }



    private void setupTextWatchers() {
        etProductName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                productName = s != null ? s.toString().trim() : "";
                isProductNameValid = !TextUtils.isEmpty(productName);
                updatePreview();
                updateSaveButtonState();
            }
        });

        etAmount.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    amount = s.toString().isEmpty() ? 0 : Double.parseDouble(s.toString());
                    isAmountValid = amount > 0;

                    TextInputLayout amountLayout = (TextInputLayout) etAmount.getParent().getParent();
                    if (!isAmountValid && !s.toString().isEmpty()) {
                        amountLayout.setError("Məbləğ 0-dan böyük olmalıdır");
                    } else {
                        amountLayout.setError(null);
                    }
                } catch (NumberFormatException e) {
                    amount = 0;
                    isAmountValid = false;
                }
                updatePreview();
                updateSaveButtonState();
            }
        });

        etQuantity.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    quantity = s.toString().isEmpty() ? 1 : Integer.parseInt(s.toString());
                    if (quantity < 1) {
                        quantity = 1;
                        etQuantity.setText("1");
                    }
                } catch (NumberFormatException e) {
                    quantity = 1;
                }
                updatePreview();
            }
        });

        etNote.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                note = s.toString();
            }
        });
    }

    private void updateSaveButtonState() {
        btnSave.setEnabled(isProductNameValid && isAmountValid && isCategoryValid);
    }

    private void setupCategories() {
        updateCategorySpinner();
    }

    private void updateCategorySpinner() {
        String[] categories = transactionType.equals("expense") ? getExpenseCategories() : getIncomeCategories();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView textView;
                if (convertView == null) {
                    textView = (TextView) getLayoutInflater().inflate(android.R.layout.simple_spinner_item, parent, false);
                } else {
                    textView = (TextView) convertView;
                }
                textView.setText(getItem(position));
                textView.setTextColor(ContextCompat.getColor(AddDataActivity.this, R.color.white));
                textView.setPadding(20, 16, 20, 16);
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView textView;
                if (convertView == null) {
                    textView = (TextView) getLayoutInflater().inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
                } else {
                    textView = (TextView) convertView;
                }
                textView.setText(getItem(position));
                textView.setTextColor(ContextCompat.getColor(AddDataActivity.this, R.color.white));
                textView.setBackgroundColor(ContextCompat.getColor(AddDataActivity.this, R.color.card_dark));
                textView.setPadding(20, 16, 20, 16);
                return textView;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        if (categories.length > 0) {
            spinnerCategory.setSelection(0);
            selectedCategory = categories[0];
            isCategoryValid = true;
        }
    }

    // Əvvəlki hardcoded array-ləri SİLİN və bunları əlavə edin:

    private String[] getExpenseCategories() {
        return new String[]{
                getString(R.string.category_grocery),
                getString(R.string.category_transport),
                getString(R.string.category_rent),
                getString(R.string.category_utilities),
                getString(R.string.category_entertainment),
                getString(R.string.category_health),
                getString(R.string.category_education),
                getString(R.string.category_clothing),
                getString(R.string.category_technology),
                getString(R.string.category_restaurant),
                getString(R.string.category_travel),
                getString(R.string.category_other)
        };
    }

    private String[] getIncomeCategories() {
        return new String[]{
                getString(R.string.category_salary),
                getString(R.string.category_bonus),
                getString(R.string.category_investment),
                getString(R.string.category_sale),
                getString(R.string.category_gift),
                getString(R.string.category_freelance),
                getString(R.string.category_business),
                getString(R.string.category_other)
        };
    }

    private void loadSavedLocations() {
        savedLocations.clear();
        savedLocations.add("🆕 Yeni konum al");

        Cursor cursor = localDB.query(LOCATIONS_TABLE,
                new String[]{"address"},
                "userId = ?",
                new String[]{userId},
                null, null,
                "lastUsed DESC, useCount DESC",
                "10");

        while (cursor.moveToNext()) {
            int addressIndex = cursor.getColumnIndex("address");
            if (addressIndex != -1) {
                String location = cursor.getString(addressIndex);
                if (!savedLocations.contains(location)) {
                    savedLocations.add(location);
                }
            }
        }
        cursor.close();

        locationAdapter = new ArrayAdapter<>(this, R.layout.item_spinner, savedLocations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSavedLocations.setAdapter(locationAdapter);
    }

    private void saveLocationToDatabase(String address, double lat, double lng) {
        saveLocationToFirebase("", address, lat, lng);
    }

    private void showAddLocationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("📌 Yeni Konum Əlavə Et");

        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Məsələn: Bazarstore, Bravo, Sederek...");
        input.setTextColor(ContextCompat.getColor(this, R.color.white));
        input.setHintTextColor(ContextCompat.getColor(this, R.color.white_60));

        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setPadding(50, 20, 50, 20);
        inputLayout.addView(input);
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setBoxStrokeColor(ContextCompat.getColor(this, R.color.purple_500));
        inputLayout.setHintTextColor(ContextCompat.getColorStateList(this, R.color.purple_500));

        builder.setView(inputLayout);

        builder.setPositiveButton("Əlavə Et", (dialog, which) -> {
            String locationName = input.getText().toString().trim();
            if (!TextUtils.isEmpty(locationName)) {
                addCustomLocation(locationName);
            } else {
                Toast.makeText(AddDataActivity.this, "Konum adı daxil edin", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Ləğv et", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addCustomLocation(String locationName) {
        Log.d(TAG, "Adding custom location: " + locationName);

        address = locationName;
        latitude = 0.0;
        longitude = 0.0;
        isLocationLoaded = true;

        // TextView-i yenilə
        tvLocation.setText(locationName);

        // Firebase-ə yadda saxla
        saveLocationToFirebase(locationName, locationName, 0.0, 0.0);
    }

    private void useSavedLocation(String selectedLocation) {
        for (LocationItem item : firebaseLocations) {
            String display = item.getName() != null && !item.getName().isEmpty()
                    ? item.getName() + " - " + item.getAddress()
                    : item.getAddress();

            if (display.equals(selectedLocation)) {
                address = item.getAddress();
                latitude = item.getLatitude();
                longitude = item.getLongitude();
                isLocationLoaded = true;

                tvLocation.setText(address);
                Snackbar.make(cardLocation, "Konum seçildi: " + address, Snackbar.LENGTH_SHORT).show();
                return;
            }
        }
    }

    // AddDataActivity.java - OCR məlumatları əlavə edildikdən SONRA

    private void checkPriceInBazarStore(ProductItem product) {
        PriceComparisonService comparisonService = new PriceComparisonService(this);

        // Sadəcə bu məhsul üçün yoxlama
        comparisonService.comparePrices(new PriceComparisonService.ComparisonCallback() {
            @Override
            public void onComparisonComplete(List<PriceComparisonService.ComparisonResult> results) {
                for (PriceComparisonService.ComparisonResult r : results) {
                    if (r.getLocalProduct().getName().equalsIgnoreCase(product.getName())) {
                        // Bu məhsul BazarStore-da ucuzdur!
                        showBazarStoreSuggestion(r);
                        break;
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "BazarStore yoxlaması xətası: " + error);
            }

            @Override
            public void onProgress(String message) {
                // İsteğe bağlı
            }
        });
    }

    private void showBazarStoreSuggestion(PriceComparisonService.ComparisonResult result) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🛍️ BazarStore-da UCUZ!")
                .setIcon(R.drawable.ic_discount)
                .setMessage(String.format(
                        "✅ %s məhsulu BazarStore-da DAHA UCUZDUR!\n\n" +
                                "📍 Sizin qiymət: ₼%.2f\n" +
                                "🏪 BazarStore: ₼%.2f\n" +
                                "💰 Qənaət: ₼%.2f (%.0f%%)\n\n" +
                                "Endirimli linkə keçmək istəyirsiniz?",
                        result.getLocalProduct().getName(),
                        result.getLocalPrice(),
                        result.getBazarPrice(),
                        result.getSavings(),
                        result.getSavingsPercent()))
                .setPositiveButton("BazarStore-a keç", (dialog, which) -> {
                    if (result.getBazarProduct().getProductUrl() != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(result.getBazarProduct().getProductUrl()));
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Link tapılmadı", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Sonra", null)
                .show();
    }

    private void loadCurrentBalance() {
        showLoading(true);

        userRef.get().addOnCompleteListener(task -> {
            showLoading(false);

            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Double balance = document.getDouble("currentBalance");
                    if (balance != null) {
                        currentBalance = balance;
                        previousBalance = balance;
                    }
                }
            } else {
                loadLocalBalance();
            }
            updateBalanceDisplay();
        });
    }

    private void loadLocalBalance() {
        Cursor cursor = localDB.rawQuery(
                "SELECT balanceAfter FROM " + LOCAL_TRANSACTIONS_TABLE +
                        " WHERE userId = ? ORDER BY date DESC LIMIT 1",
                new String[]{userId});

        if (cursor.moveToFirst()) {
            int balanceIndex = cursor.getColumnIndex("balanceAfter");
            if (balanceIndex != -1) {
                currentBalance = cursor.getDouble(balanceIndex);
                previousBalance = currentBalance;
            }
        }
        cursor.close();
    }


    private void updateBalanceDisplay() {
        tvCurrentBalance.setText(currencyFormat.format(currentBalance));

        // Əgər əvvəlki balansdan fərqlidirsə, dəyişikliyi göstər
        double difference = currentBalance - previousBalance;
        if (difference != 0) {
            String diffText = String.format("%s %s",
                    difference > 0 ? "+" : "-",
                    currencyFormat.format(Math.abs(difference)));
            tvBalanceChange.setText(diffText);
            tvBalanceChange.setTextColor(ContextCompat.getColor(this,
                    difference > 0 ? R.color.income_green : R.color.expense_red));
            tvBalanceChange.setVisibility(View.VISIBLE);

            // Animasiya
            animateBalanceChange();
        } else {
            tvBalanceChange.setVisibility(View.GONE);
        }
    }

    private void setCurrentDateTime() {
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();

        tvCurrentDate.setText(dateFormat.format(now));
        tvCurrentTime.setText(timeFormat.format(now));
        tvDayOfWeek.setText(capitalizeFirst(dayFormat.format(now)));
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        updateLocationButtonState(false, "Konum alınır...", true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> handleLocationSuccess(location))
                .addOnFailureListener(this, e -> handleLocationFailure(e));
    }

    private void handleLocationSuccess(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            getAddressFromLocation(latitude, longitude);
        } else {
            updateLocationButtonState(true, "Konum Al", false);
            Toast.makeText(AddDataActivity.this, "Konum alınamadı, GPS-i yoxlayın",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLocationFailure(Exception e) {
        updateLocationButtonState(true, "Konum Al", false);
        Toast.makeText(AddDataActivity.this, "Xəta: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
    }

    private void updateLocationButtonState(boolean enabled, String text, boolean showProgress) {
        btnGetLocation.setEnabled(enabled);
        btnGetLocation.setText(text);
        progressIndicator.setVisibility(showProgress ? View.VISIBLE : View.GONE);
    }

    private double getDoubleFromEditText(TextInputEditText editText) {
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            // Vergülü nöqtəyə çevir (azərbaycan klaviaturası üçün)
            text = text.replace(',', '.');
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Parse error: " + text);
            return 0;
        }
    }


    private void updateProductsSpinner() {
        List<String> displayList = new ArrayList<>();

        if (firebaseProducts.isEmpty()) {
            displayList.add("📋 Məhsul yoxdur");
        } else {
            displayList.add("📋 Məhsul seçin...");
            for (ProductItem item : firebaseProducts) {
                displayList.add(item.getDisplayName());
            }
        }

        productsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, displayList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(ContextCompat.getColor(AddDataActivity.this, R.color.white));
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                textView.setTextColor(ContextCompat.getColor(AddDataActivity.this, R.color.white));
                textView.setBackgroundColor(ContextCompat.getColor(AddDataActivity.this, R.color.card_dark));
                return textView;
            }
        };

        productsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProducts.setAdapter(productsAdapter);
    }

    private void getAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, new Locale("az"));

        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                runOnUiThread(() -> handleAddressResult(addresses, lat, lng));
            } catch (IOException e) {
                runOnUiThread(() -> {
                    address = String.format("%.4f, %.4f", lat, lng);
                    tvLocation.setText(address);
                    updateLocationButtonState(true, "Konum Al", false);
                    saveLocationToDatabase(address, lat, lng);
                });
            }
        }).start();
    }

    private void handleAddressResult(List<Address> addresses, double lat, double lng) {
        if (addresses != null && !addresses.isEmpty()) {
            Address addr = addresses.get(0);
            address = buildAddressString(addr);
            isLocationLoaded = true;

            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(tvLocation, "alpha", 0f, 1f);
            fadeIn.setDuration(500);
            tvLocation.setText(address);
            fadeIn.start();

            saveLocationToDatabase(address, lat, lng);
        } else {
            address = String.format("%.4f, %.4f", lat, lng);
            tvLocation.setText(address);
            saveLocationToDatabase(address, lat, lng);
        }

        updateLocationButtonState(true, "Konum Al", false);
        Snackbar.make(cardLocation, "Konum uğurla alındı", Snackbar.LENGTH_SHORT).show();
    }

    private String buildAddressString(Address addr) {
        StringBuilder sb = new StringBuilder();

        if (addr.getThoroughfare() != null)
            sb.append(addr.getThoroughfare());
        if (addr.getSubThoroughfare() != null)
            sb.append(" ").append(addr.getSubThoroughfare());
        if (addr.getLocality() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(addr.getLocality());
        }
        if (addr.getCountryName() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(addr.getCountryName());
        }

        return sb.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Konum izni tələb olunur", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updatePreview() {
        double total = amount * quantity;
        String typeText = transactionType.equals("income") ? "Gəlir" : "Xərc";
        int typeColor = transactionType.equals("income") ?
                R.color.income_green : R.color.expense_red;

        tvPreviewType.setText(typeText);
        tvPreviewType.setTextColor(ContextCompat.getColor(this, typeColor));

        tvPreviewCategory.setText(selectedCategory.isEmpty() ?
                "Kateqoriya seçilməyib" : selectedCategory);
        tvPreviewProduct.setText(productName.isEmpty() ?
                "Məhsul adı daxil edilməyib" : productName);
        tvPreviewAmount.setText(currencyFormat.format(amount));

        String totalText = String.format("Cəmi: %s", currencyFormat.format(total));
        tvPreviewTotal.setText(totalText);

        if (tvTotalInfo != null) {
            tvTotalInfo.setText(String.format("Cəmi: %s", currencyFormat.format(total)));
            tvTotalInfo.setVisibility(View.VISIBLE);
        }

        cardPreview.setVisibility(amount > 0 && !productName.isEmpty() && !selectedCategory.isEmpty()
                ? View.VISIBLE : View.GONE);
    }

    private void animateViews() {
        View[] views = {cardBalance, cardDateTime, cardType, cardCategory,
                cardProduct, cardLocation, cardNote, cardPreview};

        int delay = 0;
        for (View view : views) {
            if (view == null) continue;

            view.setAlpha(0f);
            view.setTranslationY(50f);

            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(delay)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            delay += 100;
        }

        if (animationView != null) {
            animationView.setVisibility(View.VISIBLE);
            animationView.playAnimation();

            new Handler().postDelayed(() -> {
                animationView.cancelAnimation();
                animationView.setVisibility(View.GONE);
            }, 2000);
        }
    }

    private void animateBalanceChange() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            float scale = 1f + (0.1f * animation.getAnimatedFraction());
            tvCurrentBalance.setScaleX(scale);
            tvCurrentBalance.setScaleY(scale);
        });
        animator.start();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = connectivityManager
                        .getNetworkCapabilities(connectivityManager.getActiveNetwork());
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        }
        return false;
    }

    private void checkConnectivityAndSync() {
        if (isNetworkAvailable()) {
            isOfflineMode = false;
            updateOfflineStatus(false);
            syncPendingTransactions();
        } else {
            isOfflineMode = true;
            updateOfflineStatus(true);
        }
    }

    private void updateOfflineStatus(boolean isOffline) {
        if (ivOfflineStatus != null && tvOfflineStatus != null) {
            if (isOffline) {
                ivOfflineStatus.setImageResource(R.drawable.ic_offline);
                tvOfflineStatus.setText("Offline rejim");
                tvOfflineStatus.setTextColor(Color.parseColor("#EF4444"));
                Snackbar.make(cardBalance, "Offline rejim: Məlumatlar lokalda saxlanılacaq",
                        Snackbar.LENGTH_LONG).show();
            } else {
                ivOfflineStatus.setImageResource(R.drawable.ic_online);
                tvOfflineStatus.setText("Online");
                tvOfflineStatus.setTextColor(Color.parseColor("#10B981"));
            }
        }
    }

    private void startConnectivityMonitoring() {
        syncRunnable = () -> {
            checkConnectivityAndSync();
            syncHandler.postDelayed(syncRunnable, 30000);
        };
        syncHandler.post(syncRunnable);
    }


    private void saveTransaction() {
        // Kateqoriya yoxla
        if (selectedCategory.isEmpty()) {
            Snackbar.make(cardCategory, "Kateqoriya seçin", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Seçilmiş məhsul varmı yoxla
        int selectedCount = productListAdapter.getSelectedCount();
        if (selectedCount == 0) {
            Snackbar.make(cardProduct, "Heç bir məhsul seçilməyib", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Məhsul listini Firebase-ə yadda saxla
        saveProductListToFirebase();
    }

    // List-dəki məhsulları Firebase-ə yadda saxla
    private void saveProductListToFirebase() {
        List<ProductItem> selectedProducts = productListAdapter.getSelectedProducts();

        if (selectedProducts.isEmpty()) {
            Toast.makeText(this, "Heç bir məhsul seçilməyib", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory == null || selectedCategory.isEmpty()) {
            Snackbar.make(cardCategory, "Zəhmət olmasa kateqoriya seçin", Snackbar.LENGTH_SHORT).show();
            return;
        }

        double totalAmount = productListAdapter.getSelectedTotalAmount();
        double newBalance = calculateNewBalance(totalAmount);
        String transactionGroupId = UUID.randomUUID().toString();

        showLoading(true);

        WriteBatch batch = db.batch();

        for (ProductItem product : selectedProducts) {
            String transactionId = UUID.randomUUID().toString();
            double productTotal = product.getTotalAmount();

            Map<String, Object> transaction = new HashMap<>();

            // === 1. ƏSAS MƏLUMATLAR ===
            transaction.put("transactionId", transactionId);
            transaction.put("groupId", transactionGroupId);
            transaction.put("userId", userId);
            transaction.put("type", transactionType);
            transaction.put("category", selectedCategory);
            transaction.put("note", note != null ? note : "");

            // === 2. MƏHSUL MƏLUMATLARI ===
            transaction.put("productName", product.getName());
            transaction.put("amount", product.getPrice());        // Hər məhsulun öz qiyməti
            transaction.put("productTotal", productTotal);       // Hər məhsulun öz cəmi
            transaction.put("groupTotalAmount", totalAmount);    // Qrupun ümumi cəmi

            // === 3. VAHİD MƏLUMATLARI (hər məhsulun özü) ===
            transaction.put("kg", product.getKg());
            transaction.put("liter", product.getLiter());
            transaction.put("quantity", product.getQuantity());

            // === 4. ⭐ OCR MƏLUMATLARI - HƏR MƏHSULUN ÖZÜNÜN MƏLUMATLARI ===
            // Burada hər məhsulun öz receiptId, fiscalCode, barcode, storeName, purchaseDate olmalıdır
            transaction.put("receiptId", product.getReceiptId() != null && !product.getReceiptId().isEmpty()
                    ? product.getReceiptId() : "");
            transaction.put("storeName", product.getStoreName() != null && !product.getStoreName().isEmpty()
                    ? product.getStoreName() : "");
            transaction.put("fiscalCode", product.getFiscalCode() != null && !product.getFiscalCode().isEmpty()
                    ? product.getFiscalCode() : "");
            transaction.put("barcode", product.getBarcode() != null && !product.getBarcode().isEmpty()
                    ? product.getBarcode() : "");

            // === 5. ⭐ VERGİ MƏLUMATLARI (hər məhsulun özü) ===
            transaction.put("taxAmount", product.getTaxAmount());
            transaction.put("isTaxFree", product.isTaxFree());

            // === 6. ⭐ ALIŞ TARİXİ (hər məhsulun öz qəbz tarixi) ===
            if (product.getPurchaseDate() != null) {
                transaction.put("purchaseDate", new Timestamp(product.getPurchaseDate()));
            } else {
                transaction.put("purchaseDate", null);
            }

            // === 7. TARİX VƏ SAAT ===
            transaction.put("dateString", dateFormat.format(new Date()));
            transaction.put("timeString", timeFormat.format(new Date()));
            transaction.put("timestamp", FieldValue.serverTimestamp());

            // === 8. KONUM MƏLUMATLARI ===
            transaction.put("location", address != null ? address : "");
            transaction.put("latitude", latitude);
            transaction.put("longitude", longitude);

            // === 9. BALANS MƏLUMATLARI ===
            transaction.put("balanceBefore", currentBalance);
            transaction.put("balanceAfter", newBalance);

            // === 10. BULK MƏLUMATLARI ===
            transaction.put("productCount", selectedProducts.size());
            transaction.put("isBulkTransaction", true);

            DocumentReference transactionRef = db.collection("transactions").document();
            batch.set(transactionRef, transaction);

        }

        // User balansını yenilə
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("currentBalance", newBalance);
        userUpdates.put("lastBulkTransaction", transactionGroupId);
        userUpdates.put("lastBulkTransactionDate", FieldValue.serverTimestamp());
        userUpdates.put("lastBulkProductCount", selectedProducts.size());
        userUpdates.put("lastBulkTotal", totalAmount);

        batch.update(userRef, userUpdates);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Batch transaction successful!");
                    runOnUiThread(() -> {
                        showLoading(false);
                        currentBalance = newBalance;
                        previousBalance = currentBalance;
                        updateBalanceDisplay();
                        saveNewProductsToFirebase(selectedProducts);
                        showSuccessDialog(selectedProducts.size(), totalAmount, newBalance, transactionGroupId);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Batch transaction failed: " + e.getMessage());
                    runOnUiThread(() -> {
                        showLoading(false);
                        if (!isNetworkAvailable()) {
                            saveProductsOffline(selectedProducts, totalAmount, newBalance, transactionGroupId);
                        } else {
                            new MaterialAlertDialogBuilder(AddDataActivity.this)
                                    .setTitle("❌ Xəta!")
                                    .setMessage("Məlumatlar əlavə edilmədi: " + e.getMessage())
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
                });
    }

    // Yeni məhsulları Firebase-ə əlavə et
    private void saveNewProductsToFirebase(List<ProductItem> products) {
        for (ProductItem product : products) {
            // Məhsulun Firebase-də olub-olmadığını yoxla
            boolean exists = false;
            for (ProductItem existing : firebaseProducts) {
                if (existing.getName().equalsIgnoreCase(product.getName())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                // Yeni məhsulu Firebase-ə əlavə et
                db.collection("products")
                        .add(product.toMap())
                        .addOnSuccessListener(docRef -> {
                            Log.d(TAG, "New product added to Firebase: " + product.getName());
                            product.setId(docRef.getId());
                            firebaseProducts.add(product);
                        })
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Failed to add product: " + e.getMessage())
                        );
            }
        }
    }

    // Offline rejimdə məhsulları lokalda saxla
    private void saveProductsOffline(List<ProductItem> products, double totalAmount,
                                     double newBalance, String groupId) {

        ContentValues batchValues = new ContentValues();
        batchValues.put("batchId", groupId);
        batchValues.put("userId", userId);
        batchValues.put("type", transactionType);
        batchValues.put("category", selectedCategory);
        batchValues.put("totalAmount", totalAmount);
        batchValues.put("productCount", products.size());
        batchValues.put("note", note);
        batchValues.put("date", System.currentTimeMillis());
        batchValues.put("dateString", dateFormat.format(new Date()));
        batchValues.put("timeString", timeFormat.format(new Date()));
        batchValues.put("location", address);
        batchValues.put("balanceBefore", currentBalance);
        batchValues.put("balanceAfter", newBalance);
        batchValues.put("createdAt", System.currentTimeMillis());

        // Batch məlumatını lokalda saxla
        long batchResult = localDB.insert("pending_batches", null, batchValues);

        if (batchResult != -1) {
            // Hər bir məhsulu ayrıca saxla
            for (ProductItem product : products) {
                ContentValues productValues = new ContentValues();
                productValues.put("id", UUID.randomUUID().toString());
                productValues.put("batchId", groupId);
                productValues.put("userId", userId);
                productValues.put("name", product.getName());
                productValues.put("price", product.getPrice());
                productValues.put("quantity", product.getQuantity());
                productValues.put("kg", product.getKg());
                productValues.put("liter", product.getLiter());

                double productTotal;
                if (product.getKg() > 0) {
                    productTotal = product.getKg() * product.getPrice();
                } else if (product.getLiter() > 0) {
                    productTotal = product.getLiter() * product.getPrice();
                } else {
                    productTotal = product.getPrice() * product.getQuantity();
                }
                productValues.put("total", productTotal);
                productValues.put("createdAt", System.currentTimeMillis());

                localDB.insert("pending_products", null, productValues);
            }

            runOnUiThread(() -> {
                showLoading(false);
                currentBalance = newBalance;
                previousBalance = currentBalance;
                updateBalanceDisplay();

                new MaterialAlertDialogBuilder(AddDataActivity.this)
                        .setTitle("✅ Offline Saxlanıldı")
                        .setMessage(String.format(
                                "%d məhsul lokalda saxlanıldı\n" +
                                        "İnternet bərpa olunduqda avtomatik göndəriləcək\n\n" +
                                        "Yeni balans: %s",
                                products.size(),
                                currencyFormat.format(newBalance)))
                        .setPositiveButton("OK", (dialog, which) -> {
                            // List-i təmizlə
                            productListAdapter.clearProducts();
                            updateSelectedProductsInfo();

                            Intent intent = new Intent();
                            intent.putExtra("newBalance", newBalance);
                            intent.putExtra("transactionGroupId", groupId);
                            intent.putExtra("productCount", products.size());
                            setResult(RESULT_OK, intent);
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            });
        }
    }

    // Uğur dialoqunu göstər
    private void showSuccessDialog(int productCount, double totalAmount,
                                   double newBalance, String groupId) {

        StringBuilder message = new StringBuilder();
        message.append(String.format("✅ %d məhsul uğurla əlavə edildi\n\n", productCount));
        message.append("📦 Seçilmiş məhsullar:\n");

        // Seçilmiş məhsulları siyahıya al
        List<ProductItem> selected = productListAdapter.getSelectedProducts();
        for (int i = 0; i < Math.min(selected.size(), 5); i++) {
            ProductItem p = selected.get(i);
            double productTotal;
            if (p.getKg() > 0) {
                productTotal = p.getKg() * p.getPrice();
            } else if (p.getLiter() > 0) {
                productTotal = p.getLiter() * p.getPrice();
            } else {
                productTotal = p.getPrice() * p.getQuantity();
            }
            message.append(String.format("   • %s - %s\n",
                    p.getName(),
                    currencyFormat.format(productTotal)));
        }

        if (selected.size() > 5) {
            message.append(String.format("   • ... və %d məhsul daha\n", selected.size() - 5));
        }

        message.append(String.format("\n💰 Ümumi məbləğ: %s\n", currencyFormat.format(totalAmount)));
        message.append(String.format("💳 Yeni balans: %s", currencyFormat.format(newBalance)));

        new MaterialAlertDialogBuilder(AddDataActivity.this)
                .setTitle("🎉 Əməliyyat Uğurlu!")
                .setMessage(message.toString())
                .setPositiveButton("Ana Səhifə", (dialog, which) -> {
                    // List-i təmizlə
                    productListAdapter.clearProducts();
                    updateSelectedProductsInfo();

                    // Dashboard-a qayıt
                    Intent intent = new Intent();
                    intent.putExtra("newBalance", newBalance);
                    intent.putExtra("transactionGroupId", groupId);
                    intent.putExtra("productCount", productCount);
                    intent.putExtra("totalAmount", totalAmount);
                    setResult(RESULT_OK, intent);
                    finish();
                })
                .setNegativeButton("Davam et", (dialog, which) -> {
                    // List-i təmizlə və formanı sıfırla
                    productListAdapter.clearProducts();
                    updateSelectedProductsInfo();
                    etNote.setText("");
                    note = "";

                    // Yeni əməliyyat üçün hazırlaş
                    Snackbar.make(cardProduct, "Yeni məhsullar əlavə edə bilərsiniz",
                            Snackbar.LENGTH_LONG).show();
                })
                .setCancelable(false)
                .show();
    }

    // Local database-də pending_batches və pending_products cədvəllərini yarat
    private void createPendingTables() {
        try {
            String createBatchesTable = "CREATE TABLE IF NOT EXISTS pending_batches ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "batchId TEXT UNIQUE, "
                    + "userId TEXT, "
                    + "type TEXT, "
                    + "category TEXT, "
                    + "totalAmount REAL, "
                    + "productCount INTEGER, "
                    + "note TEXT, "
                    + "date LONG, "
                    + "dateString TEXT, "
                    + "timeString TEXT, "
                    + "location TEXT, "
                    + "balanceBefore REAL, "
                    + "balanceAfter REAL, "
                    + "createdAt LONG)";

            String createProductsTable = "CREATE TABLE IF NOT EXISTS pending_products ("
                    + "id TEXT PRIMARY KEY, "
                    + "batchId TEXT, "
                    + "userId TEXT, "
                    + "name TEXT, "
                    + "price REAL, "
                    + "quantity INTEGER, "
                    + "kg REAL, "
                    + "liter REAL, "
                    + "total REAL, "
                    + "createdAt LONG)";

            localDB.execSQL(createBatchesTable);
            localDB.execSQL(createProductsTable);

            Log.d(TAG, "Pending tables created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating pending tables: " + e.getMessage());
        }
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-5367924704859976/7665228092", adRequest, new InterstitialAdLoadCallback() {
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
            mInterstitialAd.show(AddDataActivity.this);
            lastAdShownTime = currentTime; // Son reklam göstərilmə vaxtını yeniləyin
        } else if (mInterstitialAd == null) {
            Log.d("MainActivity", "Reklam hazır deyil.");
        } else {
            Log.d("MainActivity", "Reklam vaxtı tamamlanmayıb. Gözlənilir...");
        }
    }

    // Məhsul əlavə et və sonra transaction yadda saxla
    private void addProductAndSaveTransaction(final String productName, final double amount,
                                              final int quantity, final double totalAmount,
                                              final double newBalance, final String transactionId) {

        showLoading(true);

        // Məhsul məlumatlarını hazırla
        final double kg = getDoubleFromEditText(etKg);
        final double liter = getDoubleFromEditText(etLiter);
        final double price = amount; // amount artıq price-dır

        Map<String, Object> product = new HashMap<>();
        product.put("name", productName);
        product.put("kg", kg);
        product.put("liter", liter);
        product.put("price", price);
        product.put("userId", userId);
        product.put("createdAt", FieldValue.serverTimestamp());

        Log.d(TAG, "Adding new product to Firebase: " + productName);

        // Məhsulu Firebase-ə əlavə et
        db.collection("products").add(product)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Product added with ID: " + docRef.getId());

                    // Yeni məhsulu local list-ə əlavə et
                    ProductItem newItem = new ProductItem();
                    newItem.setId(docRef.getId());
                    newItem.setName(productName);
                    newItem.setKg(kg);
                    newItem.setLiter(liter);
                    newItem.setPrice(price);
                    newItem.setUserId(userId);
                    firebaseProducts.add(newItem);

                    // Spinner-i yenilə
                    runOnUiThread(() -> updateProductsSpinner());

                    // Məhsul əlavə olundu, indi transaction yadda saxla
                    saveTransactionToFirebase(productName, amount, quantity,
                            totalAmount, newBalance, transactionId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding product: " + e.getMessage());

                    // Məhsul əlavə olunmasa da, transaction-u davam etdir
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Məhsul əlavə edilmədi, ancaq transaction davam edir",
                                Toast.LENGTH_SHORT).show();
                    });

                    // Transaction-u yadda saxla (məhsul olmadan)
                    saveTransactionToFirebase(productName, amount, quantity,
                            totalAmount, newBalance, transactionId);
                });
    }

    // Transaction-ı Firebase-ə yadda saxla
    private void saveTransactionToFirebase(final String productName, final double amount,
                                           final int quantity, final double totalAmount,
                                           final double newBalance, final String transactionId) {

        Log.d(TAG, "Saving transaction to Firebase: " + transactionId);

        // Transaction məlumatlarını hazırla
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("transactionId", transactionId);
        transaction.put("userId", userId);
        transaction.put("type", transactionType);
        transaction.put("category", selectedCategory);
        transaction.put("productName", productName);
        transaction.put("liter", liter);
        transaction.put("kg", kg);
        transaction.put("amount", amount);
        transaction.put("quantity", quantity);
        transaction.put("totalAmount", totalAmount);
        transaction.put("note", note);
        transaction.put("dateString", dateFormat.format(new Date()));
        transaction.put("timeString", timeFormat.format(new Date()));
        transaction.put("timestamp", FieldValue.serverTimestamp());
        transaction.put("location", address);
        transaction.put("latitude", latitude);
        transaction.put("longitude", longitude);
        transaction.put("balanceBefore", currentBalance);
        transaction.put("balanceAfter", newBalance);

        // Firebase Transaction ilə yadda saxla
        db.runTransaction(firestoreTransaction -> {
            // 1. Cari balansı oxu (ən son vəziyyət)
            DocumentSnapshot userSnapshot = firestoreTransaction.get(userRef);
            double latestBalance = userSnapshot.contains("currentBalance")
                    ? userSnapshot.getDouble("currentBalance")
                    : 0.0;

            Log.d(TAG, "Latest balance from Firebase: " + latestBalance);

            // 2. Yeni balansı hesabla (ən son balansa əsasən)
            double calculatedNewBalance;
            if (transactionType.equals("expense")) {
                calculatedNewBalance = latestBalance - totalAmount;
            } else {
                calculatedNewBalance = latestBalance + totalAmount;
            }

            Log.d(TAG, "Calculated new balance: " + calculatedNewBalance);

            // 3. Transaction əlavə et
            DocumentReference transactionRef = db.collection("transactions").document();
            firestoreTransaction.set(transactionRef, transaction);

            // 4. User balansını yenilə
            firestoreTransaction.update(userRef, "currentBalance", calculatedNewBalance);
            firestoreTransaction.update(userRef, "lastTransaction", transactionId);
            firestoreTransaction.update(userRef, "lastTransactionDate", System.currentTimeMillis());

            return calculatedNewBalance;
        }).addOnSuccessListener(calculatedNewBalance -> {
            // Transaction uğurlu oldu
            Log.d(TAG, "✅ Transaction successful! New balance: " + calculatedNewBalance);

            // Local balansı yenilə
            currentBalance = calculatedNewBalance;
            previousBalance = currentBalance;

            runOnUiThread(() -> {
                showLoading(false);
                updateBalanceDisplay(); // UI-da balansı yenilə

                // Uğur mesajı göstər
                new MaterialAlertDialogBuilder(AddDataActivity.this)
                        .setTitle("✅ Uğurlu!")
                        .setMessage(String.format(
                                "Məlumat əlavə edildi\n\nYeni balans: %s",
                                currencyFormat.format(calculatedNewBalance)))
                        .setPositiveButton("OK", (dialog, which) -> {
                            // Dashboard-a qayıt
                            returnToDashboard(calculatedNewBalance, transactionId);
                        })
                        .setCancelable(false)
                        .show();
            });
        }).addOnFailureListener(e -> {
            // Transaction uğursuz oldu
            Log.e(TAG, "❌ Transaction failed: " + e.getMessage());
            e.printStackTrace();

            runOnUiThread(() -> {
                showLoading(false);
                btnSave.setEnabled(true);

                // Xəta mesajı göstər
                Toast.makeText(AddDataActivity.this,
                        "Xəta: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        });
    }

    // Transaction-ı local database-ə yadda saxla (offline rejim)
    private void saveTransactionOffline(final String productName, final double amount,
                                        final int quantity, final double totalAmount,
                                        final double newBalance, final String transactionId) {

        Log.d(TAG, "Saving transaction offline: " + transactionId);

        // Transaction məlumatlarını hazırla
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("transactionId", transactionId);
        transaction.put("userId", userId);
        transaction.put("type", transactionType);
        transaction.put("category", selectedCategory);
        transaction.put("productName", productName);
        transaction.put("liter", liter);
        transaction.put("kg", kg);
        transaction.put("amount", amount);
        transaction.put("quantity", quantity);
        transaction.put("totalAmount", totalAmount);
        transaction.put("note", note);
        transaction.put("dateString", dateFormat.format(new Date()));
        transaction.put("timeString", timeFormat.format(new Date()));
        transaction.put("location", address);
        transaction.put("latitude", latitude);
        transaction.put("longitude", longitude);
        transaction.put("balanceBefore", currentBalance);
        transaction.put("balanceAfter", newBalance);

        // ContentValues yarat
        ContentValues values = new ContentValues();
        values.put("id", transactionId);
        values.put("userId", userId);
        values.put("type", transactionType);
        values.put("category", selectedCategory);
        values.put("productName", productName);
        values.put("amount", amount);
        values.put("quantity", quantity);
        values.put("totalAmount", totalAmount);
        values.put("note", note);
        values.put("date", System.currentTimeMillis());
        values.put("dateString", dateFormat.format(new Date()));
        values.put("timeString", timeFormat.format(new Date()));
        values.put("location", address);
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        values.put("balanceBefore", currentBalance);
        values.put("balanceAfter", newBalance);
        values.put("createdAt", System.currentTimeMillis());

        // Pending transactions cədvəlinə əlavə et
        long result = localDB.insert(PENDING_TRANSACTIONS_TABLE, null, values);

        if (result != -1) {
            // Local transactions cədvəlinə də əlavə et
            ContentValues localValues = new ContentValues();
            localValues.put("id", transactionId);
            localValues.put("userId", userId);
            localValues.put("type", transactionType);
            localValues.put("category", selectedCategory);
            localValues.put("productName", productName);
            localValues.put("amount", amount);
            localValues.put("quantity", quantity);
            localValues.put("totalAmount", totalAmount);
            localValues.put("note", note);
            localValues.put("date", System.currentTimeMillis());
            localValues.put("dateString", dateFormat.format(new Date()));
            localValues.put("timeString", timeFormat.format(new Date()));
            localValues.put("location", address);
            localValues.put("latitude", latitude);
            localValues.put("longitude", longitude);
            localValues.put("balanceBefore", currentBalance);
            localValues.put("balanceAfter", newBalance);
            localValues.put("syncedAt", 0);

            localDB.insert(LOCAL_TRANSACTIONS_TABLE, null, localValues);

            // Local balansı yenilə
            currentBalance = newBalance;
            previousBalance = currentBalance;

            runOnUiThread(() -> {
                showLoading(false);
                updateBalanceDisplay();

                new MaterialAlertDialogBuilder(AddDataActivity.this)
                        .setTitle("✅ Offline Saxlanıldı")
                        .setMessage(String.format(
                                "Məlumat lokalda saxlanıldı\n" +
                                        "İnternet bərpa olunduqda avtomatik göndəriləcək\n\n" +
                                        "Yeni balans: %s",
                                currencyFormat.format(newBalance)))
                        .setPositiveButton("OK", (dialog, which) ->
                                returnToDashboard(newBalance, transactionId))
                        .setCancelable(false)
                        .show();
            });
        } else {
            runOnUiThread(() -> {
                showLoading(false);
                btnSave.setEnabled(true);
                Toast.makeText(AddDataActivity.this,
                        "Xəta: Məlumat saxlanılmadı", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void saveToLocalDatabase(Map<String, Object> transaction, double newBalance) {
        ContentValues values = new ContentValues();
        values.put("id", (String) transaction.get("transactionId"));
        values.put("userId", userId);
        values.put("type", transactionType);
        values.put("category", selectedCategory);
        values.put("productName", productName);
        values.put("amount", amount);
        values.put("quantity", quantity);
        values.put("totalAmount", (Double) transaction.get("totalAmount"));
        values.put("note", note);
        values.put("date", System.currentTimeMillis());
        values.put("dateString", dateFormat.format(new Date()));
        values.put("timeString", timeFormat.format(new Date()));
        values.put("location", address);
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        values.put("balanceBefore", currentBalance);
        values.put("balanceAfter", newBalance);
        values.put("createdAt", System.currentTimeMillis());

        long result = localDB.insert(PENDING_TRANSACTIONS_TABLE, null, values);

        if (result != -1) {
            ContentValues balanceValues = new ContentValues();
            balanceValues.put("id", (String) transaction.get("transactionId"));
            balanceValues.put("userId", userId);
            balanceValues.put("type", transactionType);
            balanceValues.put("category", selectedCategory);
            balanceValues.put("productName", productName);
            balanceValues.put("amount", amount);
            balanceValues.put("quantity", quantity);
            balanceValues.put("totalAmount", (Double) transaction.get("totalAmount"));
            balanceValues.put("note", note);
            balanceValues.put("date", System.currentTimeMillis());
            balanceValues.put("dateString", dateFormat.format(new Date()));
            balanceValues.put("timeString", timeFormat.format(new Date()));
            balanceValues.put("location", address);
            balanceValues.put("latitude", latitude);
            balanceValues.put("longitude", longitude);
            balanceValues.put("balanceBefore", currentBalance);
            balanceValues.put("balanceAfter", newBalance);
            balanceValues.put("syncedAt", 0);

            localDB.insert(LOCAL_TRANSACTIONS_TABLE, null, balanceValues);

            runOnUiThread(() -> {
                showLoading(false);

                new MaterialAlertDialogBuilder(AddDataActivity.this)
                        .setTitle("✅ Offline Saxlanıldı")
                        .setMessage(String.format(
                                "Məlumat lokalda saxlanıldı\nİnternet bərpa olunduqda avtomatik göndəriləcək\n\nYeni balans: %s",
                                currencyFormat.format(newBalance)))
                        .setPositiveButton("OK", (dialog, which) ->
                                returnToDashboard(newBalance, (String) transaction.get("transactionId")))
                        .setCancelable(false)
                        .show();
            });
        } else {
            runOnUiThread(() -> {
                showLoading(false);
                btnSave.setEnabled(true);
                Toast.makeText(AddDataActivity.this, "Xəta: Məlumat saxlanılmadı", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void syncPendingTransactions() {
        if (!isNetworkAvailable()) return;

        Cursor cursor = null;
        try {
            cursor = localDB.query(PENDING_TRANSACTIONS_TABLE,
                    null, "userId = ?", new String[]{userId},
                    null, null, "createdAt ASC");

            if (cursor.getCount() == 0) {
                return;
            }

            showLoading(true);

            while (cursor.moveToNext()) {
                final Map<String, Object> transaction = new HashMap<>();

                int idIndex = cursor.getColumnIndex("id");
                int userIdIndex = cursor.getColumnIndex("userId");
                int typeIndex = cursor.getColumnIndex("type");
                int categoryIndex = cursor.getColumnIndex("category");
                int productNameIndex = cursor.getColumnIndex("productName");
                int amountIndex = cursor.getColumnIndex("amount");
                int quantityIndex = cursor.getColumnIndex("quantity");
                int totalAmountIndex = cursor.getColumnIndex("totalAmount");
                int noteIndex = cursor.getColumnIndex("note");
                int dateIndex = cursor.getColumnIndex("date");
                int dateStringIndex = cursor.getColumnIndex("dateString");
                int timeStringIndex = cursor.getColumnIndex("timeString");
                int locationIndex = cursor.getColumnIndex("location");
                int latitudeIndex = cursor.getColumnIndex("latitude");
                int longitudeIndex = cursor.getColumnIndex("longitude");
                int balanceBeforeIndex = cursor.getColumnIndex("balanceBefore");
                int balanceAfterIndex = cursor.getColumnIndex("balanceAfter");

                if (idIndex != -1) transaction.put("transactionId", cursor.getString(idIndex));
                if (userIdIndex != -1) transaction.put("userId", cursor.getString(userIdIndex));
                if (typeIndex != -1) transaction.put("type", cursor.getString(typeIndex));
                if (categoryIndex != -1) transaction.put("category", cursor.getString(categoryIndex));
                if (productNameIndex != -1) transaction.put("productName", cursor.getString(productNameIndex));
                if (amountIndex != -1) transaction.put("amount", cursor.getDouble(amountIndex));
                if (quantityIndex != -1) transaction.put("quantity", cursor.getInt(quantityIndex));
                if (totalAmountIndex != -1) transaction.put("totalAmount", cursor.getDouble(totalAmountIndex));
                if (noteIndex != -1) transaction.put("note", cursor.getString(noteIndex));
                if (dateIndex != -1) transaction.put("date", new Date(cursor.getLong(dateIndex)));
                if (dateStringIndex != -1) transaction.put("dateString", cursor.getString(dateStringIndex));
                if (timeStringIndex != -1) transaction.put("timeString", cursor.getString(timeStringIndex));
                if (locationIndex != -1) transaction.put("location", cursor.getString(locationIndex));
                if (latitudeIndex != -1) transaction.put("latitude", cursor.getDouble(latitudeIndex));
                if (longitudeIndex != -1) transaction.put("longitude", cursor.getDouble(longitudeIndex));
                if (balanceBeforeIndex != -1) transaction.put("balanceBefore", cursor.getDouble(balanceBeforeIndex));
                if (balanceAfterIndex != -1) transaction.put("balanceAfter", cursor.getDouble(balanceAfterIndex));

                transaction.put("timestamp", FieldValue.serverTimestamp());

                final String transactionId = (String) transaction.get("transactionId");
                final double newBalance = (double) transaction.get("balanceAfter");

                db.runTransaction(firestoreTransaction -> {
                    DocumentReference transactionRef = db.collection("transactions").document();
                    firestoreTransaction.set(transactionRef, transaction);
                    firestoreTransaction.update(userRef, "currentBalance", newBalance);
                    return null;
                }).addOnSuccessListener(aVoid -> {
                    localDB.delete(PENDING_TRANSACTIONS_TABLE,
                            "id = ?", new String[]{transactionId});

                    runOnUiThread(() ->
                            Snackbar.make(cardBalance, "Sinxronizasiya uğurlu", Snackbar.LENGTH_SHORT).show()
                    );
                }).addOnFailureListener(e ->
                        Log.e("SYNC", "Sinxronizasiya xətası: " + e.getMessage())
                );
            }
        } catch (Exception e) {
            Log.e("SYNC", "Sinxronizasiya zamanı xəta: " + e.getMessage());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            showLoading(false);
        }
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(productName)) {
            etProductName.setError("Məhsul adı daxil edin");
            etProductName.requestFocus();
            return false;
        }

        if (amount <= 0) {
            etAmount.setError("Məbləğ daxil edin");
            etAmount.requestFocus();
            return false;
        }

        if (selectedCategory.isEmpty()) {
            Snackbar.make(cardCategory, "Kateqoriya seçin", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private double calculateNewBalance(double totalAmount) {
        if (transactionType.equals("expense")) {
            return currentBalance - totalAmount;
        } else {
            return currentBalance + totalAmount;
        }
    }

    private Map<String, Object> createTransactionMap(String transactionId, double totalAmount, double newBalance) {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("transactionId", transactionId);
        transaction.put("userId", userId);
        transaction.put("type", transactionType);
        transaction.put("category", selectedCategory);
        transaction.put("productName", productName);
        transaction.put("liter", liter);
        transaction.put("kg", kg);
        transaction.put("amount", amount);
        transaction.put("quantity", quantity);
        transaction.put("totalAmount", totalAmount);
        transaction.put("note", note);
        transaction.put("dateString", dateFormat.format(new Date()));
        transaction.put("timeString", timeFormat.format(new Date()));
        transaction.put("timestamp", FieldValue.serverTimestamp());
        transaction.put("location", address);
        transaction.put("latitude", latitude);
        transaction.put("longitude", longitude);
        transaction.put("balanceBefore", currentBalance);
        transaction.put("balanceAfter", newBalance);

        return transaction;
    }

    private void executeFirestoreTransaction(final Map<String, Object> transaction,
                                             final String transactionId,
                                             final double newBalance) {
        showLoading(true);

        db.runTransaction(firestoreTransaction -> {
            // 1. Əvvəlcə cari balansı oxu
            DocumentSnapshot userSnapshot = firestoreTransaction.get(userRef);
            double currentBalance = userSnapshot.contains("currentBalance")
                    ? userSnapshot.getDouble("currentBalance")
                    : 0.0;

            // 2. Yeni balansı hesabla
            double calculatedNewBalance;
            double totalAmount = amount * quantity;

            Log.d(TAG, "Transaction - Type: " + transactionType);
            Log.d(TAG, "Current Balance: " + currentBalance);
            Log.d(TAG, "Total Amount: " + totalAmount);

            if (transactionType.equals("expense")) {
                calculatedNewBalance = currentBalance - totalAmount;
                Log.d(TAG, "Expense: New Balance = " + calculatedNewBalance);
            } else {
                calculatedNewBalance = currentBalance + totalAmount;
                Log.d(TAG, "Income: New Balance = " + calculatedNewBalance);
            }

            // 3. Transaction əlavə et
            DocumentReference transactionRef = db.collection("transactions").document();
            firestoreTransaction.set(transactionRef, transaction);

            // 4. Balansı yenilə
            firestoreTransaction.update(userRef, "currentBalance", calculatedNewBalance);
            firestoreTransaction.update(userRef, "lastTransaction", transactionId);
            firestoreTransaction.update(userRef, "lastTransactionDate", System.currentTimeMillis());

            return calculatedNewBalance;
        }).addOnSuccessListener(calculatedNewBalance -> {
            // Transaction uğurlu oldu
            Log.d(TAG, "Transaction successful! New balance: " + calculatedNewBalance);

            currentBalance = calculatedNewBalance;
            previousBalance = currentBalance;

            runOnUiThread(() -> {
                updateBalanceDisplay();
                showLoading(false);

                new MaterialAlertDialogBuilder(AddDataActivity.this)
                        .setTitle("✅ Uğurlu!")
                        .setMessage(String.format("Məlumat əlavə edildi\nYeni balans: %s",
                                currencyFormat.format(calculatedNewBalance)))
                        .setPositiveButton("OK", (dialog, which) -> {
                            returnToDashboard(calculatedNewBalance, transactionId);
                        })
                        .setCancelable(false)
                        .show();
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction failed: " + e.getMessage());
            e.printStackTrace();

            runOnUiThread(() -> {
                showLoading(false);
                btnSave.setEnabled(true);
                Toast.makeText(AddDataActivity.this,
                        "Xəta: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        });
    }

    private void handleTransactionSuccess(Map<String, Object> transaction, double newBalance) {
        showLoading(false);

        new MaterialAlertDialogBuilder(AddDataActivity.this)
                .setTitle("✅ Uğurlu!")
                .setMessage(String.format("Məlumat əlavə edildi\nYeni balans: %s",
                        currencyFormat.format(newBalance)))
                .setPositiveButton("OK", (dialog, which) -> {
                    sendToModel(transaction);
                    returnToDashboard(newBalance, (String) transaction.get("transactionId"));
                })
                .setCancelable(false)
                .show();
    }

    private void handleTransactionFailure(Exception e) {
        showLoading(false);
        btnSave.setEnabled(true);

        if (!isNetworkAvailable()) {
            saveToLocalDatabase(createTransactionMap(
                            UUID.randomUUID().toString(),
                            amount * quantity,
                            calculateNewBalance(amount * quantity)),
                    calculateNewBalance(amount * quantity));
        } else {
            new MaterialAlertDialogBuilder(AddDataActivity.this)
                    .setTitle("❌ Xəta!")
                    .setMessage("Məlumat əlavə edilmədi: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void returnToDashboard(double newBalance, String transactionId) {
        Intent intent = new Intent();
        intent.putExtra("newBalance", newBalance);
        intent.putExtra("transactionId", transactionId);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void sendToModel(Map<String, Object> transaction) {
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                        Log.d("ML_MODEL", "Son 10 əməliyyat model üçün hazırlandı")
                );
    }

    private void showLoading(boolean show) {
        if (show) {
            progressIndicator.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);
            btnCancel.setEnabled(false);
        } else {
            progressIndicator.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            btnCancel.setEnabled(true);
        }
    }

    private void showExitConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Dəyişikliklər itəcək")
                .setMessage("Çıxmaq istədiyinizə əminsiniz? Daxil edilən məlumatlar itəcək.")
                .setPositiveButton("Çıx", (dialog, which) -> finish())
                .setNegativeButton("Qal", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (!productName.isEmpty() || amount > 0 || !note.isEmpty()) {
            showExitConfirmationDialog();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        syncHandler.removeCallbacks(syncRunnable);
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {}
    }
}