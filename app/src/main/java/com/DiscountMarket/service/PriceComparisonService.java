package com.DiscountMarket.service;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.DiscountMarket.model.DiscountProduct;
import com.data.ProductItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PriceComparisonService {
    private static final String TAG = "PriceComparison";

    private Context context;
    private FirebaseFirestore db;
    private String userId;
    private ComparisonCallback callback;

    private List<ProductItem> localProducts = new ArrayList<>();
    private List<DiscountProduct> discountedProducts = new ArrayList<>();
    private List<ComparisonResult> comparisonResults = new ArrayList<>();

    public interface ComparisonCallback {
        void onComparisonComplete(List<ComparisonResult> results);
        void onError(String error);
        void onProgress(String message);
    }

    public PriceComparisonService(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    /**
     * 🔍 BÜTÜN MÜQAYİSƏ - ƏSAS METOD
     */
    public void comparePrices(ComparisonCallback callback) {
        this.callback = callback;
        comparisonResults.clear();

        // 1. Əvvəlcə local məhsulları yüklə
        callback.onProgress("📦 Local məhsullarınız yüklənir...");
        loadLocalProducts();
    }

    private void loadLocalProducts() {
        if (userId == null) {
            callback.onError("İstifadəçi tapılmadı");
            return;
        }

        db.collection("products")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    localProducts.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ProductItem product = new ProductItem();
                        product.setId(doc.getId());
                        product.setName(doc.getString("name"));
                        product.setPrice(doc.getDouble("price") != null ? doc.getDouble("price") : 0);
                        product.setCategory(doc.getString("category"));
                        product.setKg(doc.getDouble("kg") != null ? doc.getDouble("kg") : 0);
                        product.setLiter(doc.getDouble("liter") != null ? doc.getDouble("liter") : 0);
                        product.setQuantity(doc.getLong("quantity") != null ? doc.getLong("quantity").intValue() : 0);
                        localProducts.add(product);
                    }

                    Log.d(TAG, "✅ " + localProducts.size() + " local məhsul yükləndi");
                    callback.onProgress("🛒 " + localProducts.size() + " məhsulunuz var, endirimlər yoxlanılır...");

                    // 2. BazarStore endirimli məhsulları yüklə
                    loadDiscountedProducts();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Local məhsullar yüklənərkən xəta", e);
                    callback.onError("Məhsullar yüklənə bilmədi: " + e.getMessage());
                });
    }

    private void loadDiscountedProducts() {
        callback.onProgress("🏪 BazarStore endirimləri yoxlanılır...");

        DiscountScraperService.scrapeStore(0, new DiscountScraperService.StoreScraperCallback() {
            @Override
            public void onSuccess(List<DiscountProduct> products, int totalCount) {
                discountedProducts = products;
                Log.d(TAG, "✅ " + discountedProducts.size() + " endirimli məhsul tapıldı");
                callback.onProgress("🔍 " + discountedProducts.size() + " endirim arasında axtarılır...");

                // 3. Müqayisə et
                performComparison();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Endirimlər yüklənərkən xəta", new Exception(error));
                callback.onError("BazarStore bağlantı xətası: " + error);
            }

            @Override
            public void onProgress(String message) {
                callback.onProgress(message);
            }
        },context);
    }

    private void performComparison() {
        comparisonResults.clear();

        // Hər local məhsul üçün BazarStore-da uyğun endirimi tap
        for (ProductItem localProduct : localProducts) {
            DiscountProduct bestMatch = findBestMatch(localProduct);

            if (bestMatch != null && bestMatch.getDiscountPrice() > 0) {
                double localPrice = localProduct.getPrice();
                double bazarPrice = bestMatch.getDiscountPrice();
                double savings = localPrice - bazarPrice;
                double savingsPercent = localPrice > 0 ? (savings / localPrice) * 100 : 0;

                // YALNIZ BazarStore UCUZDURSA əlavə et (qənaət varsa)
                if (savings > 0.01) {
                    ComparisonResult result = new ComparisonResult();
                    result.setLocalProduct(localProduct);
                    result.setBazarProduct(bestMatch);
                    result.setLocalPrice(localPrice);
                    result.setBazarPrice(bazarPrice);
                    result.setSavings(savings);
                    result.setSavingsPercent(savingsPercent);
                    result.setIsCheaperInBazar(true);

                    comparisonResults.add(result);
                    Log.d(TAG, String.format("🎯 %s: Local: %.2f, BazarStore: %.2f (Qənaət: %.2f AZN)",
                            localProduct.getName(), localPrice, bazarPrice, savings));
                }
            }
        }

        // Nəticələri qənaətə görə sırala (ən böyük qənaət birinci)
        comparisonResults.sort((a, b) -> Double.compare(b.getSavings(), a.getSavings()));

        Log.d(TAG, "✅ Müqayisə tamamlandı: " + comparisonResults.size() + " məhsul BazarStore-da ucuzdur");

        if (callback != null) {
            if (comparisonResults.isEmpty()) {
                callback.onProgress("📭 Heç bir məhsul BazarStore-da ucuz deyil");
            } else {
                double totalSavings = 0;
                for (ComparisonResult r : comparisonResults) {
                    totalSavings += r.getSavings();
                }
                callback.onProgress(String.format("💰 %d məhsul BazarStore-da UCUZ! Ümumi qənaət: %.2f AZN",
                        comparisonResults.size(), totalSavings));
            }
            callback.onComparisonComplete(comparisonResults);
        }
    }

    /**
     * 🎯 Local məhsula ən uyğun BazarStore endirimini tap
     */
    private DiscountProduct findBestMatch(ProductItem localProduct) {
        String localName = localProduct.getName().toLowerCase().trim();

        // 1. Tam uyğunluq (eyni ad)
        for (DiscountProduct dp : discountedProducts) {
            String dpName = dp.getProductName().toLowerCase().trim();
            if (dpName.equals(localName)) {
                return dp;
            }
        }

        // 2. Əsas sözlərə görə axtar (məhsulun əsas adını çıxar)
        String[] keywords = extractKeywords(localName);

        DiscountProduct bestMatch = null;
        int bestScore = 0;

        for (DiscountProduct dp : discountedProducts) {
            String dpName = dp.getProductName().toLowerCase().trim();
            int score = 0;

            for (String keyword : keywords) {
                if (keyword.length() > 2 && dpName.contains(keyword)) {
                    score++;
                }
            }

            // Kateqoriya uyğunluğu
            if (localProduct.getCategory() != null && dp.getCategory() != null) {
                if (localProduct.getCategory().toLowerCase().contains(dp.getCategory().toLowerCase()) ||
                        dp.getCategory().toLowerCase().contains(localProduct.getCategory().toLowerCase())) {
                    score += 2;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = dp;
            }
        }

        // 3. Əgər hələ də tapılmayıbsa, sadə axtarış
        if (bestMatch == null && !discountedProducts.isEmpty()) {
            for (DiscountProduct dp : discountedProducts) {
                String dpName = dp.getProductName().toLowerCase();
                if (localName.contains(dpName) || dpName.contains(localName)) {
                    return dp;
                }
            }
        }

        return bestScore >= 1 ? bestMatch : null;
    }

    private String[] extractKeywords(String productName) {
        // Lazımsız sözləri çıxar
        String[] stopWords = {"təzə", "yeni", "fresh", "new", "premium", "super", "ultra"};
        String cleaned = productName;
        for (String sw : stopWords) {
            cleaned = cleaned.replace(sw, "");
        }

        // Boşluqlara görə ayır
        String[] words = cleaned.split("[\\s,\\-_]+");
        List<String> keywords = new ArrayList<>();
        for (String w : words) {
            if (w.length() > 2 && !isNumber(w)) {
                keywords.add(w);
            }
        }
        return keywords.toArray(new String[0]);
    }

    private boolean isNumber(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ==================== DATA CLASS ====================

    public static class ComparisonResult {
        private ProductItem localProduct;
        private DiscountProduct bazarProduct;
        private double localPrice;
        private double bazarPrice;
        private double savings;
        private double savingsPercent;
        private boolean isCheaperInBazar;

        // Getters
        public ProductItem getLocalProduct() { return localProduct; }
        public void setLocalProduct(ProductItem localProduct) { this.localProduct = localProduct; }

        public DiscountProduct getBazarProduct() { return bazarProduct; }
        public void setBazarProduct(DiscountProduct bazarProduct) { this.bazarProduct = bazarProduct; }

        public double getLocalPrice() { return localPrice; }
        public void setLocalPrice(double localPrice) { this.localPrice = localPrice; }

        public double getBazarPrice() { return bazarPrice; }
        public void setBazarPrice(double bazarPrice) { this.bazarPrice = bazarPrice; }

        public double getSavings() { return savings; }
        public void setSavings(double savings) { this.savings = savings; }

        public double getSavingsPercent() { return savingsPercent; }
        public void setSavingsPercent(double savingsPercent) { this.savingsPercent = savingsPercent; }

        public boolean isCheaperInBazar() { return isCheaperInBazar; }
        public void setIsCheaperInBazar(boolean isCheaperInBazar) { this.isCheaperInBazar = isCheaperInBazar; }

        public String getFormattedSavings() {
            return String.format(Locale.getDefault(), "%.2f AZN (%.0f%%)", savings, savingsPercent);
        }

        public String getComparisonText() {
            if (isCheaperInBazar) {
                return String.format("🛒 BazarStore-da %.0f%% UCUZ!", savingsPercent);
            } else {
                return "⚠️ Yoxlanılmadı";
            }
        }
    }
}
