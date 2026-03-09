package com.ocr_service;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class PaddleOCRHelper {
    private static final String TAG = "PaddleOCRHelper";
    private static final String DETECTION_MODEL = "paddle_ocr_det.tflite";
    private static final String RECOGNITION_MODEL = "paddle_ocr_rec.tflite";

    private Interpreter detectionInterpreter;
    private Interpreter recognitionInterpreter;
    private Context context;

    // Model input dimensions
    private static final int DETECT_INPUT_SIZE = 640;
    private static final int REC_INPUT_HEIGHT = 48;
    private static final int REC_INPUT_WIDTH = 320;

    public PaddleOCRHelper(Context context) {
        this.context = context;
        loadModels();
    }

    private void loadModels() {
        try {
            // Detection model
            MappedByteBuffer detModel = FileUtil.loadMappedFile(context, DETECTION_MODEL);
            detectionInterpreter = new Interpreter(detModel);

            // Recognition model
            MappedByteBuffer recModel = FileUtil.loadMappedFile(context, RECOGNITION_MODEL);
            recognitionInterpreter = new Interpreter(recModel);

            Log.d(TAG, "Models loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error loading models: " + e.getMessage());
        }
    }

    public List<TextBlock> detectText(Bitmap bitmap) {
        List<TextBlock> results = new ArrayList<>();

        try {
            // Detection phase - find text regions
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, DETECT_INPUT_SIZE, DETECT_INPUT_SIZE, true);
            ByteBuffer detectionInput = convertBitmapToByteBuffer(resizedBitmap);

            float[][] detectionOutput = new float[1][DETECT_INPUT_SIZE * DETECT_INPUT_SIZE];
            detectionInterpreter.run(detectionInput, detectionOutput);

            // Post-process detection results to find text regions
            List<Rect> textRegions = processDetectionOutput(detectionOutput[0], bitmap.getWidth(), bitmap.getHeight());

            // Recognition phase - recognize text in each region
            for (Rect region : textRegions) {
                Bitmap textRegionBitmap = Bitmap.createBitmap(bitmap,
                        region.x, region.y, region.width, region.height);

                String recognizedText = recognizeText(textRegionBitmap);

                if (recognizedText != null && !recognizedText.isEmpty()) {
                    results.add(new TextBlock(recognizedText, region));
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in detectText: " + e.getMessage());
        }

        return results;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * DETECT_INPUT_SIZE * DETECT_INPUT_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[DETECT_INPUT_SIZE * DETECT_INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < DETECT_INPUT_SIZE; ++i) {
            for (int j = 0; j < DETECT_INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                // Normalize to [0,1]
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((val & 0xFF) / 255.0f);
            }
        }
        return byteBuffer;
    }

    private List<Rect> processDetectionOutput(float[] output, int originalWidth, int originalHeight) {
        List<Rect> regions = new ArrayList<>();

        // Simple threshold-based region detection
        float threshold = 0.5f;
        int gridSize = DETECT_INPUT_SIZE;

        for (int i = 0; i < output.length; i++) {
            if (output[i] > threshold) {
                int x = (i % gridSize) * originalWidth / gridSize;
                int y = (i / gridSize) * originalHeight / gridSize;
                int width = originalWidth / gridSize;
                int height = originalHeight / gridSize;

                regions.add(new Rect(x, y, width, height));
            }
        }

        // Merge nearby regions (simplified)
        return mergeRegions(regions);
    }

    private List<Rect> mergeRegions(List<Rect> regions) {
        if (regions.size() < 2) return regions;

        List<Rect> merged = new ArrayList<>();
        boolean[] merged_flag = new boolean[regions.size()];

        for (int i = 0; i < regions.size(); i++) {
            if (merged_flag[i]) continue;

            Rect current = regions.get(i);

            for (int j = i + 1; j < regions.size(); j++) {
                if (merged_flag[j]) continue;

                Rect other = regions.get(j);

                // If regions overlap horizontally and are close vertically
                if (Math.abs(current.y - other.y) < 20 &&
                        Math.abs(current.x + current.width - other.x) < 50) {

                    // Merge
                    current.width = other.x + other.width - current.x;
                    current.height = Math.max(current.y + current.height, other.y + other.height) - current.y;
                    merged_flag[j] = true;
                }
            }

            merged.add(current);
            merged_flag[i] = true;
        }

        return merged;
    }

    private String recognizeText(Bitmap textRegion) {
        try {
            // Resize for recognition model
            Bitmap resized = Bitmap.createScaledBitmap(textRegion, REC_INPUT_WIDTH, REC_INPUT_HEIGHT, true);

            // Prepare input
            ByteBuffer input = ByteBuffer.allocateDirect(4 * REC_INPUT_HEIGHT * REC_INPUT_WIDTH * 3);
            input.order(ByteOrder.nativeOrder());

            int[] pixels = new int[REC_INPUT_HEIGHT * REC_INPUT_WIDTH];
            resized.getPixels(pixels, 0, REC_INPUT_WIDTH, 0, 0, REC_INPUT_WIDTH, REC_INPUT_HEIGHT);

            for (int pixel : pixels) {
                input.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
                input.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
                input.putFloat((pixel & 0xFF) / 255.0f);
            }

            // Run inference
            float[][] output = new float[1][80]; // 80 characters (including blank)
            recognitionInterpreter.run(input, output);

            // Decode output to text
            return decodeRecognitionOutput(output[0]);

        } catch (Exception e) {
            Log.e(TAG, "Recognition error: " + e.getMessage());
            return null;
        }
    }

    private String decodeRecognitionOutput(float[] output) {
        // Simplified CTC decoder
        StringBuilder text = new StringBuilder();
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,-:()$€£";

        int lastCharIndex = -1;
        float threshold = 0.5f;

        for (int i = 0; i < output.length; i++) {
            if (output[i] > threshold) {
                if (i != lastCharIndex && i < characters.length()) {
                    text.append(characters.charAt(i));
                    lastCharIndex = i;
                }
            } else {
                lastCharIndex = -1;
            }
        }

        return text.toString();
    }

    public void close() {
        if (detectionInterpreter != null) {
            detectionInterpreter.close();
        }
        if (recognitionInterpreter != null) {
            recognitionInterpreter.close();
        }
    }

    // Helper classes
    public static class Rect {
        public int x, y, width, height;

        public Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public static class TextBlock {
        public String text;
        public Rect rect;

        public TextBlock(String text, Rect rect) {
            this.text = text;
            this.rect = rect;
        }
    }
}
