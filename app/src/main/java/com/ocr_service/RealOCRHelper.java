package com.ocr_service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Təkmilləşdirilmiş OCR Helper - Bütün çeki oxuyur
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

        // Şəkili emal üçün hazırla
        Bitmap processedBitmap = preprocessBitmap(bitmap);

        InputImage image = InputImage.fromBitmap(processedBitmap, 0);

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<TextBlock> textBlocks = new ArrayList<>();

                    // Bütün mətn bloklarını topla
                    for (Text.TextBlock block : visionText.getTextBlocks()) {
                        for (Text.Line line : block.getLines()) {
                            String lineText = line.getText().trim();
                            Rect boundingBox = line.getBoundingBox();

                            if (boundingBox != null && !lineText.isEmpty()) {
                                textBlocks.add(new TextBlock(lineText, boundingBox));
                                Log.d(TAG, "OCR Line: '" + lineText + "'");
                            }
                        }
                    }

                    // Y koordinatına görə sırala (yuxarıdan aşağı)
                    Collections.sort(textBlocks, new Comparator<TextBlock>() {
                        @Override
                        public int compare(TextBlock b1, TextBlock b2) {
                            return Integer.compare(b1.rect.top, b2.rect.top);
                        }
                    });

                    Log.d(TAG, "Total " + textBlocks.size() + " text blocks found");
                    callback.onSuccess(textBlocks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "OCR error: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    /**
     * Şəkili OCR üçün optimallaşdır
     */
    private Bitmap preprocessBitmap(Bitmap original) {
        try {
            // Kontrast artır, ağ-qara et və s.
            return original;
        } catch (Exception e) {
            return original;
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