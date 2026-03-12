package com.ocr_service;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.smart_ai_sales.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReceiptScannerActivity extends AppCompatActivity {

    private static final String TAG = "ReceiptScan";
    private static final String E_KASSA_BASE_URL = "https://monitoring.e-kassa.gov.az/#/index?doc=";

    // Request kodları
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;
    private static final int CAMERA_PERMISSION_CODE = 103;
    private static final int STORAGE_PERMISSION_CODE = 104;

    // UI Elementləri
    private ImageView ivReceiptImage;
    private TextView tvNoImageText, tvQrStatus, tvLoading, tvTotalAmount, tvDocId;
    private CardView cardImage, cardResults, cardQrInfo, cardManualFiscal;
    private LinearLayout productsContainer;
    private LinearProgressIndicator progressIndicator;
    private ProgressBar progressBar;
    private MaterialButton btnScan, btnSave, btnRetake, btnFetchFromEKassa, btnManualSearch;
    private FloatingActionButton fabCamera, fabGallery;
    private EditText etStoreName, etDate, etTime, etManualDocId;
    private ScrollView mainScrollView;

    // OCR Helper
    private RealOCRHelper realOCRHelper;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // Məlumatlar
    private Bitmap currentBitmap;
    private Uri currentPhotoUri;
    private String currentPhotoPath;
    private List<ProductItem> scannedProducts = new ArrayList<>();
    private double totalAmount = 0.0;
    private String storeName = "";
    private String receiptDate = "";
    private String receiptTime = "";
    private String qrDocId = "";

    // Drag and Drop üçün
    private View draggedView = null;
    private float initialY = 0;
    private int draggedPosition = -1;
    private boolean isDragging = false;

    // Məhsul sinfi
    public static class ProductItem {
        String name;
        double price;
        double quantity;
        String unit;
        double total;

        public ProductItem(String name, double price, double quantity, String unit, double total) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
            this.unit = unit;
            this.total = total;
        }

        public ProductItem copy() {
            return new ProductItem(name, price, quantity, unit, total);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_scanner);

        initViews();
        initOCR();
        setupClickListeners();
        animateViews();
    }

    private void initViews() {
        ivReceiptImage = findViewById(R.id.ivReceiptImage);
        tvNoImageText = findViewById(R.id.tvNoImageText);
        tvQrStatus = findViewById(R.id.tvQrStatus);
        tvLoading = findViewById(R.id.tvLoading);
        cardImage = findViewById(R.id.cardImage);
        cardResults = findViewById(R.id.cardResults);
        cardQrInfo = findViewById(R.id.cardQrInfo);
        cardManualFiscal = findViewById(R.id.cardManualFiscal);
        productsContainer = findViewById(R.id.productsContainer);
        progressIndicator = findViewById(R.id.progressIndicator);
        progressBar = findViewById(R.id.progressBar);
        btnScan = findViewById(R.id.btnScan);
        btnSave = findViewById(R.id.btnSave);
        btnRetake = findViewById(R.id.btnRetake);
        btnFetchFromEKassa = findViewById(R.id.btnFetchFromEKassa);
        btnManualSearch = findViewById(R.id.btnManualSearch);
        fabCamera = findViewById(R.id.fabCamera);
        fabGallery = findViewById(R.id.fabGallery);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvDocId = findViewById(R.id.tvDocId);
        etManualDocId = findViewById(R.id.etManualDocId);
        etStoreName = findViewById(R.id.etStoreName);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        mainScrollView = findViewById(R.id.mainScrollView);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        cardResults.setVisibility(View.GONE);
        cardQrInfo.setVisibility(View.GONE);
        cardManualFiscal.setVisibility(View.VISIBLE);

        btnSave.setEnabled(false);
        btnFetchFromEKassa.setEnabled(false);
        btnFetchFromEKassa.setAlpha(0.5f);

        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (tvLoading != null) tvLoading.setVisibility(View.GONE);
    }

    private void initOCR() {
        try {
            realOCRHelper = new RealOCRHelper(this);
            Log.d(TAG, "RealOCRHelper uğurla işə salındı");
        } catch (Exception e) {
            Log.e(TAG, "OCR işə salınarkən xəta: " + e.getMessage());
        }
    }

    private void setupClickListeners() {
        fabCamera.setOnClickListener(v -> checkCameraPermission());

        fabGallery.setOnClickListener(v -> checkStoragePermission());

        btnScan.setOnClickListener(v -> {
            if (currentBitmap != null) {
                detectAndProcessQRCode(currentBitmap);
            } else {
                Toast.makeText(this, "Əvvəlcə şəkil seçin", Toast.LENGTH_SHORT).show();
            }
        });

        btnFetchFromEKassa.setOnClickListener(v -> {
            if (!qrDocId.isEmpty()) {
                fetchReceiptFromEKassa(qrDocId);
            } else {
                Toast.makeText(this, "QR kod tapılmadı", Toast.LENGTH_SHORT).show();
            }
        });

        btnManualSearch.setOnClickListener(v -> {
            String manualDocId = etManualDocId.getText().toString().trim();
            if (!manualDocId.isEmpty()) {
                fetchReceiptFromEKassa(manualDocId);
            } else {
                Toast.makeText(this, "Fiskal kod daxil edin", Toast.LENGTH_SHORT).show();
            }
        });

        btnRetake.setOnClickListener(v -> {
            resetUI();
            showImageSourceDialog();
        });

        btnSave.setOnClickListener(v -> saveDataAndReturn());

        etStoreName.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) { storeName = s.toString(); }
        });

        etDate.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) { receiptDate = s.toString(); }
        });

        etTime.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) { receiptTime = s.toString(); }
        });
    }

    private void animateViews() {
        cardImage.setAlpha(0f);
        cardImage.setTranslationY(50f);
        cardImage.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .start();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    private void checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, STORAGE_PERMISSION_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            }
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Fayl yaradılarkən xəta", ex);
            }

            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void showImageSourceDialog() {
        String[] options = {"Kamera ilə çək", "Qalereyadan seç", "Ləğv et"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Şəkil mənbəyi seçin")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: checkCameraPermission(); break;
                        case 1: checkStoragePermission(); break;
                        case 2: dialog.dismiss(); break;
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    try {
                        currentBitmap = BitmapFactory.decodeFile(currentPhotoPath);
                        displayImage(currentBitmap);
                    } catch (Exception e) {
                        Log.e(TAG, "Kamera şəkli yüklənərkən xəta", e);
                        Toast.makeText(this, "Şəkil yüklənmədi", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case REQUEST_IMAGE_PICK:
                    if (data != null && data.getData() != null) {
                        Uri imageUri = data.getData();
                        try {
                            currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            displayImage(currentBitmap);
                        } catch (IOException e) {
                            Log.e(TAG, "Qalereya şəkli yüklənərkən xəta", e);
                            Toast.makeText(this, "Şəkil yüklənmədi", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
            }
        }
    }

    private void displayImage(Bitmap bitmap) {
        if (bitmap != null) {
            ivReceiptImage.setImageBitmap(bitmap);
            tvNoImageText.setVisibility(View.GONE);
            btnScan.setEnabled(true);
            btnScan.setAlpha(1f);
        }
    }

    private void detectAndProcessQRCode(Bitmap bitmap) {
        progressIndicator.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        tvLoading.setVisibility(View.VISIBLE);
        tvLoading.setText("QR kod axtarılır...");
        tvQrStatus.setText("QR kod axtarılır...");
        cardQrInfo.setVisibility(View.VISIBLE);
        btnScan.setEnabled(false);

        new Thread(() -> {
            try {
                String qrText = scanQRCode(bitmap);

                if (qrText != null && !qrText.isEmpty()) {
                    String docId = extractDocIdFromQR(qrText);

                    if (!docId.isEmpty()) {
                        qrDocId = docId;

                        runOnUiThread(() -> {
                            tvLoading.setVisibility(View.GONE);
                            tvQrStatus.setText("✅ QR kod tapıldı! Sənəd ID: " + docId);
                            tvDocId.setText("Sənəd ID: " + docId);
                            btnFetchFromEKassa.setEnabled(true);
                            btnFetchFromEKassa.setAlpha(1f);
                            fetchReceiptFromEKassa(docId);
                        });
                    } else {
                        runOnUiThread(() -> {
                            tvLoading.setText("QR kodda sənəd ID tapılmadı, OCR davam edir...");
                            performOCR();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        tvLoading.setText("QR kod tapılmadı, OCR davam edir...");
                        performOCR();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "QR kod aşkarlama xətası", e);
                runOnUiThread(() -> {
                    tvLoading.setText("QR kod oxunarkən xəta, OCR davam edir...");
                    performOCR();
                });
            }
        }).start();
    }

    private String scanQRCode(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            MultiFormatReader reader = new MultiFormatReader();
            Result result = reader.decode(binaryBitmap);

            return result != null ? result.getText() : null;
        } catch (Exception e) {
            Log.e(TAG, "QR skan xətası", e);
            return null;
        }
    }

    private String extractDocIdFromQR(String qrText) {
        if (qrText == null || qrText.isEmpty()) return "";

        Pattern pattern = Pattern.compile("[?&]doc=([^&]+)");
        Matcher matcher = pattern.matcher(qrText);

        if (matcher.find()) {
            return matcher.group(1);
        }

        if (qrText.length() > 10 && qrText.length() < 50) {
            return qrText;
        }

        return "";
    }

    private void fetchReceiptFromEKassa(String docId) {
        progressIndicator.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        tvLoading.setVisibility(View.VISIBLE);
        tvLoading.setText("e-kassa-dan məlumatlar yüklənir...");
        tvQrStatus.setText("e-kassa-dan məlumatlar yüklənir...");
        cardQrInfo.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String apiUrl = "https://monitoring.e-kassa.gov.az/api/receipt/" + docId;

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                        .build();

                Request apiRequest = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("Accept", "application/json")
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .build();

                Response apiResponse = client.newCall(apiRequest).execute();

                if (apiResponse.isSuccessful() && apiResponse.body() != null) {
                    String jsonData = apiResponse.body().string();

                    if (parseReceiptJson(jsonData)) {
                        runOnUiThread(() -> {
                            progressIndicator.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            tvLoading.setVisibility(View.GONE);
                            tvQrStatus.setText("✅ Məlumatlar yükləndi!");
                            displayResults();
                        });
                        return;
                    }
                }

                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    tvLoading.setVisibility(View.GONE);
                    tvQrStatus.setText("⚠️ Məlumat tapılmadı");
                    performOCR();
                });

            } catch (Exception e) {
                Log.e(TAG, "Yükləmə xətası", e);
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    tvLoading.setVisibility(View.GONE);
                    tvQrStatus.setText("⚠️ Xəta: " + e.getMessage());
                    performOCR();
                });
            }
        }).start();
    }

    private boolean parseReceiptJson(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);

            storeName = json.optString("sellerName", "Mağaza adı tapılmadı");

            String dateStr = json.optString("date", "");
            if (!dateStr.isEmpty()) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
                    Date date = inputFormat.parse(dateStr);
                    receiptDate = outputFormat.format(date);

                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
                    receiptTime = timeFormat.format(date);
                } catch (Exception e) {
                    receiptDate = dateStr;
                }
            }

            JSONArray items = json.optJSONArray("items");
            if (items != null && items.length() > 0) {
                scannedProducts.clear();
                totalAmount = 0.0;

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);

                    String name = item.optString("name", "Məhsul " + (i+1));
                    double price = item.optDouble("price", 0.0);
                    double quantity = item.optDouble("quantity", 1.0);
                    String unit = item.optString("unit", "ədəd");
                    double total = price * quantity;

                    scannedProducts.add(new ProductItem(name, price, quantity, unit, total));
                    totalAmount += total;
                }
                return true;
            }
            return false;
        } catch (JSONException e) {
            Log.e(TAG, "JSON parse xətası", e);
            return false;
        }
    }

    private void performOCR() {
        if (currentBitmap == null) {
            Toast.makeText(this, "Şəkil yoxdur", Toast.LENGTH_SHORT).show();
            return;
        }

        if (realOCRHelper == null) {
            initOCR();
        }

        progressIndicator.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        tvLoading.setVisibility(View.VISIBLE);
        tvLoading.setText("Çek analiz edilir...");
        btnScan.setEnabled(false);

        realOCRHelper.detectText(currentBitmap, new RealOCRHelper.OCRCallback() {
            @Override
            public void onSuccess(List<RealOCRHelper.TextBlock> textBlocks) {
                ReceiptParser.ReceiptData receiptData = ReceiptParser.parseReceipt(textBlocks);

                storeName = receiptData.storeName;
                receiptDate = receiptData.date;
                receiptTime = receiptData.time;
                qrDocId = receiptData.fiscalCode;

                scannedProducts.clear();
                for (ReceiptParser.ReceiptData.ProductItem product : receiptData.products) {
                    scannedProducts.add(new ProductItem(
                            product.name,
                            product.price,
                            product.quantity,
                            product.unit,
                            product.total
                    ));
                }

                totalAmount = receiptData.totalAmount;

                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    tvLoading.setVisibility(View.GONE);
                    btnScan.setEnabled(true);

                    if (!receiptData.fiscalCode.isEmpty()) {
                        tvQrStatus.setText("✅ Fiskal kod tapıldı: " + receiptData.fiscalCode);
                        tvDocId.setText("Sənəd ID: " + receiptData.fiscalCode);
                        btnFetchFromEKassa.setEnabled(true);
                        btnFetchFromEKassa.setAlpha(1f);
                    } else {
                        tvQrStatus.setText("✅ Çek analiz edildi");
                    }

                    displayResults();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "OCR xətası: " + error);
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    tvLoading.setVisibility(View.GONE);
                    btnScan.setEnabled(true);
                    showError("OCR xətası: " + error);
                });
            }
        });
    }

    private void displayResults() {
        cardResults.setVisibility(View.VISIBLE);

        etStoreName.setText(storeName.isEmpty() ? "Mağaza adı tapılmadı" : storeName);
        etDate.setText(receiptDate.isEmpty() ? new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date()) : receiptDate);
        etTime.setText(receiptTime.isEmpty() ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()) : receiptTime);

        productsContainer.removeAllViews();

        if (scannedProducts.isEmpty()) {
            addEmptyProductRow();
        } else {
            for (ProductItem product : scannedProducts) {
                addProductRow(product, false);
            }
        }

        // Əlavə et düyməsini əlavə et
        addAddProductButton();

        tvTotalAmount.setText(String.format(Locale.getDefault(), "₼%.2f", totalAmount));
        btnSave.setEnabled(true);

        cardResults.setAlpha(0f);
        cardResults.setTranslationY(50f);
        cardResults.animate().alpha(1f).translationY(0f).setDuration(400).start();
    }

    private void addProductRow(ProductItem product, boolean isNew) {
        View rowView = LayoutInflater.from(this).inflate(R.layout.item_product_drag, productsContainer, false);

        EditText etProductName = rowView.findViewById(R.id.etProductName);
        EditText etQuantity = rowView.findViewById(R.id.etQuantity);
        EditText etPrice = rowView.findViewById(R.id.etPrice);
        TextView tvTotal = rowView.findViewById(R.id.tvTotal);
        ImageView ivRemove = rowView.findViewById(R.id.ivRemove);
        ImageView ivDragHandle = rowView.findViewById(R.id.ivDragHandle);

        etProductName.setText(product.name);
        etQuantity.setText(String.valueOf(product.quantity));
        etPrice.setText(String.format(Locale.getDefault(), "%.2f", product.price));
        tvTotal.setText(String.format(Locale.getDefault(), "%.2f", product.total));

        // TextWatcher ilə dəyişiklikləri izlə
        TextWatcher textWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateProductTotal(rowView);
            }
        };

        etQuantity.addTextChangedListener(textWatcher);
        etPrice.addTextChangedListener(textWatcher);

        // Focus itirdikdə klaviaturanı gizlət
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v);
            }
        };

        etProductName.setOnFocusChangeListener(focusListener);
        etQuantity.setOnFocusChangeListener(focusListener);
        etPrice.setOnFocusChangeListener(focusListener);

        // Silmə düyməsi
        ivRemove.setOnClickListener(v -> {
            productsContainer.removeView(rowView);
            calculateGrandTotal();

            // Əgər bütün məhsullar silinibsə, boş sətir əlavə et
            if (productsContainer.getChildCount() <= 1) { // +1 əlavə et düyməsi üçün
                addEmptyProductRow();
            }
        });

        // Drag and drop üçün
        setupDragAndDrop(rowView, ivDragHandle);

        productsContainer.addView(rowView, productsContainer.getChildCount() - 1); // Əlavə et düyməsindən əvvəl əlavə et
    }

    private void addEmptyProductRow() {
        ProductItem emptyProduct = new ProductItem("", 0.0, 1.0, "ədəd", 0.0);
        addProductRow(emptyProduct, true);
    }

    private void addAddProductButton() {
        View addButton = LayoutInflater.from(this).inflate(R.layout.item_add_product, productsContainer, false);
        TextView tvAddProduct = addButton.findViewById(R.id.tvAddProduct);

        tvAddProduct.setOnClickListener(v -> {
            addEmptyProductRow();
            // Yeni əlavə olunan sətrə focus et
            mainScrollView.post(() -> mainScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });

        productsContainer.addView(addButton);
    }

    private void updateProductTotal(View rowView) {
        try {
            EditText etQuantity = rowView.findViewById(R.id.etQuantity);
            EditText etPrice = rowView.findViewById(R.id.etPrice);
            TextView tvTotal = rowView.findViewById(R.id.tvTotal);

            String qtyStr = etQuantity.getText().toString();
            String priceStr = etPrice.getText().toString();

            double quantity = qtyStr.isEmpty() ? 1.0 : Double.parseDouble(qtyStr.replace(",", "."));
            double price = priceStr.isEmpty() ? 0.0 : Double.parseDouble(priceStr.replace(",", "."));

            double total = quantity * price;
            tvTotal.setText(String.format(Locale.getDefault(), "%.2f", total));

            calculateGrandTotal();
        } catch (NumberFormatException e) {
            Log.e(TAG, "Cəm hesablanarkən xəta", e);
        }
    }

    private void calculateGrandTotal() {
        double grandTotal = 0.0;

        // Əlavə et düyməsini nəzərə alma
        for (int i = 0; i < productsContainer.getChildCount() - 1; i++) {
            View rowView = productsContainer.getChildAt(i);
            TextView tvTotal = rowView.findViewById(R.id.tvTotal);

            if (tvTotal != null) {
                String totalStr = tvTotal.getText().toString().replace("₼", "").replace(",", ".");
                try {
                    grandTotal += Double.parseDouble(totalStr);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Cəm parse xətası", e);
                }
            }
        }

        totalAmount = grandTotal;
        tvTotalAmount.setText(String.format(Locale.getDefault(), "₼%.2f", grandTotal));
    }

    private void setupDragAndDrop(View rowView, ImageView dragHandle) {
        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialY = event.getRawY();
                    draggedView = rowView;
                    draggedPosition = productsContainer.indexOfChild(rowView);
                    isDragging = true;

                    // Yüngül kölgə effekti
                    draggedView.setElevation(10f);
                    draggedView.setAlpha(0.9f);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isDragging && draggedView != null) {
                        float currentY = event.getRawY();
                        float deltaY = currentY - initialY;

                        // Müvəqqəti olaraq yeri dəyiş
                        int newPosition = calculateNewPosition(draggedPosition, deltaY);
                        if (newPosition != draggedPosition && newPosition >= 0 && newPosition < productsContainer.getChildCount() - 1) {
                            swapViews(draggedPosition, newPosition);
                            draggedPosition = newPosition;
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isDragging && draggedView != null) {
                        draggedView.setElevation(0f);
                        draggedView.setAlpha(1f);
                        draggedView = null;
                        isDragging = false;
                    }
                    return true;
            }
            return false;
        });
    }

    private int calculateNewPosition(int currentPos, float deltaY) {
        if (productsContainer.getChildCount() <= 1) return currentPos;

        float rowHeight = productsContainer.getChildAt(0).getHeight();
        int rowMoveThreshold = (int) (rowHeight * 0.5);

        if (deltaY > rowMoveThreshold && currentPos < productsContainer.getChildCount() - 2) {
            return currentPos + 1;
        } else if (deltaY < -rowMoveThreshold && currentPos > 0) {
            return currentPos - 1;
        }

        return currentPos;
    }

    private void swapViews(int from, int to) {
        if (from == to) return;

        View fromView = productsContainer.getChildAt(from);
        View toView = productsContainer.getChildAt(to);

        productsContainer.removeViewAt(from);
        productsContainer.removeViewAt(to > from ? to - 1 : to);

        productsContainer.addView(toView, from);
        productsContainer.addView(fromView, to > from ? to - 1 : to);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void saveDataAndReturn() {
        Intent resultIntent = new Intent();

        storeName = etStoreName.getText().toString();
        receiptDate = etDate.getText().toString();
        receiptTime = etTime.getText().toString();

        resultIntent.putExtra("store_name", storeName);
        resultIntent.putExtra("date", receiptDate);
        resultIntent.putExtra("time", receiptTime);
        resultIntent.putExtra("total_amount", totalAmount);
        resultIntent.putExtra("doc_id", qrDocId);

        ArrayList<String> productNames = new ArrayList<>();
        ArrayList<String> productPrices = new ArrayList<>();  // String olaraq saxla
        ArrayList<String> productQuantities = new ArrayList<>(); // String olaraq saxla
        ArrayList<String> productUnits = new ArrayList<>();

        // Məhsulları topla (əlavə et düyməsini nəzərə alma)
        for (int i = 0; i < productsContainer.getChildCount() - 1; i++) {
            View rowView = productsContainer.getChildAt(i);

            EditText etProductName = rowView.findViewById(R.id.etProductName);
            EditText etQuantity = rowView.findViewById(R.id.etQuantity);
            EditText etPrice = rowView.findViewById(R.id.etPrice);

            String name = etProductName.getText().toString().trim();
            if (name.isEmpty()) continue;

            try {
                double quantity = etQuantity.getText().toString().isEmpty() ?
                        1.0 : Double.parseDouble(etQuantity.getText().toString().replace(",", "."));
                double price = etPrice.getText().toString().isEmpty() ?
                        0.0 : Double.parseDouble(etPrice.getText().toString().replace(",", "."));

                productNames.add(name);
                productPrices.add(String.valueOf(price));  // String kimi əlavə et
                productQuantities.add(String.valueOf(quantity)); // String kimi əlavə et
                productUnits.add("ədəd");
            } catch (NumberFormatException e) {
                Log.e(TAG, "Məhsul məlumatları parse xətası", e);
            }
        }

        resultIntent.putStringArrayListExtra("product_names", productNames);
        resultIntent.putStringArrayListExtra("product_prices", productPrices);  // String ArrayList göndər
        resultIntent.putStringArrayListExtra("product_quantities", productQuantities); // String ArrayList göndər
        resultIntent.putStringArrayListExtra("product_units", productUnits);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void resetUI() {
        currentBitmap = null;
        ivReceiptImage.setImageDrawable(null);
        tvNoImageText.setVisibility(View.VISIBLE);
        cardResults.setVisibility(View.GONE);
        cardQrInfo.setVisibility(View.GONE);
        btnScan.setEnabled(false);
        btnScan.setAlpha(0.5f);
        btnSave.setEnabled(false);
        btnFetchFromEKassa.setEnabled(false);
        btnFetchFromEKassa.setAlpha(0.5f);
        scannedProducts.clear();
        productsContainer.removeAllViews();
        totalAmount = 0.0;
        storeName = "";
        receiptDate = "";
        receiptTime = "";
        qrDocId = "";

        etStoreName.setText("");
        etDate.setText("");
        etTime.setText("");
        tvQrStatus.setText("");
        tvDocId.setText("");
        etManualDocId.setText("");

        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (tvLoading != null) tvLoading.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressIndicator.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        tvLoading.setVisibility(View.GONE);
        tvQrStatus.setText("⚠️ " + message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Kamera icazəsi tələb olunur", Toast.LENGTH_SHORT).show();
                }
                break;

            case STORAGE_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery();
                } else {
                    Toast.makeText(this, "Yaddaş icazəsi tələb olunur", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realOCRHelper != null) realOCRHelper.close();
    }

    // Simple TextWatcher
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public abstract void afterTextChanged(Editable s);
    }
}