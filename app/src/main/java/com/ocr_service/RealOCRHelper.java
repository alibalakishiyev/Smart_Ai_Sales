package com.ocr_service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Təkmilləşdirilmiş OCR Helper - KEYFİYYƏTLİ YÜKLƏMƏ
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

        Log.d(TAG, "Bitmap size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
        Log.d(TAG, "Bitmap config: " + bitmap.getConfig());

        // Şəkli 3000px-ə qədər böyüt (0.67KB üçün çox böyütmə lazımdır)
        Bitmap scaledBitmap = scaleBitmap(bitmap, 3000);

        Log.d(TAG, "Scaled: " + scaledBitmap.getWidth() + "x" + scaledBitmap.getHeight());

        InputImage image = InputImage.fromBitmap(scaledBitmap, 0);

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<TextBlock> textBlocks = new ArrayList<>();

                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            String lineText = line.getText().trim();
                            Rect boundingBox = line.getBoundingBox();

                            if (boundingBox != null && !lineText.isEmpty() && lineText.length() > 1) {
                                textBlocks.add(new TextBlock(lineText, boundingBox));
                                Log.d(TAG, "OCR Line: '" + lineText + "'");
                            }
                        }
                    }

                    Collections.sort(textBlocks, new Comparator<TextBlock>() {
                        @Override
                        public int compare(TextBlock b1, TextBlock b2) {
                            return Integer.compare(b1.rect.top, b2.rect.top);
                        }
                    });

                    Log.d(TAG, "Total " + textBlocks.size() + " text blocks found");

                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle();
                    }

                    callback.onSuccess(textBlocks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR error: " + e.getMessage());

                    if (scaledBitmap != bitmap) {
                        scaledBitmap.recycle();
                    }

                    tryWithOriginal(bitmap, callback);
                });
    }

    private void tryWithOriginal(Bitmap bitmap, OCRCallback callback) {
        Log.d(TAG, "Trying with original bitmap...");

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
                                Log.d(TAG, "OCR Line (original): '" + lineText + "'");
                            }
                        }
                    }

                    Collections.sort(textBlocks, new Comparator<TextBlock>() {
                        @Override
                        public int compare(TextBlock b1, TextBlock b2) {
                            return Integer.compare(b1.rect.top, b2.rect.top);
                        }
                    });

                    Log.d(TAG, "Total with original: " + textBlocks.size() + " text blocks found");
                    callback.onSuccess(textBlocks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR error with original: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Şəkili hədəf ölçüyə qədər böyüt
     */
    private Bitmap scaleBitmap(Bitmap original, int targetSize) {
        try {
            int width = original.getWidth();
            int height = original.getHeight();
            int maxDimension = Math.max(width, height);

            // Əgər şəkil çox kiçikdirsə (0.67KB), çox böyüt
            if (maxDimension < 500) {
                float scale = (float) targetSize / maxDimension;
                int newWidth = (int) (width * scale);
                int newHeight = (int) (height * scale);

                Log.d(TAG, "Very small image, scaling by factor: " + scale);

                // Filter = true (keyfiyyətli böyütmə)
                return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
            }
            // Orta ölçülü şəkil
            else if (maxDimension < 1000) {
                float scale = 2.0f;
                int newWidth = (int) (width * scale);
                int newHeight = (int) (height * scale);

                Log.d(TAG, "Medium image, scaling by factor: " + scale);
                return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
            }

            return original;

        } catch (Exception e) {
            Log.e(TAG, "Scale error: " + e.getMessage());
            return original;
        }
    }

    /**
     * Şəkili fayldan yüklə - KEYFİYYƏTLİ YÜKLƏMƏ
     */
    public static Bitmap loadHighQualityBitmap(String path) {
        try {
            File file = new File(path);
            Log.d(TAG, "File size: " + file.length() + " bytes");

            // 1. Yalnız ölçüləri öyrən
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            int imageWidth = options.outWidth;
            int imageHeight = options.outHeight;

            Log.d(TAG, "Image dimensions: " + imageWidth + "x" + imageHeight);

            // 2. Böyük ölçüdə yüklə
            options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888; // Yüksək keyfiyyət
            options.inDither = true; // Rəng keçidlərini yumşalt
            options.inScaled = false; // Ölçüsünü dəyişmə
            options.inPremultiplied = true;

            Bitmap bitmap = BitmapFactory.decodeFile(path, options);

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap");
                return null;
            }

            Log.d(TAG, "Loaded bitmap: " + bitmap.getWidth() + "x" + bitmap.getHeight() +
                    ", config: " + bitmap.getConfig() + ", size: " + (bitmap.getByteCount() / 1024) + "KB");

            // EXIF məlumatlarını oxu
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            return rotateBitmap(bitmap, orientation);

        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmap: " + e.getMessage());
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