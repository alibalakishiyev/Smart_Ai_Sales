package com.ocr_service;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.smart_ai_sales.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptScannerActivity extends AppCompatActivity {

    private static final String TAG = "ReceiptScan";

    // Request codes
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;
    private static final int CAMERA_PERMISSION_CODE = 103;
    private static final int STORAGE_PERMISSION_CODE = 104;

    // UI Elements
    private ImageView ivReceiptImage;
    private TextView tvNoImageText;
    private CardView cardImage, cardResults;
    private LinearLayout productsContainer;
    private LinearProgressIndicator progressIndicator;
    private LottieAnimationView animationView;
    private MaterialButton btnScan, btnSave, btnRetake;
    private FloatingActionButton fabCamera, fabGallery;
    private TextView tvTotalAmount;
    private EditText etStoreName, etDate, etTime;

    // OCR Helper
    private PaddleOCRHelper paddleOCRHelper;

    // Data
    private Bitmap currentBitmap;
    private Uri currentPhotoUri;
    private String currentPhotoPath;
    private List<ProductItem> scannedProducts = new ArrayList<>();
    private double totalAmount = 0.0;
    private String storeName = "";
    private String receiptDate = "";
    private String receiptTime = "";

    // Simple TextWatcher abstract class
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public abstract void afterTextChanged(Editable s);
    }

    // Product item class
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
        cardImage = findViewById(R.id.cardImage);
        cardResults = findViewById(R.id.cardResults);
        productsContainer = findViewById(R.id.productsContainer);
        progressIndicator = findViewById(R.id.progressIndicator);
        animationView = findViewById(R.id.animationView);
        btnScan = findViewById(R.id.btnScan);
        btnSave = findViewById(R.id.btnSave);
        btnRetake = findViewById(R.id.btnRetake);
        fabCamera = findViewById(R.id.fabCamera);
        fabGallery = findViewById(R.id.fabGallery);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);

        // EditText-lər
        etStoreName = findViewById(R.id.etStoreName);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);

        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        // Initially hide results card
        cardResults.setVisibility(View.GONE);
        btnSave.setEnabled(false);
    }

    private void initOCR() {
        try {
            paddleOCRHelper = new PaddleOCRHelper(this);
            Log.d(TAG, "PaddleOCR initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing PaddleOCR: " + e.getMessage());
            Toast.makeText(this, "OCR yüklənmədi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupClickListeners() {
        fabCamera.setOnClickListener(v -> checkCameraPermission());
        fabGallery.setOnClickListener(v -> checkStoragePermission());

        btnScan.setOnClickListener(v -> {
            if (currentBitmap != null) {
                performOCR();
            } else {
                Toast.makeText(this, "Əvvəlcə şəkil seçin", Toast.LENGTH_SHORT).show();
            }
        });

        btnRetake.setOnClickListener(v -> {
            resetUI();
            showImageSourceDialog();
        });

        btnSave.setOnClickListener(v -> saveDataAndReturn());

        // Text watchers for manual editing - SimpleTextWatcher istifadə edirik
        etStoreName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                storeName = s.toString();
            }
        });

        etDate.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                receiptDate = s.toString();
            }
        });

        etTime.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                receiptTime = s.toString();
            }
        });
    }

    private void animateViews() {
        if (animationView != null) {
            animationView.setVisibility(View.VISIBLE);
            animationView.playAnimation();

            cardImage.setAlpha(0f);
            cardImage.setTranslationY(50f);
            cardImage.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(200)
                    .start();

            new android.os.Handler().postDelayed(() -> {
                animationView.cancelAnimation();
                animationView.setVisibility(View.GONE);
            }, 2000);
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        }
    }

    private void checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
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
                Log.e(TAG, "Error creating file", ex);
            }

            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
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
                        case 0:
                            checkCameraPermission();
                            break;
                        case 1:
                            checkStoragePermission();
                            break;
                        case 2:
                            dialog.dismiss();
                            break;
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
                        Log.e(TAG, "Error loading camera image", e);
                        Toast.makeText(this, "Şəkil yüklənmədi", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case REQUEST_IMAGE_PICK:
                    if (data != null && data.getData() != null) {
                        Uri imageUri = data.getData();
                        try {
                            currentBitmap = MediaStore.Images.Media.getBitmap(
                                    getContentResolver(), imageUri);
                            displayImage(currentBitmap);
                        } catch (IOException e) {
                            Log.e(TAG, "Error loading gallery image", e);
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

            // Animasiya
            cardImage.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200)
                    .withEndAction(() -> cardImage.animate().scaleX(1f).scaleY(1f).setDuration(200).start());
        }
    }

    private void performOCR() {
        if (currentBitmap == null || paddleOCRHelper == null) {
            Toast.makeText(this, "OCR hazır deyil", Toast.LENGTH_SHORT).show();
            return;
        }

        progressIndicator.setVisibility(View.VISIBLE);
        btnScan.setEnabled(false);
        if (animationView != null) {
            animationView.setVisibility(View.VISIBLE);
            animationView.playAnimation();
        }

        // OCR işini background thread-də apar
        new Thread(() -> {
            try {
                // PaddleOCR ilə text detection
                List<PaddleOCRHelper.TextBlock> textBlocks = paddleOCRHelper.detectText(currentBitmap);

                Log.d(TAG, "Found " + textBlocks.size() + " text blocks");

                // OCR nəticələrini topla
                StringBuilder stringBuilder = new StringBuilder();
                for (PaddleOCRHelper.TextBlock block : textBlocks) {
                    stringBuilder.append(block.text);
                    stringBuilder.append("\n");
                    Log.d(TAG, "Text: " + block.text + " at " + block.rect.x + "," + block.rect.y);
                }

                String ocrText = stringBuilder.toString();
                Log.d(TAG, "OCR Text: " + ocrText);

                // OCR nəticələrini parse et
                parseReceiptData(ocrText, textBlocks);

                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnScan.setEnabled(true);
                    if (animationView != null) {
                        animationView.cancelAnimation();
                        animationView.setVisibility(View.GONE);
                    }
                    displayResults();
                });

            } catch (Exception e) {
                Log.e(TAG, "OCR error", e);
                runOnUiThread(() -> {
                    Toast.makeText(ReceiptScannerActivity.this,
                            "OCR xətası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressIndicator.setVisibility(View.GONE);
                    btnScan.setEnabled(true);
                    if (animationView != null) {
                        animationView.cancelAnimation();
                        animationView.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    private void parseReceiptData(String text, List<PaddleOCRHelper.TextBlock> textBlocks) {
        scannedProducts.clear();
        totalAmount = 0.0;

        String[] lines = text.split("\n");

        // Mağaza adını tap (ən böyük və ən yuxarıdakı text)
        for (PaddleOCRHelper.TextBlock block : textBlocks) {
            if (block.rect.y < 100 && block.text.length() > 3) {
                storeName = block.text.trim();
                break;
            }
        }

        // Tarixi tap
        Pattern datePattern = Pattern.compile("(\\d{2}[./-]\\d{2}[./-]\\d{2,4})");
        for (String line : lines) {
            Matcher matcher = datePattern.matcher(line);
            if (matcher.find()) {
                receiptDate = matcher.group(1);
                break;
            }
        }

        // Saati tap
        Pattern timePattern = Pattern.compile("(\\d{2}:\\d{2})");
        for (String line : lines) {
            Matcher matcher = timePattern.matcher(line);
            if (matcher.find()) {
                receiptTime = matcher.group(1);
                break;
            }
        }

        // Məhsulları tap
        Pattern productPattern = Pattern.compile(
                "([A-Za-zıəüğöçşİƏÜĞÖÇŞ0-9\\s\\-]{3,30})\\s+" +
                        "(\\d+[.,]?\\d*)\\s*[xX*]?\\s*(\\d+[.,]?\\d*)?\\s*([\\d.,]+)");

        Pattern azProductPattern = Pattern.compile(
                "([A-Za-zıəüğöçşİƏÜĞÖÇŞ0-9\\s\\-]{3,30})\\s+" +
                        "(\\d+[.,]?\\d*)\\s*(AZN|₼|manat)?", Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            Matcher matcher = productPattern.matcher(line);
            if (matcher.find()) {
                String productName = matcher.group(1).trim();
                String priceStr = matcher.group(4) != null ? matcher.group(4) : matcher.group(2);

                try {
                    double quantity = 1.0;
                    double price = Double.parseDouble(priceStr.replace(",", ".").replace("₼", "").trim());
                    double total = price;

                    if (matcher.group(3) != null && !matcher.group(3).isEmpty()) {
                        quantity = Double.parseDouble(matcher.group(2));
                        price = Double.parseDouble(matcher.group(3).replace(",", "."));
                        total = quantity * price;
                    }

                    productName = productName.replaceAll("[^A-Za-zıəüğöçşİƏÜĞÖÇŞ\\s]", "").trim();

                    if (!productName.isEmpty() && price > 0) {
                        scannedProducts.add(new ProductItem(
                                productName,
                                price,
                                quantity,
                                "ədəd",
                                total
                        ));
                        totalAmount += total;
                        Log.d(TAG, "Found product: " + productName + " - " + price);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Parse error: " + line);
                }
            } else {
                Matcher azMatcher = azProductPattern.matcher(line);
                if (azMatcher.find()) {
                    String productName = azMatcher.group(1).trim();
                    String priceStr = azMatcher.group(2);

                    try {
                        double price = Double.parseDouble(priceStr.replace(",", "."));

                        productName = productName.replaceAll("[^A-Za-zıəüğöçşİƏÜĞÖÇŞ\\s]", "").trim();

                        if (!productName.isEmpty() && price > 0) {
                            scannedProducts.add(new ProductItem(
                                    productName,
                                    price,
                                    1.0,
                                    "ədəd",
                                    price
                            ));
                            totalAmount += price;
                            Log.d(TAG, "Found AZ product: " + productName + " - " + price);
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "AZ parse error: " + line);
                    }
                }
            }
        }

        Pattern totalPattern = Pattern.compile("(CƏMİ|TOPLAM|Yekun|ÜMUMİ|TOTAL)\\s*[:]?\\s*([\\d.,]+)\\s*(AZN|₼|manat)?",
                Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            Matcher matcher = totalPattern.matcher(line);
            if (matcher.find()) {
                try {
                    double total = Double.parseDouble(matcher.group(2).replace(",", "."));
                    if (total > 0) {
                        totalAmount = total;
                        Log.d(TAG, "Found total amount: " + total);
                        break;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Total parse error");
                }
            }
        }
    }

    private void displayResults() {
        cardResults.setVisibility(View.VISIBLE);

        etStoreName.setText(storeName.isEmpty() ? "Mağaza adı tapılmadı" : storeName);

        if (!receiptDate.isEmpty()) {
            etDate.setText(receiptDate);
        } else {
            etDate.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date()));
        }

        if (!receiptTime.isEmpty()) {
            etTime.setText(receiptTime);
        } else {
            etTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        }

        productsContainer.removeAllViews();

        if (scannedProducts.isEmpty()) {
            addEmptyProductRow();
            Toast.makeText(this, "Heç bir məhsul tapılmadı, əl ilə daxil edin", Toast.LENGTH_LONG).show();
        } else {
            for (ProductItem product : scannedProducts) {
                addProductRow(product);
            }
        }

        tvTotalAmount.setText(String.format(Locale.getDefault(), "₼%.2f", totalAmount));

        btnSave.setEnabled(true);

        cardResults.setAlpha(0f);
        cardResults.setTranslationY(50f);
        cardResults.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .start();
    }

    private void addProductRow(ProductItem product) {
        View rowView = LayoutInflater.from(this).inflate(R.layout.item_product_edit, productsContainer, false);

        EditText etProductName = rowView.findViewById(R.id.etProductName);
        EditText etQuantity = rowView.findViewById(R.id.etQuantity);
        EditText etPrice = rowView.findViewById(R.id.etPrice);
        EditText etTotal = rowView.findViewById(R.id.etTotal);
        ImageView ivRemove = rowView.findViewById(R.id.ivRemove);
        TextView tvUnit = rowView.findViewById(R.id.tvUnit);
        ImageView ivAdd = rowView.findViewById(R.id.ivAdd);

        etProductName.setText(product.name);
        etQuantity.setText(String.valueOf(product.quantity));
        etPrice.setText(String.format(Locale.getDefault(), "%.2f", product.price));
        etTotal.setText(String.format(Locale.getDefault(), "₼%.2f", product.total));

        TextWatcher priceWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateTotal(rowView);
            }
        };

        etQuantity.addTextChangedListener(priceWatcher);
        etPrice.addTextChangedListener(priceWatcher);

        ivRemove.setOnClickListener(v -> {
            if (productsContainer.getChildCount() > 1) {
                productsContainer.removeView(rowView);
                calculateGrandTotal();
            } else {
                Toast.makeText(this, "Ən azı bir məhsul olmalıdır", Toast.LENGTH_SHORT).show();
            }
        });

        ivAdd.setOnClickListener(v -> addEmptyProductRow());

        productsContainer.addView(rowView);
    }

    private void addEmptyProductRow() {
        View rowView = LayoutInflater.from(this).inflate(R.layout.item_product_edit, productsContainer, false);

        EditText etProductName = rowView.findViewById(R.id.etProductName);
        EditText etQuantity = rowView.findViewById(R.id.etQuantity);
        EditText etPrice = rowView.findViewById(R.id.etPrice);
        EditText etTotal = rowView.findViewById(R.id.etTotal);
        ImageView ivRemove = rowView.findViewById(R.id.ivRemove);
        TextView tvUnit = rowView.findViewById(R.id.tvUnit);
        ImageView ivAdd = rowView.findViewById(R.id.ivAdd);

        etProductName.setHint("Məhsul adı");
        etQuantity.setText("1");
        etPrice.setText("0");
        etTotal.setText("₼0.00");

        TextWatcher priceWatcher = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateTotal(rowView);
            }
        };

        etQuantity.addTextChangedListener(priceWatcher);
        etPrice.addTextChangedListener(priceWatcher);

        ivRemove.setOnClickListener(v -> {
            if (productsContainer.getChildCount() > 1) {
                productsContainer.removeView(rowView);
                calculateGrandTotal();
            } else {
                Toast.makeText(this, "Ən azı bir məhsul olmalıdır", Toast.LENGTH_SHORT).show();
            }
        });

        ivAdd.setOnClickListener(v -> addEmptyProductRow());

        productsContainer.addView(rowView);
    }

    private void updateTotal(View rowView) {
        try {
            EditText etQuantity = rowView.findViewById(R.id.etQuantity);
            EditText etPrice = rowView.findViewById(R.id.etPrice);
            EditText etTotal = rowView.findViewById(R.id.etTotal);

            String qtyStr = etQuantity.getText().toString();
            String priceStr = etPrice.getText().toString();

            double quantity = qtyStr.isEmpty() ? 1.0 : Double.parseDouble(qtyStr);
            double price = priceStr.isEmpty() ? 0.0 : Double.parseDouble(priceStr.replace(",", "."));

            double total = quantity * price;
            etTotal.setText(String.format(Locale.getDefault(), "₼%.2f", total));

            calculateGrandTotal();
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error updating total", e);
        }
    }

    private void calculateGrandTotal() {
        double grandTotal = 0.0;

        for (int i = 0; i < productsContainer.getChildCount(); i++) {
            View rowView = productsContainer.getChildAt(i);
            EditText etTotal = rowView.findViewById(R.id.etTotal);
            String totalStr = etTotal.getText().toString().replace("₼", "").replace(",", ".");

            try {
                grandTotal += Double.parseDouble(totalStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing total", e);
            }
        }

        totalAmount = grandTotal;
        tvTotalAmount.setText(String.format(Locale.getDefault(), "₼%.2f", grandTotal));
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

        ArrayList<String> productNames = new ArrayList<>();
        ArrayList<Double> productPrices = new ArrayList<>();
        ArrayList<Double> productQuantities = new ArrayList<>();
        ArrayList<String> productUnits = new ArrayList<>();

        for (int i = 0; i < productsContainer.getChildCount(); i++) {
            View rowView = productsContainer.getChildAt(i);

            EditText etProductName = rowView.findViewById(R.id.etProductName);
            EditText etQuantity = rowView.findViewById(R.id.etQuantity);
            EditText etPrice = rowView.findViewById(R.id.etPrice);
            TextView tvUnit = rowView.findViewById(R.id.tvUnit);

            String name = etProductName.getText().toString().trim();
            if (name.isEmpty()) continue;

            try {
                double quantity = etQuantity.getText().toString().isEmpty() ?
                        1.0 : Double.parseDouble(etQuantity.getText().toString());
                double price = etPrice.getText().toString().isEmpty() ?
                        0.0 : Double.parseDouble(etPrice.getText().toString().replace(",", "."));

                productNames.add(name);
                productPrices.add(price);
                productQuantities.add(quantity);
                productUnits.add(tvUnit.getText().toString());

            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing product data", e);
            }
        }

        resultIntent.putStringArrayListExtra("product_names", productNames);
        resultIntent.putExtra("product_prices", productPrices);
        resultIntent.putExtra("product_quantities", productQuantities);
        resultIntent.putStringArrayListExtra("product_units", productUnits);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void resetUI() {
        currentBitmap = null;
        ivReceiptImage.setImageDrawable(null);
        tvNoImageText.setVisibility(View.VISIBLE);
        cardResults.setVisibility(View.GONE);
        btnScan.setEnabled(false);
        btnScan.setAlpha(0.5f);
        btnSave.setEnabled(false);
        scannedProducts.clear();
        productsContainer.removeAllViews();
        totalAmount = 0.0;
        storeName = "";
        receiptDate = "";
        receiptTime = "";

        etStoreName.setText("");
        etDate.setText("");
        etTime.setText("");
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
        if (paddleOCRHelper != null) {
            paddleOCRHelper.close();
        }
    }
}