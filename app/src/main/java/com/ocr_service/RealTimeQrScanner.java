package com.ocr_service;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.smart_ai_sales.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RealTimeQrScanner extends AppCompatActivity {

    private static final String TAG = "RealTimeQR";
    private static final String E_KASSA_BASE_URL = "https://monitoring.e-kassa.gov.az/#/index?doc=";
    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final int GALLERY_REQUEST_CODE = 102;
    private static final int CAPTURE_IMAGE_REQUEST_CODE = 103;

    // Camera
    private PreviewView previewView;
    private boolean isScanning = true;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Camera camera;
    private ImageCapture imageCapture;
    private boolean isFlashOn = false;

    // UI Elements
    private MaterialCardView cardQrResult, cardManualInput;
    private TextView tvQrContent, tvQrStatus, tvDocId;
    private MaterialButton btnOpenBrowser, btnManualInput, btnAnalyzeImage, btnFlash, btnCaptureImage, btnGallery;
    private EditText etDocId;
    private ImageView btnBack;

    // QR Processing
    private long lastScanTime = 0;
    private static final long SCAN_INTERVAL = 2000;
    private String lastScannedQr = "";

    // Executor
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Image capture file
    private File outputDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_qr_scanner);

        outputDirectory = getOutputDirectory();
        initViews();
        setupClickListeners();
        checkCameraPermission();
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        cardQrResult = findViewById(R.id.cardQrResult);
        cardManualInput = findViewById(R.id.cardManualInput);
        tvQrContent = findViewById(R.id.tvQrContent);
        tvQrStatus = findViewById(R.id.tvQrStatus);
        tvDocId = findViewById(R.id.tvDocId);
        btnOpenBrowser = findViewById(R.id.btnOpenBrowser);
        btnManualInput = findViewById(R.id.btnManualInput);
        btnAnalyzeImage = findViewById(R.id.btnAnalyzeImage);
        btnFlash = findViewById(R.id.btnFlash);
        btnCaptureImage = findViewById(R.id.btnCaptureImage);
        btnGallery = findViewById(R.id.btnGallery);
        etDocId = findViewById(R.id.etDocId);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        cardQrResult.setVisibility(View.GONE);
        cardManualInput.setVisibility(View.GONE);
        tvQrStatus.setText("QR kod axtarılır...");
    }

    private void setupClickListeners() {
        btnOpenBrowser.setOnClickListener(v -> {
            String docId = tvDocId.getText().toString().replace("Sənəd ID: ", "");
            if (!docId.isEmpty()) {
                openInBrowser(docId);
            }
        });

        btnManualInput.setOnClickListener(v -> {
            cardManualInput.setVisibility(View.VISIBLE);
            cardQrResult.setVisibility(View.GONE);
        });

        btnAnalyzeImage.setOnClickListener(v -> {
            String manualDocId = etDocId.getText().toString().trim();
            if (!manualDocId.isEmpty()) {
                openInBrowser(manualDocId);
            } else {
                Toast.makeText(this, "Sənəd ID daxil edin", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnCloseManual).setOnClickListener(v -> {
            cardManualInput.setVisibility(View.GONE);
            etDocId.setText("");
        });

        // Flash düyməsi
        btnFlash.setOnClickListener(v -> toggleFlash());

        // Şəkil çək düyməsi
        btnCaptureImage.setOnClickListener(v -> {
            if (imageCapture != null) {
                captureImage();
            } else {
                Toast.makeText(this, "Kamera hazır deyil", Toast.LENGTH_SHORT).show();
            }
        });

        // Qalereyadan şəkil seç
        btnGallery.setOnClickListener(v -> {
            openGallery();
        });
    }

    private void toggleFlash() {
        if (camera != null) {
            if (isFlashOn) {
                camera.getCameraControl().enableTorch(false);
                btnFlash.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_flash_off));
                isFlashOn = false;
            } else {
                camera.getCameraControl().enableTorch(true);
                btnFlash.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_flash_on));
                isFlashOn = true;
            }
        }
    }

    private void captureImage() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        String fileName = "QR_" + dateFormat.format(System.currentTimeMillis()) + ".jpg";
        File photoFile = new File(outputDirectory, fileName);

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg = "Şəkil çəkildi: " + photoFile.getAbsolutePath();
                        Toast.makeText(RealTimeQrScanner.this, "Şəkil çəkildi, QR analiz edilir...", Toast.LENGTH_SHORT).show();

                        // Çəkilmiş şəkli QR oxu üçün analiz et
                        Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                        if (bitmap != null) {
                            analyzeBitmapForQR(bitmap);
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Şəkil çəkmə xətası: " + exception.getMessage());
                        Toast.makeText(RealTimeQrScanner.this, "Şəkil çəkmə xətası", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    private void analyzeBitmapForQR(Bitmap bitmap) {
        executorService.execute(() -> {
            String qrText = scanQRCode(bitmap);
            runOnUiThread(() -> {
                if (qrText != null && !qrText.isEmpty()) {
                    processQrCode(qrText);
                } else {
                    Toast.makeText(this, "Şəkildə QR kod tapılmadı", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private File getOutputDirectory() {
        File mediaDir = new File(getExternalFilesDir(null), "QR_Scans");
        if (!mediaDir.exists()) {
            mediaDir.mkdirs();
        }
        return mediaDir;
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        }
    }

    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) ProcessCameraProvider.getInstance(this).get();
                bindCameraUseCases(cameraProvider);
            } catch (Exception e) {
                Log.e(TAG, "Camera provider error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        // Preview
        Preview preview = new Preview.Builder()
                .build();

        // Camera selector
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Image Capture
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // Image Analysis - DÜZƏLİŞ: YUV_420_888 formatında analiz
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))  // Daha kiçik ölçü, daha sürətli
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(executorService, new QrCodeAnalyzer());

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        } catch (Exception e) {
            Log.e(TAG, "Camera bind error", e);
        }
    }

    /**
     * QR kod analizatoru - DÜZƏLİŞ: Birbaşa YUV formatından oxuyur
     */
    private class QrCodeAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            if (!isScanning) {
                image.close();
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastScanTime < SCAN_INTERVAL) {
                image.close();
                return;
            }

            try {
                // YUV formatından birbaşa QR oxu
                String qrText = scanQRCodeFromImage(image);

                if (qrText != null && !qrText.isEmpty() && !qrText.equals(lastScannedQr)) {
                    lastScanTime = currentTime;
                    lastScannedQr = qrText;
                    isScanning = false;

                    runOnUiThread(() -> processQrCode(qrText));

                    // 3 saniyə sonra scan-ı yenidən aktiv et
                    handler.postDelayed(() -> {
                        isScanning = true;
                        lastScannedQr = "";
                    }, 3000);
                }

                image.close();
            } catch (Exception e) {
                Log.e(TAG, "QR analysis error: " + e.getMessage());
                image.close();
            }
        }
    }

    /**
     * YÜKSƏK SÜRƏTLİ QR OXUMA - ImageProxy-dən birbaşa YUV formatında oxuyur
     */
    private String scanQRCodeFromImage(ImageProxy image) {
        try {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            if (planes.length < 3) return null;

            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int width = image.getWidth();
            int height = image.getHeight();

            // YUV420 formatında məlumatları düzgün birləşdir
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            // Y (luminance) məlumatını köçür
            yBuffer.get(nv21, 0, ySize);

            // U və V məlumatlarını düzgün qaydada köçür
            // NV21 formatında: YYYYYYYY... VUVU...
            // Android kamera adətən NV21 formatında məlumat verir

            // V məlumatını köçür (əvvəl)
            vBuffer.get(nv21, ySize, vSize);

            // U məlumatını köçür (sonra)
            uBuffer.get(nv21, ySize + vSize, uSize);

            // PlanarYUVLuminanceSource ilə birbaşa ZXing-ə ver
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    nv21,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false);

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            return result != null ? result.getText() : null;

        } catch (Exception e) {
            Log.e(TAG, "QR scan error: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Köhnə metod - ehtiyat üçün saxlanılır
     */
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
            return null;
        }
    }

    private void processQrCode(String qrText) {
        tvQrContent.setText(qrText);

        String docId = extractDocIdFromQR(qrText);

        if (!docId.isEmpty()) {
            tvDocId.setText("Sənəd ID: " + docId);
            tvQrStatus.setText("✅ QR kod tapıldı!");
            tvQrStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            cardQrResult.setVisibility(View.VISIBLE);

            // Vibrasiya
            try {
                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    vibrator.vibrate(200);
                }
            } catch (Exception e) {
                Log.e(TAG, "Vibrator error", e);
            }

            // Avtomatik browser-də aç
            openInBrowser(docId);
        } else {
            tvDocId.setText("Sənəd ID tapılmadı");
            tvQrStatus.setText("⚠️ QR kodda sənəd ID yoxdur");
            tvQrStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
            cardQrResult.setVisibility(View.VISIBLE);
        }
    }

    private String extractDocIdFromQR(String qrText) {
        if (qrText == null || qrText.isEmpty()) return "";

        // URL formatını yoxla
        Pattern pattern = Pattern.compile("[?&]doc=([^&]+)");
        Matcher matcher = pattern.matcher(qrText);

        if (matcher.find()) {
            return matcher.group(1);
        }

        // JSON formatını yoxla
        try {
            org.json.JSONObject json = new org.json.JSONObject(qrText);
            if (json.has("doc")) {
                return json.getString("doc");
            }
            if (json.has("docId")) {
                return json.getString("docId");
            }
            if (json.has("documentId")) {
                return json.getString("documentId");
            }
        } catch (Exception e) {
            // JSON deyil
        }

        // Birbaşa mətn formatı (8-25 simvol, alfasayısal)
        if (qrText.length() >= 8 && qrText.length() <= 25 &&
                qrText.matches("[A-Za-z0-9]+")) {
            return qrText;
        }

        return "";
    }

    private void openInBrowser(String docId) {
        String url = E_KASSA_BASE_URL + docId;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                InputStream imageStream = getContentResolver().openInputStream(selectedImage);
                Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                if (bitmap != null) {
                    Toast.makeText(this, "Şəkil import edildi, QR analiz edilir...", Toast.LENGTH_SHORT).show();
                    analyzeBitmapForQR(bitmap);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Şəkil tapılmadı", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Kamera icazəsi tələb olunur", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        handler.removeCallbacksAndMessages(null);
    }
}