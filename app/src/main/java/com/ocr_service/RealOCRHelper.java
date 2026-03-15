package com.ocr_service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Təkmilləşdirilmiş OCR Helper - SUPER GÜCLÜ VERSİYA
 */
public class RealOCRHelper {
    private static final String TAG = "RealOCRHelper";
    private TextRecognizer textRecognizer;

    public static class TextBlock {
        public String text;
        public Rect rect;
        public float confidence;

        public TextBlock(String text, Rect rect, float confidence) {
            this.text = text;
            this.rect = rect;
            this.confidence = confidence;
        }

        public TextBlock(String text, Rect rect) {
            this(text, rect, 1.0f);
        }

        @Override
        public String toString() {
            return String.format("'%s' at [%d,%d,%d,%d]",
                    text, rect.left, rect.top, rect.right, rect.bottom);
        }
    }

    public interface OCRCallback {
        void onSuccess(List<TextBlock> textBlocks);
        void onError(String error);
    }

    public RealOCRHelper(Context context) {
        try {
            textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            Log.d(TAG, "ML Kit Text Recognizer started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error starting ML Kit: " + e.getMessage());
            throw e;
        }
    }

    public void detectText(Bitmap bitmap, OCRCallback callback) {
        if (bitmap == null) {
            callback.onError("Bitmap is null");
            return;
        }

        if (textRecognizer == null) {
            callback.onError("TextRecognizer not initialized");
            return;
        }

        // 1. CƏHD: Güclü preprocessing ilə
        tryWithPreprocessing(bitmap, callback, 0);
    }

    private void tryWithPreprocessing(Bitmap bitmap, OCRCallback callback, int attempt) {
        List<Bitmap> variants = new ArrayList<>();

        switch (attempt) {
            case 0:
                // 1-ci cəhd: Orijinal + böyütmə
                variants.add(preprocessBitmap(bitmap, 0));
                break;
            case 1:
                // 2-ci cəhd: Ağ-qara + yüksək kontrast
                variants.add(preprocessBitmap(bitmap, 1));
                break;
            case 2:
                // 3-ci cəhd: Tersinə çevrilmiş (ağ fon, qara mətn)
                variants.add(preprocessBitmap(bitmap, 2));
                break;
            case 3:
                // 4-ci cəhd: Kəskinləşdirilmiş
                variants.add(preprocessBitmap(bitmap, 3));
                break;
            case 4:
                // 5-ci cəhd: Parlaq
                variants.add(preprocessBitmap(bitmap, 4));
                break;
            default:
                // Bütün cəhdlər uğursuz oldusa, orijinal ilə cəhd et
                tryWithOriginal(bitmap, callback);
                return;
        }

        processVariant(variants.get(0), bitmap, callback, attempt);
    }

