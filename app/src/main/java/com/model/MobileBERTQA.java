package com.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MobileBERTQA {

    private static final String TAG = "MobileBERTQA";
    private Context context;
    private Random random;

    // Maliyyə məlumatları (cache)
    private double cachedTotalIncome = 0;
    private double cachedTotalExpense = 0;
    private double cachedMonthlySalary = 0;
    private double cachedSavings = 0;

    // Gündəlik xərclər üçün tarixçə
    private double[] dailyExpenses = new double[30];
    private long lastUpdateTime = 0;

    // ✅ YENİ: Məhsul analizi məlumatları
    private Map<String, Integer> productFrequency = new HashMap<>();
    private Map<String, Double> productSpending = new HashMap<>();
    private Map<String, Double> categoryTotals = new HashMap<>();
    private Map<String, Integer> categoryCount = new HashMap<>();
    private Map<String, Integer> storeFrequency = new HashMap<>();
    private List<ProductPurchase> productPurchases = new ArrayList<>();

    // Məhsul alış məlumatları üçün class
    private static class ProductPurchase {
        String productName;
        String category;
        String storeName;
        double price;
        double total;
        int quantity;
        long date;

        ProductPurchase(String productName, String category, String storeName,
                        double price, double total, int quantity, long date) {
            this.productName = productName;
            this.category = category;
            this.storeName = storeName;
            this.price = price;
            this.total = total;
            this.quantity = quantity;
            this.date = date;
        }
    }

    public MobileBERTQA(Context context) {
        this.context = context;
        this.random = new Random();
        loadCachedData();
        loadDailyExpenses();
        loadProductAnalyticsData();
        Log.d(TAG, "✅ MobileBERTQA hazırdır! Proqnoz və məhsul analizi sistemi aktiv.");
    }

    private void loadCachedData() {
        SharedPreferences prefs = context.getSharedPreferences("finance_data", Context.MODE_PRIVATE);
        cachedTotalIncome = prefs.getFloat("total_income", 0);
        cachedTotalExpense = prefs.getFloat("total_expense", 0);
        cachedMonthlySalary = prefs.getFloat("monthly_salary", 0);
        cachedSavings = cachedTotalIncome - cachedTotalExpense;
        Log.d(TAG, "📊 Yüklənmiş məlumatlar: Gəlir=" + cachedTotalIncome + ", Xərc=" + cachedTotalExpense);
    }

    private void loadDailyExpenses() {
        SharedPreferences prefs = context.getSharedPreferences("daily_expenses", Context.MODE_PRIVATE);
        for (int i = 0; i < 30; i++) {
            dailyExpenses[i] = prefs.getFloat("day_" + i, 0);
        }
        lastUpdateTime = prefs.getLong("last_update", 0);
    }

    private void saveDailyExpenses() {
        SharedPreferences prefs = context.getSharedPreferences("daily_expenses", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < 30; i++) {
            editor.putFloat("day_" + i, (float) dailyExpenses[i]);
        }
        editor.putLong("last_update", System.currentTimeMillis());
        editor.apply();
    }

    // ✅ YENİ: Məhsul analizi məlumatlarını yüklə
    private void loadProductAnalyticsData() {
        SharedPreferences prefs = context.getSharedPreferences("product_analytics", Context.MODE_PRIVATE);
        // Məlumatlar SharedPreferences-dən yüklənir
    }



    public void updateFinancialData(double totalIncome, double totalExpense, double monthlySalary) {
        this.cachedTotalIncome = totalIncome;
        this.cachedTotalExpense = totalExpense;
        this.cachedMonthlySalary = monthlySalary;
        this.cachedSavings = totalIncome - totalExpense;

        SharedPreferences prefs = context.getSharedPreferences("finance_data", Context.MODE_PRIVATE);
        prefs.edit()
                .putFloat("total_income", (float) totalIncome)
                .putFloat("total_expense", (float) totalExpense)
                .putFloat("monthly_salary", (float) monthlySalary)
                .apply();

        Log.d(TAG, "📊 Maliyyə məlumatları yeniləndi: Gəlir=" + totalIncome + ", Xərc=" + totalExpense);
    }

    public void addDailyExpense(double amount) {
        for (int i = 28; i >= 0; i--) {
            dailyExpenses[i + 1] = dailyExpenses[i];
        }
        dailyExpenses[0] = amount;
        saveDailyExpenses();
        Log.d(TAG, "📝 Gündəlik xərc əlavə edildi: " + amount);
    }

    public String answerQuestion(String question, String contextText) {
        Log.d(TAG, "🔍 Sual: " + question);

        if (question == null || question.trim().isEmpty()) {
            return getRandomGreeting();
        }

        String q = question.toLowerCase().trim();

        // 1. SALAMLAŞMA
        if (containsAny(q, "salam", "hello", "hi", "hey", "merhaba", "привет")) {
            return getRandomGreeting();
        }

        // 2. KÖMƏK
        if (containsAny(q, "kömək", "help", "yardım", "nece", "how to", "помощь")) {
            return getHelpMessage();
        }

        // ✅ YENİ: ƏN ÇOX ALINAN MƏHSUL
        if (containsAny(q, "ən çox", "coxsatilan", "top product", "most bought", "ən çox aldığım", "tez-tez alıram")) {
            return getTopProductsAnswer();
        }

        // ✅ YENİ: KATEQORİYA ANALİZİ
        if (containsAny(q, "kateqoriya", "category", "hansı kateqoriya", "ən çox xərc kateqoriyası")) {
            return getTopCategoriesAnswer();
        }

        // ✅ YENİ: MAĞAZA ANALİZİ
        if (containsAny(q, "mağaza", "store", "haradan", "where do i shop", "ən çox haradan")) {
            return getStoreAnswer();
        }

        // ✅ YENİ: MƏHSUL XƏRCLƏRİ
        if (containsAny(q, "məhsul xərcləri", "product spending", "hansı məhsula çox pul verirəm")) {
            return getProductSpendingAnswer();
        }

        // ✅ YENİ: XƏRCLƏRİN TƏHLİLİ (ümumi)
        if (containsAny(q, "xərclərimin təhlili", "spending analysis", "xərclərimi təhlil et")) {
            return getFullSpendingAnalysis();
        }

        // 3. XƏRC SUALLARI
        if (containsAny(q, "xərc", "expense", "spend", "spent", "harcama", "потратил", "расход")) {
            return getExpenseAnswer(q);
        }

        // 4. GƏLİR SUALLARI
        if (containsAny(q, "gəlir", "income", "earn", "maaş", "salary", "доход", "зарплата")) {
            return getIncomeAnswer(q);
        }

        // 5. QƏNAƏT SUALLARI
        if (containsAny(q, "qənaət", "saving", "save", "economy", "экономия")) {
            return getSavingsAnswer(q);
        }

        // 6. BALANS
        if (containsAny(q, "balans", "balance", "qalıq", "remain", "баланс", "остаток")) {
            return getBalanceAnswer();
        }

        // 7. MƏSLƏHƏT
        if (containsAny(q, "məsləhət", "advice", "tövsiyə", "recommend", "совет")) {
            return getAdvice();
        }

        // 8. PROQNOZ
        if (containsAny(q, "proqnoz", "forecast", "predict", "olacaq", "прогноз")) {
            return getAccurateForecast();
        }

        // 9. TƏŞƏKKÜR
        if (containsAny(q, "təşəkkür", "thank", "sağol", "thanks", "спасибо")) {
            return getThankYouMessage();
        }

        // 10. ANALİZ
        if (containsAny(q, "analiz", "analysis", "təhlil", "analiza", "анализ")) {
            return getAnalysisAnswer();
        }

        // 11. MÜQAYİSƏ
        if (containsAny(q, "müqayisə", "compare", "comparison", "arasında", "сравнение")) {
            return getComparisonAnswer();
        }

        // 12. DEFAULT CAVAB
        return getDefaultAnswer();
    }

    // ==================== YENİ MƏHSUL ANALİZİ METODLARI ====================

    private String getTopProductsAnswer() {
        Log.d(TAG, "📊 getTopProductsAnswer çağırıldı, productFrequency ölçüsü: " + productFrequency.size());

        if (productFrequency.isEmpty()) {
            // SharedPreferences-dən yenidən yükləməyə çalış
            loadProductAnalyticsFromCache();

            if (productFrequency.isEmpty()) {
                return "📭 Hələ kifayət qədər məhsul məlumatı yoxdur. Əməliyyatlar əlavə edin və mən sizə ən çox aldığınız məhsulları göstərim.\n\n💡 İpucu: Əməliyyat əlavə edərkən 'Məhsul adı' sahəsini doldurun!";
            }
        }

        // Məhsulları tezliyə görə sırala
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(productFrequency.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        StringBuilder sb = new StringBuilder();
        sb.append("🛒 ƏN ÇOX ALDIĞINIZ MƏHSULLAR:\n");
        sb.append("═══════════════════════════════\n\n");

        int count = 0;
        for (Map.Entry<String, Integer> entry : sorted) {
            if (count++ >= 5) break;
            double totalSpent = productSpending.getOrDefault(entry.getKey(), 0.0);
            double avgPrice = entry.getValue() > 0 ? totalSpent / entry.getValue() : 0;

            sb.append(String.format(Locale.getDefault(),
                    "%d. %s\n", count, entry.getKey()));
            sb.append(String.format(Locale.getDefault(),
                    "   • Alış sayı: %d dəfə\n", entry.getValue()));
            sb.append(String.format(Locale.getDefault(),
                    "   • Ümumi xərc: ₼%.2f\n", totalSpent));
            sb.append(String.format(Locale.getDefault(),
                    "   • Orta qiymət: ₼%.2f\n\n", avgPrice));
        }

        // Tövsiyə əlavə et
        if (!sorted.isEmpty()) {
            String topProduct = sorted.get(0).getKey();
            int freq = sorted.get(0).getValue();
            sb.append("💡 TÖVSİYƏ: ");
            if (freq > 10) {
                sb.append(String.format("\"%s\" məhsulunu çox tez-tez alırsınız (%d dəfə). Toplu alış və ya endirimləri izləyin!", topProduct, freq));
            } else {
                sb.append(String.format("\"%s\" məhsulu ən çox seçiminizdir. Bu məhsulda qənaət etmək üçün alternativ mağazaları araşdırın.", topProduct));
            }
        }

        return sb.toString();
    }




    private String getTopCategoriesAnswer() {
        if (categoryTotals.isEmpty()) {
            return "📭 Kateqoriya məlumatları hələ kifayət deyil. Əməliyyatlar əlavə edin.";
        }

        // Kateqoriyaları xərcə görə sırala
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(categoryTotals.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        double totalSpending = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("📁 ƏN ÇOX XƏRC ETDİYİNİZ KATEQORİYALAR:\n");
        sb.append("═══════════════════════════════════\n\n");

        int count = 0;
        for (Map.Entry<String, Double> entry : sorted) {
            if (count++ >= 5) break;
            double percent = totalSpending > 0 ? (entry.getValue() / totalSpending) * 100 : 0;
            int purchaseCount = categoryCount.getOrDefault(entry.getKey(), 0);

            sb.append(String.format(Locale.getDefault(),
                    "%d. %s\n", count, entry.getKey()));
            sb.append(String.format(Locale.getDefault(),
                    "   • Ümumi xərc: ₼%.2f (%.0f%%)\n", entry.getValue(), percent));
            sb.append(String.format(Locale.getDefault(),
                    "   • Alış sayı: %d dəfə\n\n", purchaseCount));
        }

        // Tövsiyə əlavə et
        if (!sorted.isEmpty()) {
            String topCategory = sorted.get(0).getKey();
            double topPercent = (sorted.get(0).getValue() / totalSpending) * 100;

            sb.append("💡 TÖVSİYƏ: ");
            if (topPercent > 40) {
                sb.append(String.format("Xərclərinizin %.0f%%-i \"%s\" kateqoriyasına aiddir. Bu sahədə qənaət etmək üçün büdcə planlaması edin!", topPercent, topCategory));
            } else if (topPercent > 20) {
                sb.append(String.format("\"%s\" kateqoriyası əsas xərclərinizdəndir. Endirimləri izləməyə davam edin.", topCategory));
            } else {
                sb.append(String.format("Xərcləriniz müxtəlif kateqoriyalara yayılmışdır. Bu yaxşı balansdır!", topCategory));
            }
        }

        return sb.toString();
    }

    private String getStoreAnswer() {
        if (storeFrequency.isEmpty()) {
            return "🏪 Mağaza məlumatları hələ kifayət deyil. Əməliyyatlar əlavə edin.";
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(storeFrequency.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        StringBuilder sb = new StringBuilder();
        sb.append("🏪 ƏN ÇOX ALIŞ-VERİŞ ETDİYİNİZ MAĞAZALAR:\n");
        sb.append("═══════════════════════════════════\n\n");

        int count = 0;
        for (Map.Entry<String, Integer> entry : sorted) {
            if (count++ >= 3) break;
            sb.append(String.format(Locale.getDefault(),
                    "%d. %s - %d dəfə\n", count, entry.getKey(), entry.getValue()));
        }

        if (!sorted.isEmpty()) {
            sb.append("\n💡 TÖVSİYƏ: ");
            sb.append(String.format("Ən çox \"%s\" mağazasından alış-veriş edirsiniz. Bu mağazanın endirim və kampaniyalarını izləməyə davam edin!",
                    sorted.get(0).getKey()));
        }

        return sb.toString();
    }

    private String getProductSpendingAnswer() {
        if (productSpending.isEmpty()) {
            return "📭 Məhsul xərc məlumatları hələ kifayət deyil.";
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(productSpending.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        StringBuilder sb = new StringBuilder();
        sb.append("💰 ƏN ÇOX PUL XƏRCLƏDİYİNİZ MƏHSULLAR:\n");
        sb.append("═══════════════════════════════════\n\n");

        int count = 0;
        for (Map.Entry<String, Double> entry : sorted) {
            if (count++ >= 5) break;
            int freq = productFrequency.getOrDefault(entry.getKey(), 0);

            sb.append(String.format(Locale.getDefault(),
                    "%d. %s\n", count, entry.getKey()));
            sb.append(String.format(Locale.getDefault(),
                    "   • Ümumi xərc: ₼%.2f\n", entry.getValue()));
            sb.append(String.format(Locale.getDefault(),
                    "   • Alış sayı: %d dəfə\n", freq));
            sb.append(String.format(Locale.getDefault(),
                    "   • Orta qiymət: ₼%.2f\n\n", entry.getValue() / freq));
        }

        return sb.toString();
    }

    private String getFullSpendingAnalysis() {
        if (productFrequency.isEmpty() && categoryTotals.isEmpty()) {
            return "📭 Xərc təhlili üçün kifayət qədər məlumat yoxdur. Əvvəlcə əməliyyatlar əlavə edin.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📊 XƏRCLƏRİNİZİN TAM TƏHLİLİ\n");
        sb.append("═══════════════════════════════════\n\n");

        // 1. Ən çox alınan məhsul
        if (!productFrequency.isEmpty()) {
            String topProduct = Collections.max(productFrequency.entrySet(),
                    Map.Entry.comparingByValue()).getKey();
            int topCount = productFrequency.get(topProduct);
            sb.append("🛒 Ən çox aldığınız məhsul: ");
            sb.append(String.format("\"%s\" (%d dəfə)\n\n", topProduct, topCount));
        }


        // 2. Ən çox xərc olunan kateqoriya
        if (!categoryTotals.isEmpty()) {
            String topCategory = Collections.max(categoryTotals.entrySet(),
                    Map.Entry.comparingByValue()).getKey();
            double topAmount = categoryTotals.get(topCategory);
            double total = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();
            double percent = total > 0 ? (topAmount / total) * 100 : 0;
            sb.append("📁 Ən çox xərc etdiyiniz kateqoriya: ");
            sb.append(String.format("\"%s\" (₼%.2f - %.0f%%)\n\n", topCategory, topAmount, percent));
        }

        // 3. Ən çox alış-veriş edilən mağaza
        if (!storeFrequency.isEmpty()) {
            String topStore = Collections.max(storeFrequency.entrySet(),
                    Map.Entry.comparingByValue()).getKey();
            int storeCount = storeFrequency.get(topStore);
            sb.append("🏪 Ən çox alış-veriş etdiyiniz mağaza: ");
            sb.append(String.format("\"%s\" (%d dəfə)\n\n", topStore, storeCount));
        }

        // 4. Ümumi xərc
        double totalExpense = productSpending.values().stream().mapToDouble(Double::doubleValue).sum();
        sb.append(String.format("💰 Ümumi xərc: ₼%.2f\n\n", totalExpense));

        // 5. AI Tövsiyəsi
        sb.append("💡 AI TÖVSİYƏSİ:\n");
        if (!categoryTotals.isEmpty()) {
            String topCat = Collections.max(categoryTotals.entrySet(),
                    Map.Entry.comparingByValue()).getKey();
            double topCatAmount = categoryTotals.get(topCat);
            double totalCat = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();
            double catPercent = totalCat > 0 ? (topCatAmount / totalCat) * 100 : 0;

            if (catPercent > 40) {
                sb.append(String.format("   • \"%s\" kateqoriyası xərclərinizin %.0f%%-ni təşkil edir. Bu sahədə qənaət etmək üçün alternativ variantlar araşdırın.", topCat, catPercent));
            } else {
                sb.append("   • Xərcləriniz müxtəlif kateqoriyalara yayılmışdır. Bu yaxşı balansdır! Qənaət etməyə davam edin.");
            }
        } else {
            sb.append("   • Daha çox məlumat əlavə edin ki, dəqiq tövsiyə verə bilim.");
        }

        return sb.toString();
    }

    /**
     * Transaction-lardan məhsul analizi məlumatlarını yeniləyir
     * @param transactions - Firebase-dən çəkilmiş transaction siyahısı
     */
    public void updateProductAnalytics(List<Map<String, Object>> transactions) {
        Log.d(TAG, "=========================================");
        Log.d(TAG, "🔄 updateProductAnalytics çağırıldı!");
        Log.d(TAG, "Transaction list ölçüsü: " + (transactions != null ? transactions.size() : 0));

        if (transactions == null || transactions.isEmpty()) {
            Log.d(TAG, "⚠️ Transaction list boşdur və ya null!");
            return;
        }

        // Məlumatları təmizlə
        productFrequency.clear();
        productSpending.clear();
        categoryTotals.clear();
        categoryCount.clear();
        storeFrequency.clear();
        productPurchases.clear();

        int expenseCount = 0;
        int validProductCount = 0;
        Map<String, Double> tempProductSpending = new HashMap<>();
        Map<String, Integer> tempProductFrequency = new HashMap<>();

        for (Map<String, Object> t : transactions) {
            String type = (String) t.get("type");

            if ("expense".equals(type)) {
                expenseCount++;

                // Məhsul adını al
                String productName = (String) t.get("productName");

                // Əgər productName yoxdursa, note-dan çıxarmağa çalış
                if (productName == null || productName.isEmpty()) {
                    String note = (String) t.get("note");
                    if (note != null && !note.isEmpty()) {
                        // Note formatı: "• PIRSAAT YUMURTA AG - 30 ədəd x 0,19 AZN = 5,70 AZN"
                        String[] parts = note.split("•");
                        if (parts.length > 1) {
                            String firstProduct = parts[1].trim();
                            int dashIndex = firstProduct.indexOf("-");
                            if (dashIndex > 0) {
                                productName = firstProduct.substring(0, dashIndex).trim();
                            } else {
                                productName = firstProduct;
                            }
                            Log.d(TAG, "📝 Note-dan məhsul adı çıxarıldı: " + productName);
                        }
                    }
                }

                // Qiyməti al (amount və ya productTotal)
                Double amount = (Double) t.get("amount");
                Double productTotal = (Double) t.get("productTotal");

                double price = 0;
                if (amount != null && amount > 0) {
                    price = amount;
                } else if (productTotal != null && productTotal > 0) {
                    price = productTotal;
                }

                String category = (String) t.get("category");
                String storeName = (String) t.get("storeName");
                Long date = (Long) t.get("date");

                // Debug üçün log
                if (productName != null && !productName.isEmpty()) {
                    Log.d(TAG, "📦 Məhsul: " + productName + ", Qiymət: " + price + ", Kateqoriya: " + category);
                }

                // Məhsul adı və qiymət varsa, əlavə et
                if (productName != null && !productName.isEmpty() && price > 0) {
                    validProductCount++;

                    // Məhsul tezliyi (sayı)
                    int currentCount = tempProductFrequency.getOrDefault(productName, 0);
                    tempProductFrequency.put(productName, currentCount + 1);

                    // Məhsul xərci
                    double currentSpent = tempProductSpending.getOrDefault(productName, 0.0);
                    tempProductSpending.put(productName, currentSpent + price);

                    // Kateqoriya (əgər yoxdursa "Digər")
                    String cat = (category != null && !category.isEmpty()) ? category : "Digər";
                    categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0) + price);
                    categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + 1);

                    // Mağaza (əgər yoxdursa və ya "Mağaza adı tapılmadı"dırsa əlavə etmə)
                    if (storeName != null && !storeName.isEmpty() && !storeName.equals("Mağaza adı tapılmadı")) {
                        storeFrequency.put(storeName, storeFrequency.getOrDefault(storeName, 0) + 1);
                    }

                    // Alış məlumatı
                    productPurchases.add(new ProductPurchase(
                            productName, cat, storeName != null ? storeName : "Naməlum",
                            price, price, 1, date != null ? date : System.currentTimeMillis()
                    ));
                } else if (price > 0) {
                    // Məhsul adı yoxdursa, "Naməlum Məhsul" kimi əlavə et
                    String unknownProduct = "Naməlum Məhsul";
                    Log.d(TAG, "⚠️ Məhsul adı tapılmadı, '" + unknownProduct + "' istifadə olunur");

                    validProductCount++;
                    int currentCount = tempProductFrequency.getOrDefault(unknownProduct, 0);
                    tempProductFrequency.put(unknownProduct, currentCount + 1);

                    double currentSpent = tempProductSpending.getOrDefault(unknownProduct, 0.0);
                    tempProductSpending.put(unknownProduct, currentSpent + price);

                    String cat = (category != null && !category.isEmpty()) ? category : "Digər";
                    categoryTotals.put(cat, categoryTotals.getOrDefault(cat, 0.0) + price);
                    categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + 1);
                }
            }
        }

        // Məlumatları əsas map-lərə köçür
        productFrequency.putAll(tempProductFrequency);
        productSpending.putAll(tempProductSpending);

        Log.d(TAG, "✅ Xərc transaction sayı: " + expenseCount);
        Log.d(TAG, "✅ Etibarlı məhsul sayı: " + validProductCount);
        Log.d(TAG, "✅ Unikal məhsul sayı: " + productFrequency.size());
        Log.d(TAG, "✅ Kateqoriya sayı: " + categoryTotals.size());

        // Məhsul siyahısını göstər (azalan sıra ilə)
        if (!productFrequency.isEmpty()) {
            Log.d(TAG, "📊 MƏHSUL SİYAHISI (alış sayına görə):");

            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(productFrequency.entrySet());
            sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

            for (Map.Entry<String, Integer> entry : sorted) {
                double spent = productSpending.getOrDefault(entry.getKey(), 0.0);
                Log.d(TAG, "   • " + entry.getKey() + " - " + entry.getValue() + " dəfə, ₼" + String.format("%.2f", spent));
            }
        } else {
            Log.d(TAG, "⚠️ Heç bir məhsul əlavə edilmədi!");
            Log.d(TAG, "⚠️ Səbəb: transaction-larda 'productName' sahəsi yoxdur və ya boşdur!");
        }

        // ============ CACHE-DƏ SAXLA ============
        saveProductAnalyticsToCache();

        Log.d(TAG, "=========================================");
    }

    /**
     * Məhsul analizi məlumatlarını SharedPreferences cache-də saxlayır
     */
    private void saveProductAnalyticsToCache() {
        SharedPreferences prefs = context.getSharedPreferences("product_analytics_cache", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Məhsul məlumatlarını JSON formatında saxla
        StringBuilder productsSb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : productFrequency.entrySet()) {
            double spent = productSpending.getOrDefault(entry.getKey(), 0.0);
            productsSb.append(entry.getKey()).append("|").append(entry.getValue()).append("|").append(spent).append("\n");
        }
        editor.putString("products", productsSb.toString());

        // Kateqoriya məlumatlarını saxla
        StringBuilder categoriesSb = new StringBuilder();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            int count = categoryCount.getOrDefault(entry.getKey(), 0);
            categoriesSb.append(entry.getKey()).append("|").append(entry.getValue()).append("|").append(count).append("\n");
        }
        editor.putString("categories", categoriesSb.toString());

        // Mağaza məlumatlarını saxla
        StringBuilder storesSb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : storeFrequency.entrySet()) {
            storesSb.append(entry.getKey()).append("|").append(entry.getValue()).append("\n");
        }
        editor.putString("stores", storesSb.toString());

        editor.putLong("last_update", System.currentTimeMillis());
        editor.apply();

        Log.d(TAG, "💾 Məhsul analizi məlumatları cache-də saxlanıldı");
        Log.d(TAG, "   • Məhsul sayı: " + productFrequency.size());
        Log.d(TAG, "   • Kateqoriya sayı: " + categoryTotals.size());
        Log.d(TAG, "   • Mağaza sayı: " + storeFrequency.size());
    }

    /**
     * Cache-dən məhsul analizi məlumatlarını yükləyir
     */
    public void loadProductAnalyticsFromCache() {
        SharedPreferences prefs = context.getSharedPreferences("product_analytics_cache", Context.MODE_PRIVATE);

        String productsStr = prefs.getString("products", "");
        if (productsStr.isEmpty()) {
            Log.d(TAG, "📭 Cache-də məhsul məlumatı tapılmadı");
            return;
        }

        // Məhsul məlumatlarını yüklə
        productFrequency.clear();
        productSpending.clear();
        String[] productLines = productsStr.split("\n");
        for (String line : productLines) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("\\|");
            if (parts.length >= 3) {
                String name = parts[0];
                int count = Integer.parseInt(parts[1]);
                double spent = Double.parseDouble(parts[2]);
                productFrequency.put(name, count);
                productSpending.put(name, spent);
            }
        }

        // Kateqoriya məlumatlarını yüklə
        categoryTotals.clear();
        categoryCount.clear();
        String categoriesStr = prefs.getString("categories", "");
        if (!categoriesStr.isEmpty()) {
            String[] catLines = categoriesStr.split("\n");
            for (String line : catLines) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 3) {
                    String name = parts[0];
                    double total = Double.parseDouble(parts[1]);
                    int count = Integer.parseInt(parts[2]);
                    categoryTotals.put(name, total);
                    categoryCount.put(name, count);
                }
            }
        }

        // Mağaza məlumatlarını yüklə
        storeFrequency.clear();
        String storesStr = prefs.getString("stores", "");
        if (!storesStr.isEmpty()) {
            String[] storeLines = storesStr.split("\n");
            for (String line : storeLines) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    String name = parts[0];
                    int count = Integer.parseInt(parts[1]);
                    storeFrequency.put(name, count);
                }
            }
        }

        long lastUpdate = prefs.getLong("last_update", 0);
        Log.d(TAG, "📦 Cache-dən məlumatlar yükləndi:");
        Log.d(TAG, "   • Məhsul sayı: " + productFrequency.size());
        Log.d(TAG, "   • Kateqoriya sayı: " + categoryTotals.size());
        Log.d(TAG, "   • Mağaza sayı: " + storeFrequency.size());
        Log.d(TAG, "   • Son yeniləmə: " + new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date(lastUpdate)));
    }

    /**
     * Məhsul analizi məlumatlarının olub olmadığını yoxlayır
     */
    public boolean hasProductAnalytics() {
        return !productFrequency.isEmpty();
    }

    /**
     * Məhsul analizi məlumatlarını təmizləyir
     */
    public void clearProductAnalytics() {
        productFrequency.clear();
        productSpending.clear();
        categoryTotals.clear();
        categoryCount.clear();
        storeFrequency.clear();
        productPurchases.clear();

        SharedPreferences prefs = context.getSharedPreferences("product_analytics_cache", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Log.d(TAG, "🗑️ Məhsul analizi məlumatları təmizləndi");
    }

    // ==================== KÖHNƏ METODLAR (qalan hissə eyni qalır) ====================

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String getExpenseAnswer(String question) {
        if (cachedTotalExpense == 0) {
            SharedPreferences prefs = context.getSharedPreferences("finance_data", Context.MODE_PRIVATE);
            cachedTotalExpense = prefs.getFloat("total_expense", 0);
            if (cachedTotalExpense == 0) {
                return "📭 Hələ heç bir xərc məlumatı yoxdur. Əvvəlcə əməliyyatlar əlavə edin!";
            }
        }

        if (containsAny(question, "gün", "today", "bugün", "день")) {
            double todayExpense = dailyExpenses[0];
            return String.format("📆 Bugünkü xərcləriniz: %.2f AZN\n%s",
                    todayExpense, todayExpense > 50 ? "⚠️ Bugünkü xərcləriniz yüksəkdir!" : "✅ Normal səviyyədədir.");
        } else if (containsAny(question, "həftə", "week", "неделя")) {
            double weekExpense = 0;
            for (int i = 0; i < 7; i++) weekExpense += dailyExpenses[i];
            return String.format("📊 Həftəlik xərcləriniz: %.2f AZN\n📈 Orta gündəlik: %.2f AZN", weekExpense, weekExpense / 7);
        } else if (containsAny(question, "ay", "month", "месяц")) {
            return String.format("📈 Aylıq ümumi xərcləriniz: %.2f AZN", cachedTotalExpense);
        }
        return String.format("💰 Ümumi xərcləriniz: %.2f AZN", cachedTotalExpense);
    }

    private String getIncomeAnswer(String question) {
        if (cachedTotalIncome == 0) {
            SharedPreferences prefs = context.getSharedPreferences("finance_data", Context.MODE_PRIVATE);
            cachedTotalIncome = prefs.getFloat("total_income", 0);
            if (cachedTotalIncome == 0) {
                return "📭 Hələ heç bir gəlir məlumatı yoxdur. Maaş və digər gəlirləri əlavə edin!";
            }
        }
        if (containsAny(question, "maaş", "salary", "aylıq", "monthly")) {
            return String.format("💵 Aylıq maaşınız: %.2f AZN", cachedMonthlySalary);
        }
        return String.format("💵 Ümumi gəliriniz: %.2f AZN\n🏦 Aylıq maaş: %.2f AZN", cachedTotalIncome, cachedMonthlySalary);
    }

    private String getSavingsAnswer(String question) {
        double savings = cachedTotalIncome - cachedTotalExpense;
        double savingsRate = cachedTotalIncome > 0 ? (savings / cachedTotalIncome) * 100 : 0;
        if (cachedTotalIncome == 0) {
            return "📭 Qənaət hesablamaq üçün əvvəlcə gəlir məlumatlarınızı əlavə edin.";
        }
        String status = savingsRate >= 20 ? "🎉 Mükəmməl!" : savingsRate >= 10 ? "👍 Yaxşı!" : savingsRate > 0 ? "⚠️ Aşağıdır!" : "🔴 Xərclər gəlirdən çoxdur!";
        return String.format("💰 Qənaətiniz: %.2f AZN\n📊 Qənaət dərəcəsi: %.1f%%\n%s", savings, savingsRate, status);
    }

    private String getBalanceAnswer() {
        double balance = cachedTotalIncome - cachedTotalExpense;
        return String.format("%s Hazırkı balansınız: %.2f AZN\n%s",
                balance >= 0 ? "💚" : "🔴", balance,
                balance >= 0 ? "✅ Maliyyə vəziyyətiniz yaxşıdır!" : "⚠️ Xərcləriniz gəlirinizdən çoxdur!");
    }

    private String getAdvice() {
        double savings = cachedTotalIncome - cachedTotalExpense;
        double savingsRate = cachedTotalIncome > 0 ? (savings / cachedTotalIncome) * 100 : 0;
        if (savingsRate < 10) {
            return "⚠️ XƏBƏRDARLIQ: Qənaət dərəcəniz çox aşağıdır!\n\n🎯 Tövsiyələr:\n• Lazımsız abunəlikləri ləğv edin\n• Kafe/restoran xərclərini azaldın\n• Evdə yemək bişirməyə çalışın\n• Endirimləri izləyin";
        } else if (savingsRate < 20) {
            return "👍 Yaxşı gedirsiniz!\n\n🎯 Tövsiyələr:\n• Aylıq qənaət hədəfinizi 20%-ə çıxarın\n• Artıq pulunuzu investisiya etməyə başlayın";
        } else {
            return "🎉 MÖHTƏŞƏM!\n\n🎯 Tövsiyələr:\n• İnvestisiya portfelinizi şaxələndirin\n• Uzunmüddətli maliyyə hədəfləri qoyun";
        }
    }

    private String getAccurateForecast() {
        double avgDailyExpense = cachedTotalExpense > 0 ? cachedTotalExpense / 30 : 20;
        double forecast3d = avgDailyExpense * 3;
        double forecast7d = avgDailyExpense * 7;
        String date = new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date());
        String date3d = getDateAfterDays(3);
        String date7d = getDateAfterDays(7);
        return String.format("🔮 PROQNOZ (%s - %s)\n━━━━━━━━━━━━━━━━━━━━━━━━━━\n📊 Gündəlik orta: %.2f AZN\n\n📈 3 GÜN (%s - %s): %.2f AZN\n📈 7 GÜN (%s - %s): %.2f AZN\n\n💡 Gündəlik %.2f AZN qənaət etsəniz, ayda %.2f AZN yığarsınız!",
                date, date7d, avgDailyExpense, date, date3d, forecast3d, date, date7d, forecast7d, avgDailyExpense * 0.2, avgDailyExpense * 0.2 * 30);
    }

    private String getDateAfterDays(int days) {
        return new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date(System.currentTimeMillis() + days * 24L * 60 * 60 * 1000));
    }

    private String getAnalysisAnswer() {
        double balance = cachedTotalIncome - cachedTotalExpense;
        String healthStatus = balance >= 1000 ? "🌟 ƏLA" : balance >= 500 ? "👍 YAXŞI" : balance >= 0 ? "⚠️ ORTA" : "🔴 ZƏİF";
        return String.format("📊 MALİYYƏ ANALİZİ\n━━━━━━━━━━━━━━━━━━\n💰 Gəlir: %.2f AZN\n💸 Xərc: %.2f AZN\n💚 Balans: %.2f AZN\n📈 Sağlamlıq: %s",
                cachedTotalIncome, cachedTotalExpense, balance, healthStatus);
    }

    private String getComparisonAnswer() {
        double difference = cachedTotalIncome - cachedTotalExpense;
        return String.format("📊 GƏLİR VS XƏRC\n━━━━━━━━━━━━━━━━━━\n💵 Gəlir: %.2f AZN\n💸 Xərc: %.2f AZN\n%s",
                cachedTotalIncome, cachedTotalExpense,
                difference > 0 ? String.format("✅ Fərq: +%.2f AZN", difference) :
                        difference < 0 ? String.format("⚠️ Fərq: -%.2f AZN", Math.abs(difference)) : "📊 Bərabər");
    }

    private String getRandomGreeting() {
        String[] greetings = {"👋 Salam! Mən sizin AI maliyyə köməkçinizəm.", "🤖 Buyurun! Maliyyə suallarınıza cavab verirəm.", "💬 Salam! Xərc, gəlir və ya məhsul analizi?"};
        return greetings[random.nextInt(greetings.length)];
    }

    private String getHelpMessage() {
        return "🤖 MALİYYƏ KÖMƏKÇİSİ\n\n📌 Sual edə bilərsiniz:\n• Nə qədər xərclədim?\n• Nə qədər qənaət etdim?\n• Proqnoz ver\n• Məsləhət ver\n• Ən çox aldığım məhsullar\n• Kateqoriya analizi\n• Mağaza analizi\n• Xərclərimin təhlili";
    }

    private String getThankYouMessage() {
        String[] thanks = {"😊 Buyurun!", "👍 Rica edərəm!", "💪 Hər zaman köməyə hazırıq!"};
        return thanks[random.nextInt(thanks.length)];
    }

    private String getDefaultAnswer() {
        String[] defaults = {"🤔 Sualınızı tam başa düşmədim. Xərc, gəlir, qənaət, proqnoz, məsləhət, məhsul analizi haqqında soruşa bilərsiniz.",
                "💡 İpucu: 'Ən çox aldığım məhsullar' və ya 'Xərclərimin təhlili' yaza bilərsiniz."};
        return defaults[random.nextInt(defaults.length)];
    }

    public String createFinancialContext(double totalIncome, double totalExpense, double monthlySalary, double savings) {
        updateFinancialData(totalIncome, totalExpense, monthlySalary);
        return String.format("Total income: %.2f AZN. Total expenses: %.2f AZN. Monthly salary: %.2f AZN. Net savings: %.2f AZN.",
                totalIncome, totalExpense, monthlySalary, savings);
    }

    public void close() {
        Log.d(TAG, "MobileBERTQA closed.");
    }
}