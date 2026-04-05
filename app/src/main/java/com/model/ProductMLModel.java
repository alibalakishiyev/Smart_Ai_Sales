package com.model;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

public class ProductMLModel {
    private static final String TAG = "ProductMLModel";
    private static final String MODEL_FILE = "DLModel/cibim_model.tflite";
    private static final String SCALER_CONFIG = "DLModel/scaler_config.json";

    private Interpreter tflite;
    private boolean isInitialized = false;
    private Context context;

    // MinMaxScaler üçün dəyərlər (Sizin JSON formatınıza uyğun)
    private float[] minVals;
    private float[] maxVals;
    private float[] scaleVals;
    private String scalerType;
    private String[] featureNames;

    private static final float SCORE_THRESHOLD = 0.45f;

    public ProductMLModel(Context context) {
        this.context = context;

        try {
            // 1. Load TFLite model
            tflite = new Interpreter(loadModelFile(context));

            // 2. Load scaler configuration (sizin JSON formatınıza uyğun)
            loadScalerConfig();

            isInitialized = true;
            Log.d(TAG, "✅ REAL Deep Learning model loaded successfully!");
            Log.d(TAG, "✅ Scaler type: " + scalerType);
            if (minVals != null) {
                Log.d(TAG, "✅ Min values: " + Arrays.toString(minVals));
                Log.d(TAG, "✅ Max values: " + Arrays.toString(maxVals));
            }

            // Test model
            testModel();

        } catch (IOException e) {
            Log.e(TAG, "❌ Failed to load model", e);
            isInitialized = false;
        } catch (Exception e) {
            Log.e(TAG, "❌ Initialization error", e);
            isInitialized = false;
        }
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void loadScalerConfig() throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(SCALER_CONFIG))
        );
        StringBuilder jsonString = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonString.append(line);
        }
        reader.close();

        JSONObject config = new JSONObject(jsonString.toString());

        // Sizin JSON formatınıza uyğun oxu
        scalerType = config.optString("scaler_type", "minmax");

        // min_vals, max_vals, scale_vals oxu
        org.json.JSONArray minArray = config.getJSONArray("min_vals");
        org.json.JSONArray maxArray = config.getJSONArray("max_vals");
        org.json.JSONArray scaleArray = config.getJSONArray("scale_vals");
        org.json.JSONArray featuresArray = config.getJSONArray("feature_names");

        minVals = new float[minArray.length()];
        maxVals = new float[maxArray.length()];
        scaleVals = new float[scaleArray.length()];
        featureNames = new String[featuresArray.length()];

        for (int i = 0; i < minArray.length(); i++) {
            minVals[i] = (float) minArray.getDouble(i);
            maxVals[i] = (float) maxArray.getDouble(i);
            scaleVals[i] = (float) scaleArray.getDouble(i);
            featureNames[i] = featuresArray.getString(i);
        }

        Log.d(TAG, "✅ Scaler config loaded. Features: " + featureNames.length);
        Log.d(TAG, "✅ Scaler type: " + scalerType);
    }

    /**
     * MinMaxScaler normalization (Sizin Python modelinizə uyğun)
     * Formula: (x - min) / (max - min)
     */
    private float[] normalize(float[] features) {
        float[] normalized = new float[features.length];
        for (int i = 0; i < features.length; i++) {
            float range = maxVals[i] - minVals[i];
            if (range != 0) {
                normalized[i] = (features[i] - minVals[i]) / range;
            } else {
                normalized[i] = 0.5f;
            }
            // Clip to [0,1] range
            normalized[i] = Math.max(0.0f, Math.min(1.0f, normalized[i]));
        }
        return normalized;
    }

    /**
     * REAL prediction using actual trained model
     */
    public float predict(Product product) {
        if (!isInitialized || tflite == null) {
            Log.w(TAG, "Model not initialized, using fallback");
            return fallbackPrediction(product);
        }

        try {
            // Extract features in the correct order
            float[] features = extractFeatures(product);

            // Log raw features
            Log.d(TAG, "Raw features: " + formatFeatures(features));

            // Normalize features using MinMaxScaler
            float[] normalized = normalize(features);
            Log.d(TAG, "Normalized features: " + formatFeatures(normalized));

            // Run inference
            float[][] input = new float[1][features.length];
            input[0] = normalized;

            float[][] output = new float[1][1];
            tflite.run(input, output);

            float rawScore = output[0][0];

            // Apply sigmoid for probability
            float probability = (float) (1.0 / (1.0 + Math.exp(-rawScore)));

            Log.d(TAG, String.format("🎯 Deep Learning Prediction: raw=%.4f, prob=%.3f (is_expensive: %s)",
                    rawScore, probability, probability >= SCORE_THRESHOLD ? "YES" : "NO"));

            return probability;

        } catch (Exception e) {
            Log.e(TAG, "Prediction failed", e);
            return fallbackPrediction(product);
        }
    }

    private String formatFeatures(float[] features) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < features.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format(Locale.US, "%.4f", features[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Extract features - MUST match Python training order
     * Order: ["price", "in_stock", "is_low_stock", "title_word_cnt",
     *          "price_rank_brand", "price_rank_global", "price_vs_brand_mean",
     *          "volume_std", "price_per_unit"]
     */
    private float[] extractFeatures(Product product) {
        float[] features = new float[9];

        features[0] = (float) product.price;                           // price
        features[1] = product.inStock ? 1.0f : 0.0f;                  // in_stock
        features[2] = product.isLowStock ? 1.0f : 0.0f;               // is_low_stock
        features[3] = product.titleWordCount;                          // title_word_cnt
        features[4] = product.priceRankBrand;                          // price_rank_brand
        features[5] = product.priceRankGlobal;                         // price_rank_global
        features[6] = product.priceVsBrandMean;                        // price_vs_brand_mean
        features[7] = product.volumeStd;                               // volume_std
        features[8] = (float) product.pricePerUnit;                    // price_per_unit

        return features;
    }

    private float fallbackPrediction(Product product) {
        float score = 0.3f;
        if (product.price > 50) score += 0.2f;
        if (product.pricePerUnit > 10) score += 0.2f;
        if (!product.inStock) score -= 0.1f;
        if (product.priceRankBrand > 0.7f) score += 0.15f;
        return Math.min(0.95f, Math.max(0.05f, score));
    }

    public List<ProductPrediction> predictBatch(List<Product> products) {
        List<ProductPrediction> results = new ArrayList<>();

        if (!isInitialized || tflite == null) {
            for (Product p : products) {
                results.add(new ProductPrediction(p, fallbackPrediction(p)));
            }
            return results;
        }

        for (Product product : products) {
            float score = predict(product);
            results.add(new ProductPrediction(product, score));
        }

        return results;
    }

    public List<ProductPrediction> getTopExpensive(List<Product> products, int topN) {
        List<ProductPrediction> predictions = predictBatch(products);
        predictions.sort((a, b) -> Float.compare(b.score, a.score));
        return predictions.subList(0, Math.min(topN, predictions.size()));
    }

    public Map<String, BrandStats> getBrandAverages(List<Product> products) {
        Map<String, BrandStats> stats = new HashMap<>();

        for (Product product : products) {
            float score = predict(product);
            BrandStats brandStat = stats.getOrDefault(product.brand, new BrandStats(product.brand));
            brandStat.addScore(score);
            stats.put(product.brand, brandStat);
        }

        return stats;
    }

    public boolean testModel() {
        if (!isInitialized) return false;

        // Create a test product
        Product testProduct = new Product();
        testProduct.price = 99.99;
        testProduct.inStock = true;
        testProduct.isLowStock = false;
        testProduct.titleWordCount = 5;
        testProduct.priceRankBrand = 0.75f;
        testProduct.priceRankGlobal = 0.60f;
        testProduct.priceVsBrandMean = 0.15f;
        testProduct.volumeStd = 8.5f;
        testProduct.pricePerUnit = 12.99;

        float result = predict(testProduct);
        Log.d(TAG, "🧪 DEEP LEARNING TEST RESULT: " + String.format(Locale.US, "%.3f", result));
        return result > 0;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            Log.d(TAG, "Model closed");
        }
    }

    // ==================== DATA CLASSES ====================

    public static class Product {
        public String id;
        public String title;
        public String brand;
        public String categoryType;
        public double price;
        public double pricePerUnit;
        public boolean inStock;
        public boolean isLowStock;
        public int titleWordCount;
        public float priceRankBrand;
        public float priceRankGlobal;
        public float priceVsBrandMean;
        public float volumeStd;

        public Product() {}
    }

    public static class ProductPrediction {
        public Product product;
        public float score;
        public boolean isExpensive;
        public long timestamp;

        public ProductPrediction(Product product, float score) {
            this.product = product;
            this.score = score;
            this.isExpensive = score >= SCORE_THRESHOLD;
            this.timestamp = System.currentTimeMillis();
        }

        public String getFormattedScore() {
            return String.format(Locale.US, "%.1f%%", score * 100);
        }
    }

    public static class BrandStats {
        public String brand;
        public float totalScore;
        public int count;
        public float minScore;
        public float maxScore;

        public BrandStats(String brand) {
            this.brand = brand;
            this.totalScore = 0;
            this.count = 0;
            this.minScore = 1.0f;
            this.maxScore = 0.0f;
        }

        public void addScore(float score) {
            totalScore += score;
            count++;
            if (score < minScore) minScore = score;
            if (score > maxScore) maxScore = score;
        }

        public float getAverage() {
            return count > 0 ? totalScore / count : 0;
        }
    }
}