    private void processVariant(Bitmap variant, Bitmap original, OCRCallback callback, int attempt) {
        InputImage image = InputImage.fromBitmap(variant, 0);

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<TextBlock> textBlocks = new ArrayList<>();

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            String lineText = line.getText().trim();
                            Rect boundingBox = line.getBoundingBox();

                            if (boundingBox != null && !lineText.isEmpty() && lineText.length() > 1) {
                                textBlocks.add(new TextBlock(lineText, boundingBox));
                                Log.d(TAG, "OCR Line (cəhd " + attempt + "): '" + lineText + "'");
                            }
                        }
                    }

                    Collections.sort(textBlocks, new Comparator<TextBlock>() {
                        @Override
                        public int compare(TextBlock b1, TextBlock b2) {
                            return Integer.compare(b1.rect.top, b2.rect.top);
                        }
                    });

                    Log.d(TAG, "Cəhd " + attempt + ": " + textBlocks.size() + " blok tapıldı");

                    // Əgər kifayət qədər blok tapıldısa (10+), uğurlu say
                    if (textBlocks.size() >= 10) {
                        if (variant != original) {
                            variant.recycle();
                        }
                        callback.onSuccess(textBlocks);
                    } else {
                        // Az blok tapıldısa, növbəti cəhdə keç
                        if (variant != original) {
                            variant.recycle();
                        }
                        Log.d(TAG, "Cəhd " + attempt + " yetərli deyil (" + textBlocks.size() + "), növbəti cəhdə keçilir");
                        tryWithPreprocessing(original, callback, attempt + 1);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR error (cəhd " + attempt + "): " + e.getMessage());
                    if (variant != original) {
                        variant.recycle();
                    }
                    tryWithPreprocessing(original, callback, attempt + 1);
                });
    }

    private void tryWithOriginal(Bitmap bitmap, OCRCallback callback) {
        Log.d(TAG, "Son cəhd: Orijinal bitmap");

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<TextBlock> textBlocks = new ArrayList<>();

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            String lineText = line.getText().trim();
                            Rect boundingBox = line.getBoundingBox();

                            if (boundingBox != null && !lineText.isEmpty() && lineText.length() > 1) {
                                textBlocks.add(new TextBlock(lineText, boundingBox));
                                Log.d(TAG, "OCR Line (son): '" + lineText + "'");
                            }
                        }
                    }

                    Collections.sort(textBlocks, new Comparator<TextBlock>() {
                        @Override
                        public int compare(TextBlock b1, TextBlock b2) {
                            return Integer.compare(b1.rect.top, b2.rect.top);
                        }
                    });

                    Log.d(TAG, "Son cəhd: " + textBlocks.size() + " blok tapıldı");
                    callback.onSuccess(textBlocks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR error (son): " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Şəkili OCR üçün optimallaşdır - 5 FƏRQLİ ÜSUL
     */
    private Bitmap preprocessBitmap(Bitmap original, int method) {
        try {
            // 1. Şəkili böyüt (əsas)
            Bitmap scaledBitmap;
            int targetSize = 1200; // Daha böyük hədəf

            if (original.getWidth() < targetSize || original.getHeight() < targetSize) {
                float scale = Math.max(
                        (float) targetSize / original.getWidth(),
                        (float) targetSize / original.getHeight()
                );
                int newWidth = (int) (original.getWidth() * scale);
                int newHeight = (int) (original.getHeight() * scale);
                scaledBitmap = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
                Log.d(TAG, "Method " + method + ": Scaled to " + newWidth + "x" + newHeight);
            } else {
                scaledBitmap = Bitmap.createBitmap(original);
            }

            // 2. Metoda görə preprocessing
            Bitmap processed = Bitmap.createBitmap(scaledBitmap.getWidth(), scaledBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            int[] pixels = new int[scaledBitmap.getWidth() * scaledBitmap.getHeight()];
            scaledBitmap.getPixels(pixels, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());

            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                // Ağ-qara çevir
                int gray = (r + g + b) / 3;

                switch (method) {
                    case 0: // Method 0: Orijinal (sizin kodunuz)
                        processed = scaledBitmap;
                        continue;

                    case 1: // Method 1: Ağ-qara + yüksək kontrast
                        // Kontrast artır
                        if (gray < 128) {
                            gray = (int) (gray * 2.7); // Qaraları daha qara
                        } else {
                            gray = (int) (gray * 2.7); // Ağları daha ağ
                        }
                        gray = Math.min(255, Math.max(0, gray));
                        break;

                    case 2: // Method 2: Tersinə çevrilmiş (ağ fon, qara mətn)
                        gray = 255 - gray;
                        // Kontrast artır
                        if (gray < 128) {
                            gray = (int) (gray * 2.7);
                        } else {
                            gray = (int) (gray * 2.7);
                        }
                        gray = Math.min(255, Math.max(0, gray));
                        break;

                    case 3: // Method 3: Kəskinləşdirilmiş
                        // Kənarları kəskinləşdir
                        if (gray > 200) {
                            gray = 255; // Ağları tam ağ et
                        } else if (gray < 50) {
                            gray = 0;   // Qaraları tam qara et
                        }
                        break;

                    case 4: // Method 4: Parlaq
                        gray = (int) (gray * 1.2);
                        gray = Math.min(255, gray);
                        break;
                }

                pixels[i] = Color.argb(Color.alpha(pixel), gray, gray, gray);
            }

            if (method != 0) {
                processed.setPixels(pixels, 0, processed.getWidth(), 0, 0, processed.getWidth(), processed.getHeight());
            }

            if (scaledBitmap != original && method != 0) {
                scaledBitmap.recycle();
            }

            return processed;

        } catch (Exception e) {
            Log.e(TAG, "Preprocessing xətası (method " + method + "): " + e.getMessage());
            return original;
        }
    }

    /**
     * Şəkili fayldan yüklə
     */
    public static Bitmap loadBitmap(String path) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);

            if (bitmap == null) return null;

            // EXIF məlumatlarını oxu
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            return rotateBitmap(bitmap, orientation);

        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmap: " + e.getMessage());
            return null;
        }
    }

    /**
     * Şəkili fayldan yüklə və orientasiyanı düzəlt
     */
    public static Bitmap loadAndCorrectOrientation(String path) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);

            if (bitmap == null) return null;

            // EXIF məlumatlarını oxu
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            return rotateBitmap(bitmap, orientation);

        } catch (IOException e) {
            Log.e(TAG, "Şəkil yüklənərkən xəta: " + e.getMessage());
            return null;
        }
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }

        try {
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            return rotated;
        } catch (Exception e) {
            return bitmap;
        }
    }

    public void close() {
        if (textRecognizer != null) {
            try {
                textRecognizer.close();
                Log.d(TAG, "TextRecognizer closed");
            } catch (Exception e) {
                Log.e(TAG, "Error closing TextRecognizer: " + e.getMessage());
            }
        }
    }
}