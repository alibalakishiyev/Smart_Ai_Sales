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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.data.location.LocationItem;
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
import com.smart_ai_sales.R;

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

    // Offline support
    private SQLiteDatabase localDB;
    private LocalDatabaseHelper dbHelper;
    private static final String PENDING_TRANSACTIONS_TABLE = "pending_transactions";
    private static final String LOCAL_TRANSACTIONS_TABLE = "local_transactions";
    private static final String LOCATIONS_TABLE = "saved_locations";
    private Handler syncHandler = new Handler();
    private Runnable syncRunnable;
    private boolean isOfflineMode = false;

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

        // btnBack-i tap


        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(AddDataActivity.this, com.main.MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        updateSaveButtonState();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnCancel.setOnClickListener(v -> showExitConfirmationDialog());
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

        fabScan.setOnClickListener(v ->
                Snackbar.make(cardProduct, "OCR skan funksiyası hazırlanır...", Snackbar.LENGTH_SHORT).show()
        );


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

    private void saveLocationToLocalDB(String name, String address, double lat, double lng) {
        try {
            ContentValues values = new ContentValues();
            values.put("userId", userId);
            values.put("address", address);
            values.put("latitude", lat);
            values.put("longitude", lng);
            values.put("lastUsed", System.currentTimeMillis());
            values.put("useCount", 1);

            localDB.insert(LOCATIONS_TABLE, null, values);
            Log.d(TAG, "Location saved to local DB: " + address);
        } catch (Exception e) {
            Log.e(TAG, "Error saving to local DB: " + e.getMessage());
        }
    }

    private void saveLocationsToLocalDB(List<LocationItem> locations) {
        for (LocationItem item : locations) {
            ContentValues values = new ContentValues();
            values.put("id", item.getId());
            values.put("userId", item.getUserId());
            values.put("name", item.getName());
            values.put("address", item.getAddress());
            values.put("latitude", item.getLatitude());
            values.put("longitude", item.getLongitude());
            values.put("useCount", item.getUseCount());
            values.put("lastUsed", item.getLastUsed() != null ? item.getLastUsed().toDate().getTime() : System.currentTimeMillis());
            values.put("synced", 1);

            localDB.insertWithOnConflict(LOCATIONS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private void updateLocationSpinner() {
        savedLocations.clear();
        savedLocations.add("🆕 Yeni konum al");

        for (LocationItem item : firebaseLocations) {
            String display = item.getName() != null && !item.getName().isEmpty()
                    ? item.getName() + " - " + item.getAddress()
                    : item.getAddress();
            savedLocations.add(display);
        }

        locationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, savedLocations);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSavedLocations.setAdapter(locationAdapter);
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
        String[] categories = transactionType.equals("expense") ? expenseCategories : incomeCategories;

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

    // Yeni məhsul əlavə et - sadə versiya
    private void addProductToFirebase() {
        String name = etProductName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Məhsul adı daxil edin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Məhsul məlumatlarını topla
        final String productName = name;
        final double kg = getDoubleFromEditText(etKg);
        final double liter = getDoubleFromEditText(etLiter);
        final double price = getDoubleFromEditText(etAmount);

        Map<String, Object> product = new HashMap<>();
        product.put("name", productName);
        product.put("kg", kg);
        product.put("liter", liter);
        product.put("price", price);
        product.put("userId", userId);
        product.put("createdAt", FieldValue.serverTimestamp());

        showLoading(true);

        db.collection("products").add(product)
                .addOnSuccessListener(docRef -> {
                    // Firebase-dən gələn ID ilə yeni obyekt yarat
                    ProductItem newItem = new ProductItem();
                    newItem.setId(docRef.getId());
                    newItem.setName(productName);
                    newItem.setKg(kg);
                    newItem.setLiter(liter);
                    newItem.setPrice(price);
                    newItem.setUserId(userId);
                    firebaseProducts.add(newItem);

                    runOnUiThread(() -> {
                        updateProductsSpinner();
                        showLoading(false);
                        clearProductInputs();
                        Toast.makeText(AddDataActivity.this, "✅ Məhsul əlavə edildi", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(AddDataActivity.this, "❌ Xəta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                });
    }

    // Köməkçi metodlar
    private double getDoubleFromEditText(TextInputEditText editText) {
        String text = editText.getText().toString();
        try {
            return text.isEmpty() ? 0 : Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void clearProductInputs() {
        etProductName.setText("");
        etKg.setText("0");
        etLiter.setText("0");
        etAmount.setText("");
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
        // 1. Inputları yoxla
        if (!validateInputs()) {
            return;
        }

        // 2. Məhsul adını və məbləği al
        final String productNameInput = etProductName.getText().toString().trim();
        final double amountInput = getDoubleFromEditText(etAmount);
        final int quantityInput = Integer.parseInt(etQuantity.getText().toString());
        final double totalAmount = amountInput * quantityInput;

        Log.d(TAG, "=== SAVE TRANSACTION STARTED ===");
        Log.d(TAG, "Type: " + transactionType);
        Log.d(TAG, "Product: " + productNameInput);
        Log.d(TAG, "Amount: " + amountInput + " x " + quantityInput + " = " + totalAmount);
        Log.d(TAG, "Current Balance: " + currentBalance);

        // 3. Yeni balansı hesabla
        final double newBalance = calculateNewBalance(totalAmount);
        Log.d(TAG, "New Balance will be: " + newBalance);

        // 4. Transaction ID yarat
        final String transactionId = UUID.randomUUID().toString();

        // 5. Məhsul yenidirmi yoxla
        boolean isNewProduct = true;
        for (ProductItem item : firebaseProducts) {
            if (item.getName().equalsIgnoreCase(productNameInput)) {
                isNewProduct = false;
                break;
            }
        }

        // 6. Əgər internet varsa Firebase-ə yadda saxla, yoxsa local
        if (isNetworkAvailable()) {
            // 6a. Yeni məhsuldursa, əvvəl məhsulu əlavə et, sonra transaction
            if (isNewProduct) {
                addProductAndSaveTransaction(productNameInput, amountInput, quantityInput,
                        totalAmount, newBalance, transactionId);
            } else {
                // 6b. Mövcud məhsuldursa, birbaşa transaction yadda saxla
                saveTransactionToFirebase(productNameInput, amountInput, quantityInput,
                        totalAmount, newBalance, transactionId);
            }
        } else {
            // 7. Offline rejim - local database-ə yadda saxla
            saveTransactionOffline(productNameInput, amountInput, quantityInput,
                    totalAmount, newBalance, transactionId);
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