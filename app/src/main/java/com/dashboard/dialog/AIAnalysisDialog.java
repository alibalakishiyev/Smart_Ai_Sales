package com.dashboard.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.smart_ai_sales.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AIAnalysisDialog extends AppCompatDialogFragment {

    private String analysisText;
    private String title;
    private Map<String, Double> categoryData;
    private Map<String, Map<String, Double>> categoryProductData; // Kateqoriya -> Məhsul -> Məbləğ
    private double totalIncome;
    private double totalExpense;
    private double monthlySalary;
    private List<Map<String, Object>> allTransactions;
    private String currentLanguage;
    private SharedPreferences sharedPreferences;

    // Constructor
    public AIAnalysisDialog(String analysisText, String title,
                            List<Map<String, Object>> allTransactions,
                            double totalIncome, double totalExpense,
                            double monthlySalary) {
        this.analysisText = analysisText != null ? analysisText : "Analiz məlumatı yoxdur";
        this.title = title != null ? title : "AI Analiz";
        this.allTransactions = allTransactions != null ? allTransactions : new ArrayList<>();
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.monthlySalary = monthlySalary;
        this.categoryData = new HashMap<>();
        this.categoryProductData = new HashMap<>();

        // Kateqoriya və məhsul məlumatlarını hazırla
        processTransactionData();
    }

    private void processTransactionData() {
        for (Map<String, Object> t : allTransactions) {
            String type = (String) t.get("type");
            if ("expense".equals(type)) {
                String category = (String) t.get("category");
                Double amount = (Double) t.get("amount");
                String productName = (String) t.get("productName");

                if (category != null && amount != null) {
                    // Kateqoriya üzrə cəmi
                    categoryData.put(category, categoryData.getOrDefault(category, 0.0) + amount);

                    // Kateqoriya daxilində məhsul üzrə cəmi
                    if (productName != null && !productName.isEmpty()) {
                        categoryProductData.putIfAbsent(category, new HashMap<>());
                        Map<String, Double> products = categoryProductData.get(category);
                        products.put(productName, products.getOrDefault(productName, 0.0) + amount);
                    }
                }
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_ai_analysis, container, false);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Dil yüklə
        loadLanguage();

        try {
            // Başlıq
            TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
            if (tvTitle != null) {
                tvTitle.setText(getLocalizedTitle());
            }

            // Bağlama düyməsi
            ImageView btnClose = view.findViewById(R.id.btnClose);
            if (btnClose != null) {
                btnClose.setOnClickListener(v -> dismiss());
            }

            // AI Analiz Mətni
            TextView tvAnalysis = view.findViewById(R.id.tvAnalysis);
            if (tvAnalysis != null) {
                String detailedAnalysis = generateDetailedAnalysis();
                tvAnalysis.setText(detailedAnalysis);
                tvAnalysis.setMovementMethod(new ScrollingMovementMethod());
            }

            // Kateqoriya məlumatları
            setupCategoryData(view);

            // Ümumi statistika
            setupSummary(view);

            // Tövsiyələr bölməsi
            setupRecommendations(view);

            // Maliyyə sağlamlığı
            setupFinancialHealth(view);

            // Bağlama düyməsi 2
            TextView btnClose2 = view.findViewById(R.id.btnClose2);
            if (btnClose2 != null) {
                btnClose2.setOnClickListener(v -> dismiss());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return view;
    }

    private void loadLanguage() {
        if (getContext() != null) {
            sharedPreferences = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
            currentLanguage = sharedPreferences.getString("app_language", "az");
        } else {
            currentLanguage = "az";
        }
    }

    private String getLocalizedTitle() {
        switch (currentLanguage) {
            case "en":
                return "🤖 AI Financial Analysis";
            case "ru":
                return "🤖 AI Финансовый Анализ";
            default:
                return "🤖 AI Maliyyə Analizi";
        }
    }

    private String getStringByKey(String keyAz, String keyEn, String keyRu) {
        switch (currentLanguage) {
            case "en":
                return keyEn;
            case "ru":
                return keyRu;
            default:
                return keyAz;
        }
    }

    private String generateDetailedAnalysis() {
        StringBuilder analysis = new StringBuilder();

        String separator = getStringByKey(
                "══════════════════════════════════════\n",
                "══════════════════════════════════════\n",
                "══════════════════════════════════════\n"
        );

        String titleText = getStringByKey(
                "     🤖 AI MALİYYƏ ANALİZİ     \n",
                "     🤖 AI FINANCIAL ANALYSIS     \n",
                "     🤖 AI ФИНАНСОВЫЙ АНАЛИЗ     \n"
        );

        analysis.append(separator);
        analysis.append(titleText);
        analysis.append(separator);
        analysis.append("\n");

        // 1. ÜMUMİ MƏLUMATLAR
        String summaryTitle = getStringByKey(
                "📊 ÜMUMİ MƏLUMATLAR:\n",
                "📊 SUMMARY STATISTICS:\n",
                "📊 ОБЩАЯ СТАТИСТИКА:\n"
        );
        String totalIncomeText = getStringByKey("• Ümumi Gəlir: ", "• Total Income: ", "• Общий Доход: ");
        String totalExpenseText = getStringByKey("• Ümumi Xərc: ", "• Total Expense: ", "• Общий Расход: ");
        String netProfitText = getStringByKey("• Xalis Mənfəət: ", "• Net Profit: ", "• Чистая Прибыль: ");
        String transactionCountText = getStringByKey("• Əməliyyat sayı: ", "• Transactions: ", "• Количество операций: ");

        analysis.append(summaryTitle);
        analysis.append("────────────────────\n");
        analysis.append(String.format("%s₼%.2f\n", totalIncomeText, totalIncome));
        analysis.append(String.format("%s₼%.2f\n", totalExpenseText, totalExpense));
        analysis.append(String.format("%s₼%.2f\n", netProfitText, (totalIncome - totalExpense)));
        analysis.append(String.format("%s%d\n\n", transactionCountText, allTransactions.size()));

        // 2. AYLIK ANALİZ
        if (monthlySalary > 0) {
            String monthlyTitle = getStringByKey(
                    "💰 AYLIK ANALİZ:\n",
                    "💰 MONTHLY ANALYSIS:\n",
                    "💰 МЕСЯЧНЫЙ АНАЛИЗ:\n"
            );
            String avgSalaryText = getStringByKey("• Orta Maaş: ", "• Average Salary: ", "• Средняя Зарплата: ");
            String monthlyExpenseText = getStringByKey("• Aylıq Xərc: ", "• Monthly Expense: ", "• Месячный Расход: ");
            String savingsRateText = getStringByKey("• Qənaət dərəcəsi: ", "• Savings Rate: ", "• Норма сбережений: ");

            analysis.append(monthlyTitle);
            analysis.append("────────────────────\n");
            analysis.append(String.format("%s₼%.2f\n", avgSalaryText, monthlySalary));
            analysis.append(String.format("%s₼%.2f\n", monthlyExpenseText, totalExpense));

            double savingsRate = ((monthlySalary - totalExpense) / monthlySalary) * 100;
            analysis.append(String.format("%s%.1f%%\n\n", savingsRateText, savingsRate));
        }

        // 3. KATEQORİYA VƏ MƏHSULLAR ANALİZİ
        if (categoryData != null && !categoryData.isEmpty()) {
            String categoryTitle = getStringByKey(
                    "📋 XƏRC KATEQORİYALARI VƏ MƏHSULLAR:\n",
                    "📋 EXPENSE CATEGORIES & PRODUCTS:\n",
                    "📋 КАТЕГОРИИ РАСХОДОВ И ТОВАРЫ:\n"
            );
            analysis.append(categoryTitle);
            analysis.append("────────────────────\n");

            // Kateqoriyaları sırala
            List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(categoryData.entrySet());
            sortedCategories.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            double totalExpenseAmount = 0;
            for (double value : categoryData.values()) {
                totalExpenseAmount += value;
            }

            for (Map.Entry<String, Double> entry : sortedCategories) {
                String category = entry.getKey();
                double amount = entry.getValue();
                double percent = totalExpenseAmount > 0 ? (amount / totalExpenseAmount) * 100 : 0;

                analysis.append(String.format("\n📌 %s: ₼%.2f (%.1f%%)\n", category, amount, percent));

                // Bu kateqoriyadakı məhsulları göstər
                if (categoryProductData.containsKey(category)) {
                    Map<String, Double> products = categoryProductData.get(category);
                    List<Map.Entry<String, Double>> sortedProducts = new ArrayList<>(products.entrySet());
                    sortedProducts.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

                    for (Map.Entry<String, Double> product : sortedProducts) {
                        double productPercent = amount > 0 ? (product.getValue() / amount) * 100 : 0;
                        analysis.append(String.format("   • %s: ₼%.2f (%.0f%%)\n",
                                product.getKey(), product.getValue(), productPercent));
                    }
                }
            }
            analysis.append("\n");
        }

        // 4. PROQNOZLAR
        String predictionTitle = getStringByKey(
                "🔮 PROQNOZLAR:\n",
                "🔮 PREDICTIONS:\n",
                "🔮 ПРОГНОЗЫ:\n"
        );
        String nextMonthIncomeText = getStringByKey("• Növbəti ay gəlir: ", "• Next month income: ", "• Доход в следующем месяце: ");
        String nextMonthExpenseText = getStringByKey("• Növbəti ay xərc: ", "• Next month expense: ", "• Расход в следующем месяце: ");
        String expectedSavingsText = getStringByKey("• Gözlənilən qənaət: ", "• Expected savings: ", "• Ожидаемая экономия: ");

        analysis.append(predictionTitle);
        analysis.append("────────────────────\n");
        double[] predictions = predictNextMonth();
        analysis.append(String.format("%s₼%.2f\n", nextMonthIncomeText, predictions[0]));
        analysis.append(String.format("%s₼%.2f\n", nextMonthExpenseText, predictions[1]));
        analysis.append(String.format("%s₼%.2f\n\n", expectedSavingsText, (predictions[0] - predictions[1])));

        analysis.append(separator);
        return analysis.toString();
    }

    private double[] predictNextMonth() {
        double[] predictions = new double[2];
        if (allTransactions.isEmpty()) return predictions;

        long threeMonthsAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000);
        Map<String, Double> monthlyIncome = new HashMap<>();
        Map<String, Double> monthlyExpense = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");

        for (Map<String, Object> t : allTransactions) {
            Long date = (Long) t.get("date");
            String type = (String) t.get("type");
            Double amount = (Double) t.get("amount");

            if (date != null && amount != null && date >= threeMonthsAgo) {
                String month = sdf.format(new java.util.Date(date));
                if ("income".equals(type)) {
                    monthlyIncome.put(month, monthlyIncome.getOrDefault(month, 0.0) + amount);
                } else {
                    monthlyExpense.put(month, monthlyExpense.getOrDefault(month, 0.0) + amount);
                }
            }
        }

        double totalMonthIncome = monthlyIncome.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalMonthExpense = monthlyExpense.values().stream().mapToDouble(Double::doubleValue).sum();
        int monthCount = monthlyIncome.size();

        predictions[0] = monthCount > 0 ? totalMonthIncome / monthCount : totalIncome;
        predictions[1] = monthCount > 0 ? totalMonthExpense / monthCount : totalExpense;
        return predictions;
    }

    private void setupCategoryData(View view) {
        if (view == null) return;
        LinearLayout container = view.findViewById(R.id.cardCategory);
        if (container == null) return;
        container.removeAllViews();

        if (categoryData == null || categoryData.isEmpty()) {
            String emptyMsg = getStringByKey(
                    "Kateqoriya məlumatı yoxdur",
                    "No category data available",
                    "Нет данных по категориям"
            );
            addEmptyView(container, emptyMsg);
            return;
        }

        double total = categoryData.values().stream().mapToDouble(Double::doubleValue).sum();
        List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(categoryData.entrySet());
        sortedCategories.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        for (Map.Entry<String, Double> entry : sortedCategories) {
            String category = entry.getKey();
            Double amount = entry.getValue();
            if (category == null || amount == null) continue;

            View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_analysis_category, container, false);

            TextView tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            TextView tvCategoryAmount = itemView.findViewById(R.id.tvCategoryAmount);
            TextView tvCategoryPercent = itemView.findViewById(R.id.tvCategoryPercent);
            LinearLayout productContainer = itemView.findViewById(R.id.productContainer);

            if (tvCategoryName != null) tvCategoryName.setText(category);
            if (tvCategoryAmount != null) tvCategoryAmount.setText(String.format("₼%.2f", amount));

            int percent = total > 0 ? (int) ((amount / total) * 100) : 0;
            if (tvCategoryPercent != null) tvCategoryPercent.setText(percent + "%");

            // Kateqoriya daxilində məhsulları göstər
            if (categoryProductData.containsKey(category) && productContainer != null) {
                Map<String, Double> products = categoryProductData.get(category);
                List<Map.Entry<String, Double>> sortedProducts = new ArrayList<>(products.entrySet());
                sortedProducts.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

                for (Map.Entry<String, Double> product : sortedProducts) {
                    View productView = LayoutInflater.from(getContext()).inflate(R.layout.item_product_in_category, productContainer, false);
                    TextView tvProductName = productView.findViewById(R.id.tvProductName);
                    TextView tvProductAmount = productView.findViewById(R.id.tvProductAmount);

                    if (tvProductName != null) tvProductName.setText("• " + product.getKey());
                    if (tvProductAmount != null) tvProductAmount.setText(String.format("₼%.2f", product.getValue()));

                    productContainer.addView(productView);
                }
            }

            container.addView(itemView);
        }
    }

    private void setupRecommendations(View view) {
        if (view == null) return;
        LinearLayout container = view.findViewById(R.id.cardRecommendations);
        if (container == null) return;
        container.removeAllViews();

        List<String> recommendations = generateRecommendations();

        if (recommendations.isEmpty()) {
            String emptyMsg = getStringByKey(
                    "Tövsiyə yoxdur",
                    "No recommendations available",
                    "Нет рекомендаций"
            );
            addEmptyView(container, emptyMsg);
            return;
        }

        for (String rec : recommendations) {
            View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_recommendation, container, false);
            TextView tvRecommendation = itemView.findViewById(R.id.tvRecommendation);
            if (tvRecommendation != null) tvRecommendation.setText(rec);
            container.addView(itemView);
        }
    }

    private List<String> generateRecommendations() {
        List<String> recommendations = new ArrayList<>();

        if (monthlySalary <= 0) {
            String noSalaryMsg = getStringByKey(
                    "Maaş məlumatı əlavə edin (kateqoriya: Maaş)",
                    "Add salary information (category: Salary)",
                    "Добавьте информацию о зарплате (категория: Зарплата)"
            );
            recommendations.add(noSalaryMsg);
            return recommendations;
        }

        double savingsRate = ((monthlySalary - totalExpense) / monthlySalary) * 100;

        if (savingsRate < 0) {
            recommendations.add(getStringByKey("🔴 Xərcləriniz maaşınızdan çoxdur!", "🔴 Your expenses exceed your salary!", "🔴 Ваши расходы превышают зарплату!"));
            recommendations.add(getStringByKey("Təcili olaraq xərcləri azaldın", "Reduce expenses urgently", "Срочно сократите расходы"));
            recommendations.add(getStringByKey("Lazımsız xərcləri kəsin", "Cut unnecessary expenses", "Откажитесь от ненужных трат"));
        } else if (savingsRate < 10) {
            recommendations.add(getStringByKey("⚠️ Qənaət nisbətiniz çox aşağıdır (10%-dən az)", "⚠️ Your savings rate is too low (below 10%)", "⚠️ Ваша норма сбережений слишком низкая (менее 10%)"));
            recommendations.add(getStringByKey("Xərclərinizi 20% azaltmağa çalışın", "Try to reduce your expenses by 20%", "Попробуйте сократить расходы на 20%"));
            recommendations.add(getStringByKey("Gündəlik xərcləri izləyin", "Track your daily expenses", "Отслеживайте ежедневные расходы"));
        } else if (savingsRate < 20) {
            recommendations.add(getStringByKey("👍 Qənaət nisbətiniz orta səviyyədədir", "👍 Your savings rate is average", "👍 Ваша норма сбережений средняя"));
            recommendations.add(getStringByKey("Hədəfiniz 20% qənaət etmək olsun", "Set a goal to save 20%", "Поставьте цель экономить 20%"));
            recommendations.add(getStringByKey("İnvestisiya variantlarını araşdırın", "Explore investment options", "Изучите варианты инвестиций"));
        } else {
            recommendations.add(getStringByKey("🎉 Əla qənaət nisbəti!", "🎉 Excellent savings rate!", "🎉 Отличная норма сбережений!"));
            recommendations.add(getStringByKey("İnvestisiya etməyə başlaya bilərsiniz", "You can start investing", "Вы можете начать инвестировать"));
            recommendations.add(getStringByKey("Uzunmüddətli maliyyə planı qurun", "Create a long-term financial plan", "Составьте долгосрочный финансовый план"));
        }

        // Kateqoriya əsaslı tövsiyələr
        if (categoryData != null && !categoryData.isEmpty()) {
            String topCategory = "";
            double maxAmount = 0;
            for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
                if (entry.getValue() > maxAmount) {
                    maxAmount = entry.getValue();
                    topCategory = entry.getKey();
                }
            }

            if (!topCategory.isEmpty()) {
                String topCategoryMsg = getStringByKey(
                        String.format("📊 Ən çox xərc: %s (₼%.2f)", topCategory, maxAmount),
                        String.format("📊 Top expense: %s (₼%.2f)", topCategory, maxAmount),
                        String.format("📊 Самый большой расход: %s (₼%.2f)", topCategory, maxAmount)
                );
                recommendations.add(topCategoryMsg);

                String categoryLower = topCategory.toLowerCase();
                if (categoryLower.contains("restoran") || categoryLower.contains("kafe") || categoryLower.contains("yemək")) {
                    recommendations.add(getStringByKey("🍽️ Evdə yemək hazırlamaq daha qənaətcildir", "🍽️ Cooking at home is more economical", "🍽️ Готовить дома экономичнее"));
                } else if (categoryLower.contains("əyləncə") || categoryLower.contains("oyun")) {
                    recommendations.add(getStringByKey("🎮 Əyləncə xərclərini məhdudlaşdırın", "🎮 Limit entertainment expenses", "🎮 Ограничьте расходы на развлечения"));
                } else if (categoryLower.contains("nəqliyyat") || categoryLower.contains("taksi")) {
                    recommendations.add(getStringByKey("🚗 İctimai nəqliyyatdan istifadə edin", "🚗 Use public transportation", "🚗 Пользуйтесь общественным транспортом"));
                } else if (categoryLower.contains("alış-veriş") || categoryLower.contains("geyim")) {
                    recommendations.add(getStringByKey("🛍️ Endirimləri izləyin, ehtiyac siyahısı hazırlayın", "🛍️ Track discounts, make a needs list", "🛍️ Следите за скидками, составляйте список потребностей"));
                }
            }
        }

        return recommendations;
    }

    private void setupFinancialHealth(View view) {
        if (view == null) return;
        TextView tvHealthScore = view.findViewById(R.id.tvHealthScore);
        TextView tvHealthDetails = view.findViewById(R.id.tvHealthDetails);
        if (tvHealthScore == null || tvHealthDetails == null) return;

        if (monthlySalary <= 0) {
            String lowDataMsg = getStringByKey("Məlumat az", "Insufficient data", "Недостаточно данных");
            String addSalaryMsg = getStringByKey("Maaş məlumatı daxil edin", "Add salary information", "Добавьте информацию о зарплате");
            tvHealthScore.setText(lowDataMsg);
            tvHealthScore.setTextColor(Color.parseColor("#F59E0B"));
            tvHealthDetails.setText(addSalaryMsg);
            return;
        }

        double savingsRate = ((monthlySalary - totalExpense) / monthlySalary) * 100;
        String healthText;
        int healthColor;
        String healthDetails;

        if (savingsRate >= 20) {
            healthText = getStringByKey("🌟 Əla", "🌟 Excellent", "🌟 Отлично");
            healthColor = Color.parseColor("#10B981");
            healthDetails = getStringByKey(
                    String.format("Qənaət: %.1f%% | İnvestisiya vaxtıdır!", savingsRate),
                    String.format("Savings: %.1f%% | Time to invest!", savingsRate),
                    String.format("Сбережения: %.1f%% | Время инвестировать!", savingsRate)
            );
        } else if (savingsRate >= 10) {
            healthText = getStringByKey("👍 Yaxşı", "👍 Good", "👍 Хорошо");
            healthColor = Color.parseColor("#3B82F6");
            healthDetails = getStringByKey(
                    String.format("Qənaət: %.1f%% | Yaxşı vəziyyət", savingsRate),
                    String.format("Savings: %.1f%% | Good condition", savingsRate),
                    String.format("Сбережения: %.1f%% | Хорошее состояние", savingsRate)
            );
        } else if (savingsRate >= 0) {
            healthText = getStringByKey("⚠️ Orta", "⚠️ Average", "⚠️ Средне");
            healthColor = Color.parseColor("#F59E0B");
            healthDetails = getStringByKey(
                    String.format("Qənaət: %.1f%% | Diqqətli olun", savingsRate),
                    String.format("Savings: %.1f%% | Be careful", savingsRate),
                    String.format("Сбережения: %.1f%% | Будьте внимательны", savingsRate)
            );
        } else {
            healthText = getStringByKey("🔴 Zəif", "🔴 Poor", "🔴 Плохо");
            healthColor = Color.parseColor("#EF4444");
            healthDetails = getStringByKey(
                    String.format("Zərər: %.1f%% | Təcili tədbir!", Math.abs(savingsRate)),
                    String.format("Loss: %.1f%% | Urgent action!", Math.abs(savingsRate)),
                    String.format("Убыток: %.1f%% | Срочные меры!", Math.abs(savingsRate))
            );
        }

        tvHealthScore.setText(healthText);
        tvHealthScore.setTextColor(healthColor);
        tvHealthDetails.setText(healthDetails);
    }

    private void addEmptyView(LinearLayout container, String message) {
        if (container == null || getContext() == null) return;
        TextView emptyView = new TextView(getContext());
        emptyView.setText(message);
        emptyView.setTextColor(Color.parseColor("#B3FFFFFF"));
        emptyView.setTextSize(14);
        emptyView.setPadding(32, 32, 32, 32);
        emptyView.setGravity(android.view.Gravity.CENTER);
        container.addView(emptyView);
    }

    private void setupSummary(View view) {
        if (view == null) return;
        try {
            TextView tvTotalIncome = view.findViewById(R.id.chipIncome);
            TextView tvTotalExpense = view.findViewById(R.id.chipExpense);
            TextView tvNetProfit = view.findViewById(R.id.cardProfile);
            TextView tvNetProfitValue = view.findViewById(R.id.cardProfile);
            TextView tvMonthlySalaryText = view.findViewById(R.id.etSalaryNote);
            TextView tvMonthlySalaryValue = view.findViewById(R.id.tvMonthlySalary);

            if (tvTotalIncome != null) tvTotalIncome.setText(String.format("₼%.2f", totalIncome));
            if (tvTotalExpense != null) tvTotalExpense.setText(String.format("₼%.2f", totalExpense));

            double netProfit = totalIncome - totalExpense;
            if (tvNetProfitValue != null) {
                tvNetProfitValue.setText(String.format("₼%.2f", netProfit));
                tvNetProfitValue.setTextColor(netProfit >= 0 ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
            }
            if (tvNetProfit != null) tvNetProfit.setText(netProfit >= 0 ?
                    getStringByKey("Mənfəət", "Profit", "Прибыль") :
                    getStringByKey("Zərər", "Loss", "Убыток"));

            if (monthlySalary > 0) {
                if (tvMonthlySalaryText != null) tvMonthlySalaryText.setVisibility(View.VISIBLE);
                if (tvMonthlySalaryValue != null) {
                    tvMonthlySalaryValue.setVisibility(View.VISIBLE);
                    tvMonthlySalaryValue.setText(String.format("₼%.2f", monthlySalary));
                }
            } else {
                if (tvMonthlySalaryText != null) tvMonthlySalaryText.setVisibility(View.GONE);
                if (tvMonthlySalaryValue != null) tvMonthlySalaryValue.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}