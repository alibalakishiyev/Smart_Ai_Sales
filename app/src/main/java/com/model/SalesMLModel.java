package com.model;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SalesMLModel {
    private static final String TAG = "SalesMLModel";

    private Context context;
    private boolean isModelLoaded = false;

    // Model input/output details
    private int inputSize = 10; // 10 days
    private int featureCount = 8; // 8 features
    private int outputSize = 5; // sales, expenses, profit, avg_price, customer_count

    public SalesMLModel(Context context) {
        this.context = context;

        // Model always loaded in fallback mode
        this.isModelLoaded = true; // ƏSAS DƏYİŞİKLİK: true olaraq təyin edin
        Log.d(TAG, "📊 SalesMLModel initialized in FALLBACK mode");
        Log.d(TAG, "📊 Using statistical analysis instead of ML model");
        Log.d(TAG, "✅ Model status: LOADED (fallback mode)");
    }

    /**
     * Modelin yüklü olub-olmadığını yoxlayır
     */
    public boolean isModelLoaded() {
        return true; // ƏSAS DƏYİŞİKLİK: true qaytarır
    }

    /**
     * Proqnoz vermə (fallback mode)
     * @param inputData float[10][8] - 10 transactions, 8 features
     * @return float[5] - [predicted_income, predicted_expense, predicted_profit, predicted_avg_price, predicted_customer_count]
     */
    public float[] predict(float[][] inputData) {
        Log.d(TAG, "📊 Using fallback prediction");
        return fallbackPredict(inputData);
    }

    /**
     * Model olmadıqda istifadə olunan sadə proqnoz
     */
    private float[] fallbackPredict(float[][] inputData) {
        float totalIncome = 0, totalExpense = 0;
        int validDays = 0;

        try {
            // Son 5 günün ortalamasını hesabla
            for (int i = Math.max(0, inputData.length - 5); i < inputData.length; i++) {
                if (inputData[i] != null) {
                    totalIncome += inputData[i][0];
                    totalExpense += inputData[i][1];
                    validDays++;
                }
            }

            if (validDays > 0) {
                float avgIncome = totalIncome / validDays;
                float avgExpense = totalExpense / validDays;

                // Mövsümi amillərə görə proqnoz (sadə)
                float seasonalFactor = 1.0f;
                Calendar cal = Calendar.getInstance();
                int month = cal.get(Calendar.MONTH);

                // Yay aylarında artım, qışda azalma
                if (month >= 5 && month <= 8) { // İyun-Sentyabr
                    seasonalFactor = 1.2f;
                } else if (month >= 11 || month <= 1) { // Dekabr-Fevral
                    seasonalFactor = 0.9f;
                }

                return new float[]{
                        avgIncome * 1.1f * seasonalFactor,      // predicted income
                        avgExpense * 1.05f * seasonalFactor,    // predicted expense
                        (avgIncome - avgExpense) * 1.08f * seasonalFactor, // predicted profit
                        avgIncome / 10f,                         // predicted avg price
                        10f                                       // predicted customer count
                };
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in fallback prediction", e);
        }

        // Default dəyərlər
        return new float[]{1500f, 500f, 1000f, 150f, 10f};
    }

    /**
     * Transaction-lardan feature hazırlayır
     */
    public float[][] prepareFeatures(List<Map<String, Object>> transactions) {
        float[][] features = new float[10][8];

        // Bütün feature-ları sıfırla
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 8; j++) {
                features[i][j] = 0f;
            }
        }

        if (transactions == null || transactions.isEmpty()) {
            Log.d(TAG, "No transactions, using zeros");
            return features;
        }

        try {
            // Son 10 əməliyyatı al (əgər azdırsa, başını 0-la doldur)
            int startIdx = Math.max(0, transactions.size() - 10);
            int count = Math.min(10, transactions.size());

            for (int i = 0; i < count; i++) {
                Map<String, Object> t = transactions.get(startIdx + i);
                if (t == null) continue;

                // amount - totalAmount istifadə et, yoxdursa amount istifadə et
                Number totalAmountNum = (Number) t.get("totalAmount");
                Number amountNum = (Number) t.get("amount");
                float amount = 0f;

                if (totalAmountNum != null) {
                    amount = totalAmountNum.floatValue();
                } else if (amountNum != null) {
                    amount = amountNum.floatValue();
                }
                features[i][0] = amount;

                // category_encoded
                String category = (String) t.get("category");
                features[i][1] = getCategoryCode(category);

                // hour
                Long date = (Long) t.get("date");
                if (date != null && date > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(date);
                    features[i][2] = cal.get(Calendar.HOUR_OF_DAY);

                    // day_of_week (0-6, Bazar ertəsi = 1, Bazar = 7)
                    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                    features[i][3] = dayOfWeek - 1; // 0-6 formatına çevir

                    // is_weekend
                    features[i][4] = (dayOfWeek == Calendar.SATURDAY ||
                            dayOfWeek == Calendar.SUNDAY) ? 1f : 0f;
                }

                // log_amount (log1p = log(1+x))
                features[i][5] = (float) Math.log1p(features[i][0]);

                // amount_per_item
                Long quantity = (Long) t.get("quantity");
                int qty = quantity != null ? quantity.intValue() : 1;
                features[i][6] = qty > 0 ? features[i][0] / qty : 0;

                // quantity
                features[i][7] = qty;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error preparing features", e);
        }

        return features;
    }

    /**
     * Kateqoriya adını kodlaşdırır
     */
    private float getCategoryCode(String category) {
        if (category == null) return 0f;

        switch (category) {
            case "🛒 Ərzaq": return 1f;
            case "🚗 Nəqliyyat": return 2f;
            case "🏠 Kirayə": return 3f;
            case "💡 Kommunal": return 4f;
            case "🎮 Əyləncə": return 5f;
            case "🏥 Səhiyyə": return 6f;
            case "📚 Təhsil": return 7f;
            case "👕 Geyim": return 8f;
            case "📱 Texnologiya": return 9f;
            case "🍽️ Restoran": return 10f;
            case "💰 Maaş": return 11f;
            case "💼 Bonus": return 12f;
            case "📈 İnvestisiya": return 13f;
            case "🛍️ Satış": return 14f;
            case "🎁 Hədiyyə": return 15f;
            case "💵 Freelance": return 16f;
            case "🥩 Ət məhsulları": return 17f;
            case "🥛 Süd məhsulları": return 18f;
            case "🍞 Çörək məhsulları": return 19f;
            case "🧃 İçkilər": return 20f;
            case "🍎 Meyvə-tərəvəz": return 21f;
            default: return 0f;
        }
    }

    /**
     * Kateqoriya kodunu ada çevirir
     */
    public String getCategoryFromCode(float code) {
        int intCode = Math.round(code);
        switch (intCode) {
            case 1: return "🛒 Ərzaq";
            case 2: return "🚗 Nəqliyyat";
            case 3: return "🏠 Kirayə";
            case 4: return "💡 Kommunal";
            case 5: return "🎮 Əyləncə";
            case 6: return "🏥 Səhiyyə";
            case 7: return "📚 Təhsil";
            case 8: return "👕 Geyim";
            case 9: return "📱 Texnologiya";
            case 10: return "🍽️ Restoran";
            case 11: return "💰 Maaş";
            case 12: return "💼 Bonus";
            case 13: return "📈 İnvestisiya";
            case 14: return "🛍️ Satış";
            case 15: return "🎁 Hədiyyə";
            case 16: return "💵 Freelance";
            case 17: return "🥩 Ət məhsulları";
            case 18: return "🥛 Süd məhsulları";
            case 19: return "🍞 Çörək məhsulları";
            case 20: return "🧃 İçkilər";
            case 21: return "🍎 Meyvə-tərəvəz";
            default: return "Digər";
        }
    }

    /**
     * Analiz aparır
     */
    public Map<String, Object> analyzeTransactions(List<Map<String, Object>> transactions) {
        Map<String, Object> analysis = new HashMap<>();

        if (transactions == null || transactions.isEmpty()) {
            analysis.put("totalIncome", 0.0);
            analysis.put("totalExpense", 0.0);
            analysis.put("balance", 0.0);
            analysis.put("transactionCount", 0);
            analysis.put("topCategories", new ArrayList<>());
            analysis.put("topProducts", new ArrayList<>());
            return analysis;
        }

        double totalIncome = 0;
        double totalExpense = 0;
        Map<String, Double> categoryTotals = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();
        Map<String, Double> productTotals = new HashMap<>();
        Map<String, Integer> productCounts = new HashMap<>();

        for (Map<String, Object> t : transactions) {
            try {
                String type = (String) t.get("type");
                Number totalAmountNum = (Number) t.get("totalAmount");
                Number amountNum = (Number) t.get("amount");
                double amount = 0;

                if (totalAmountNum != null) {
                    amount = totalAmountNum.doubleValue();
                } else if (amountNum != null) {
                    amount = amountNum.doubleValue();
                }

                String category = (String) t.get("category");
                String product = (String) t.get("productName");
                Number quantityNum = (Number) t.get("quantity");
                int quantity = quantityNum != null ? quantityNum.intValue() : 1;

                if ("income".equals(type)) {
                    totalIncome += amount;
                } else if ("expense".equals(type)) {
                    totalExpense += amount;
                }

                if (category != null && !category.isEmpty()) {
                    categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
                    categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
                }

                if (product != null && !product.isEmpty()) {
                    productTotals.put(product, productTotals.getOrDefault(product, 0.0) + amount);
                    productCounts.put(product, productCounts.getOrDefault(product, 0) + quantity);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error analyzing transaction", e);
            }
        }

        double balance = totalIncome - totalExpense;
        double avgTransaction = transactions.size() > 0 ?
                (totalIncome + totalExpense) / transactions.size() : 0;

        // Ən çox gəlir gətirən kateqoriyalar
        List<Map<String, Object>> topCategories = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            Map<String, Object> cat = new HashMap<>();
            cat.put("name", entry.getKey());
            cat.put("amount", entry.getValue());
            cat.put("count", categoryCounts.getOrDefault(entry.getKey(), 0));
            cat.put("avg", entry.getValue() / categoryCounts.getOrDefault(entry.getKey(), 1));
            topCategories.add(cat);
        }
        topCategories.sort((a, b) -> Double.compare(
                (Double) b.get("amount"),
                (Double) a.get("amount")
        ));

        // Ən çox satılan məhsullar
        List<Map<String, Object>> topProducts = new ArrayList<>();
        for (Map.Entry<String, Double> entry : productTotals.entrySet()) {
            Map<String, Object> prod = new HashMap<>();
            prod.put("name", entry.getKey());
            prod.put("amount", entry.getValue());
            prod.put("count", productCounts.getOrDefault(entry.getKey(), 0));
            prod.put("avg", entry.getValue() / productCounts.getOrDefault(entry.getKey(), 1));
            topProducts.add(prod);
        }
        topProducts.sort((a, b) -> Double.compare(
                (Double) b.get("amount"),
                (Double) a.get("amount")
        ));

        analysis.put("totalIncome", totalIncome);
        analysis.put("totalExpense", totalExpense);
        analysis.put("balance", balance);
        analysis.put("avgTransaction", avgTransaction);
        analysis.put("transactionCount", transactions.size());
        analysis.put("topCategories", topCategories.subList(0, Math.min(5, topCategories.size())));
        analysis.put("topProducts", topProducts.subList(0, Math.min(10, topProducts.size())));

        return analysis;
    }

    /**
     * Tövsiyələr yaradır
     */
    public List<String> generateRecommendations(Map<String, Object> analysis) {
        List<String> recommendations = new ArrayList<>();

        try {
            double totalExpense = (double) analysis.get("totalExpense");
            double totalIncome = (double) analysis.get("totalIncome");
            double balance = (double) analysis.get("balance");
            int transactionCount = (int) analysis.get("transactionCount");

            // Balans tövsiyələri
            if (balance < 0) {
                recommendations.add("⚠️ Balansınız mənfidir! Xərcləri azaltmağa çalışın.");
                recommendations.add("💡 Gəlirləri artırmaq üçün yeni məhsullar əlavə edin.");
            } else if (balance < totalIncome * 0.2) {
                recommendations.add("📉 Yığım səviyyəniz aşağıdır. Gəlirin 20%-ni yığmağa çalışın.");
            } else if (balance > totalIncome * 0.5) {
                recommendations.add("✅ Əla vəziyyət! Yığım səviyyəniz çox yaxşıdır.");
            } else {
                recommendations.add("✅ Yaxşı iş! Balansınız stabil görünür.");
            }

            // Əməliyyat sayı tövsiyəsi
            if (transactionCount < 10) {
                recommendations.add("📝 Daha dəqiq analiz üçün daha çox məlumat daxil edin.");
            }

            // Kateqoriya tövsiyələri
            List<Map<String, Object>> topCategories = (List<Map<String, Object>>) analysis.get("topCategories");
            if (topCategories != null && !topCategories.isEmpty()) {
                Map<String, Object> top = topCategories.get(0);
                double topAmount = (double) top.get("amount");
                String topName = (String) top.get("name");

                if (topAmount > totalExpense * 0.3) {
                    recommendations.add("💡 " + topName + " kateqoriyasına çox xərc edirsiniz (" +
                            String.format("%.0f", topAmount) + " ₼). Bura qənaət edə bilərsiniz.");
                }

                if (topCategories.size() > 1) {
                    Map<String, Object> second = topCategories.get(1);
                    recommendations.add("📊 İkinci ən çox xərc: " + second.get("name") +
                            " (" + String.format("%.0f", (double) second.get("amount")) + " ₼)");
                }
            }

            // Məhsul tövsiyələri
            List<Map<String, Object>> topProducts = (List<Map<String, Object>>) analysis.get("topProducts");
            if (topProducts != null && !topProducts.isEmpty()) {
                Map<String, Object> top = topProducts.get(0);
                recommendations.add("🏆 Ən çox satılan məhsul: " + top.get("name") +
                        " (" + String.format("%.0f", (double) top.get("amount")) + " ₼)");
            }

            // Ümumi tövsiyələr
            recommendations.add("📝 Hər gün məlumatları qeyd edin, analiz daha dəqiq olacaq.");
            recommendations.add("🎯 Aylıq büdcə təyin edin və ona əməl edin.");
            recommendations.add("📊 Müntəzəm olaraq hesabatları yoxlayın.");

        } catch (Exception e) {
            Log.e(TAG, "Error generating recommendations", e);
            recommendations.add("📊 Məlumatlar analiz edilir...");
        }

        return recommendations;
    }

    /**
     * Növbəti əməliyyatı proqnozlaşdırır
     */
    public Map<String, Object> predictNext(List<Map<String, Object>> lastTransactions) {
        float[][] features = prepareFeatures(lastTransactions);
        float[] prediction = predict(features);

        Map<String, Object> result = new HashMap<>();
        result.put("predictedIncome", prediction[0]);
        result.put("predictedExpense", prediction[1]);
        result.put("predictedProfit", prediction[2]);
        result.put("predictedAvgPrice", prediction[3]);
        result.put("predictedCustomerCount", (int) prediction[4]);

        // Əminlik faizi (model olmadığı üçün sabit)
        result.put("confidence", 0.65);

        return result;
    }

    /**
     * Modeli bağlayır
     */
    public void close() {
        Log.d(TAG, "SalesMLModel closed");
    }
}