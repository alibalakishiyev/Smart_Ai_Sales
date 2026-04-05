package com.dashboard.dialog;

import android.app.Dialog;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AIAnalysisDialog extends AppCompatDialogFragment {

    private String analysisText;
    private String title;
    private Map<String, Double> categoryData;
    private double totalIncome;
    private double totalExpense;
    private double monthlySalary;
    private List<Map<String, Object>> allTransactions;

    // Əsas constructor
    public AIAnalysisDialog(String analysisText, String title,
                            Map<String, Double> categoryData,
                            double totalIncome, double totalExpense,
                            double monthlySalary) {
        this.analysisText = analysisText != null ? analysisText : "Analiz məlumatı yoxdur";
        this.title = title != null ? title : "AI Analiz";
        this.categoryData = categoryData;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.monthlySalary = monthlySalary;
        this.allTransactions = new ArrayList<>();
    }

    // Transaction listi ilə constructor
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

        // Kateqoriya məlumatlarını transactionlardan hazırla
        this.categoryData = new HashMap<>();
        for (Map<String, Object> t : this.allTransactions) {
            String type = (String) t.get("type");
            if ("expense".equals(type)) {
                String category = (String) t.get("category");
                Double amount = (Double) t.get("amount");
                if (category != null && amount != null) {
                    this.categoryData.put(category,
                            this.categoryData.getOrDefault(category, 0.0) + amount);
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

        try {
            // Başlıq
            TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
            if (tvTitle != null) {
                tvTitle.setText(title);
            }

            // Bağlama düyməsi
            ImageView btnClose = view.findViewById(R.id.btnClose);
            if (btnClose != null) {
                btnClose.setOnClickListener(v -> dismiss());
            }

            // AI Analiz Mətni - ƏTRAFLI ANALİZ
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

    private String generateDetailedAnalysis() {
        StringBuilder analysis = new StringBuilder();

        analysis.append("══════════════════════════════════════\n");
        analysis.append("     🤖 AI MALİYYƏ ANALİZİ     \n");
        analysis.append("══════════════════════════════════════\n\n");

        // 1. ÜMUMİ MƏLUMATLAR
        analysis.append("📊 ÜMUMİ MƏLUMATLAR:\n");
        analysis.append("────────────────────\n");
        analysis.append(String.format("• Ümumi Gəlir: ₼%.2f\n", totalIncome));
        analysis.append(String.format("• Ümumi Xərc: ₼%.2f\n", totalExpense));
        analysis.append(String.format("• Xalis Mənfəət: ₼%.2f\n", (totalIncome - totalExpense)));
        analysis.append(String.format("• Əməliyyat sayı: %d\n\n", allTransactions.size()));

        // 2. AYLIK ANALİZ
        if (monthlySalary > 0) {
            analysis.append("💰 AYLIK ANALİZ:\n");
            analysis.append("────────────────────\n");
            analysis.append(String.format("• Orta Maaş: ₼%.2f\n", monthlySalary));
            analysis.append(String.format("• Aylıq Xərc: ₼%.2f\n", totalExpense));

            double savingsRate = ((monthlySalary - totalExpense) / monthlySalary) * 100;
            analysis.append(String.format("• Qənaət dərəcəsi: %.1f%%\n\n", savingsRate));
        }

        // 3. KATEQORİYA ANALİZİ
        if (categoryData != null && !categoryData.isEmpty()) {
            analysis.append("📋 XƏRC KATEQORİYALARI:\n");
            analysis.append("────────────────────\n");

            List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(categoryData.entrySet());
            sortedCategories.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            double totalExpenseAmount = 0;
            for (double value : categoryData.values()) {
                totalExpenseAmount += value;
            }

            for (Map.Entry<String, Double> entry : sortedCategories) {
                double percent = totalExpenseAmount > 0 ? (entry.getValue() / totalExpenseAmount) * 100 : 0;
                analysis.append(String.format("• %s: ₼%.2f (%.1f%%)\n",
                        entry.getKey(), entry.getValue(), percent));
            }
            analysis.append("\n");
        }

        // 4. GÜNLÜK ANALİZ
        if (!allTransactions.isEmpty()) {
            analysis.append("📅 GÜNLÜK ANALİZ:\n");
            analysis.append("────────────────────\n");

            Map<String, Double> dailyIncome = new HashMap<>();
            Map<String, Double> dailyExpense = new HashMap<>();
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());

            for (Map<String, Object> t : allTransactions) {
                Long date = (Long) t.get("date");
                String type = (String) t.get("type");
                Double amount = (Double) t.get("amount");

                if (date != null && amount != null) {
                    String dayStr = sdf.format(new java.util.Date(date));
                    if ("income".equals(type)) {
                        dailyIncome.put(dayStr, dailyIncome.getOrDefault(dayStr, 0.0) + amount);
                    } else {
                        dailyExpense.put(dayStr, dailyExpense.getOrDefault(dayStr, 0.0) + amount);
                    }
                }
            }

            // Son 5 günün ortalaması
            double avgDailyIncome = dailyIncome.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double avgDailyExpense = dailyExpense.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);

            analysis.append(String.format("• Ort. günlük gəlir: ₼%.2f\n", avgDailyIncome));
            analysis.append(String.format("• Ort. günlük xərc: ₼%.2f\n\n", avgDailyExpense));
        }

        // 5. PROQNOZLAR
        analysis.append("🔮 PROQNOZLAR:\n");
        analysis.append("────────────────────\n");

        double[] predictions = predictNextMonth();
        analysis.append(String.format("• Növbəti ay gəlir: ₼%.2f\n", predictions[0]));
        analysis.append(String.format("• Növbəti ay xərc: ₼%.2f\n", predictions[1]));
        analysis.append(String.format("• Gözlənilən qənaət: ₼%.2f\n\n", (predictions[0] - predictions[1])));

        analysis.append("\n══════════════════════════════════════\n");

        return analysis.toString();
    }

    private double[] predictNextMonth() {
        double[] predictions = new double[2];

        if (allTransactions.isEmpty()) {
            return predictions;
        }

        // Son 3 ayın ortalaması
        long threeMonthsAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000);
        double totalMonthIncome = 0, totalMonthExpense = 0;
        int monthCount = 0;

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

        for (double value : monthlyIncome.values()) {
            totalMonthIncome += value;
            monthCount++;
        }

        for (double value : monthlyExpense.values()) {
            totalMonthExpense += value;
        }

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
            addEmptyView(container, "Kateqoriya məlumatı yoxdur");
            return;
        }

        double total = 0;
        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            if (entry.getValue() != null) {
                total += entry.getValue();
            }
        }

        int count = 0;
        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            if (count >= 5) break;

            String category = entry.getKey();
            Double amount = entry.getValue();
            if (category == null || amount == null) continue;

            try {
                View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_analysis_category, container, false);

                TextView tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
                TextView tvCategoryAmount = itemView.findViewById(R.id.tvCategoryAmount);
                TextView tvCategoryPercent = itemView.findViewById(R.id.tvCategoryPercent);

                if (tvCategoryName != null) {
                    tvCategoryName.setText(category);
                }

                if (tvCategoryAmount != null) {
                    tvCategoryAmount.setText(String.format(Locale.getDefault(), "₼%.2f", amount));
                }

                if (tvCategoryPercent != null) {
                    int percent = total > 0 ? (int) ((amount / total) * 100) : 0;
                    tvCategoryPercent.setText(percent + "%");
                }

                container.addView(itemView);
                count++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (count == 0) {
            addEmptyView(container, "Kateqoriya məlumatı yoxdur");
        }
    }

    private void setupRecommendations(View view) {
        if (view == null) return;

        LinearLayout container = view.findViewById(R.id.cardRecommendations);
        if (container == null) return;

        container.removeAllViews();

        List<String> recommendations = generateRecommendations();

        if (recommendations.isEmpty()) {
            addEmptyView(container, "Tövsiyə yoxdur");
            return;
        }

        for (String rec : recommendations) {
            try {
                View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_recommendation, container, false);

                TextView tvRecommendation = itemView.findViewById(R.id.tvRecommendation);
                if (tvRecommendation != null) {
                    tvRecommendation.setText("• " + rec);
                }

                container.addView(itemView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private List<String> generateRecommendations() {
        List<String> recommendations = new ArrayList<>();

        if (monthlySalary <= 0) {
            recommendations.add("Maaş məlumatı əlavə edin (kateqoriya: Maaş)");
            return recommendations;
        }

        double savingsRate = ((monthlySalary - totalExpense) / monthlySalary) * 100;

        if (savingsRate < 0) {
            recommendations.add("🔴 Xərcləriniz maaşınızdan çoxdur!");
            recommendations.add("Təcili olaraq xərcləri azaldın");
            recommendations.add("Lazımsız xərcləri kəsin");
        } else if (savingsRate < 10) {
            recommendations.add("⚠️ Qənaət nisbətiniz çox aşağıdır (10%-dən az)");
            recommendations.add("Xərclərinizi 20% azaltmağa çalışın");
            recommendations.add("Gündəlik xərcləri izləyin");
        } else if (savingsRate < 20) {
            recommendations.add("👍 Qənaət nisbətiniz orta səviyyədədir");
            recommendations.add("Hədəfiniz 20% qənaət etmək olsun");
            recommendations.add("İnvestisiya variantlarını araşdırın");
        } else {
            recommendations.add("🎉 Əla qənaət nisbəti!");
            recommendations.add("İnvestisiya etməyə başlaya bilərsiniz");
            recommendations.add("Uzunmüddətli maliyyə planı qurun");
        }

        // Kateqoriya tövsiyələri
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
                recommendations.add(String.format("📊 Ən çox xərc: %s (₼%.2f)", topCategory, maxAmount));

                if (topCategory.toLowerCase().contains("restoran") ||
                        topCategory.toLowerCase().contains("kafe") ||
                        topCategory.toLowerCase().contains("yemək")) {
                    recommendations.add("🍽️ Evdə yemək hazırlamaq daha qənaətcildir");
                } else if (topCategory.toLowerCase().contains("əyləncə") ||
                        topCategory.toLowerCase().contains("oyun")) {
                    recommendations.add("🎮 Əyləncə xərclərini məhdudlaşdırın");
                } else if (topCategory.toLowerCase().contains("nəqliyyat") ||
                        topCategory.toLowerCase().contains("taksi")) {
                    recommendations.add("🚗 İctimai nəqliyyatdan istifadə edin");
                } else if (topCategory.toLowerCase().contains("alış-veriş") ||
                        topCategory.toLowerCase().contains("geyim")) {
                    recommendations.add("🛍️ Endirimləri izləyin, ehtiyac siyahısı hazırlayın");
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
            tvHealthScore.setText("Məlumat az");
            tvHealthScore.setTextColor(Color.parseColor("#F59E0B"));
            tvHealthDetails.setText("Maaş məlumatı daxil edin");
            return;
        }

        double savingsRate = ((monthlySalary - totalExpense) / monthlySalary) * 100;
        double expenseRatio = (totalExpense / monthlySalary) * 100;

        String healthText;
        int healthColor;
        String healthDetails;

        if (savingsRate >= 20) {
            healthText = "🌟 Əla";
            healthColor = Color.parseColor("#10B981");
            healthDetails = String.format("Qənaət: %.1f%% | İnvestisiya vaxtıdır!", savingsRate);
        } else if (savingsRate >= 10) {
            healthText = "👍 Yaxşı";
            healthColor = Color.parseColor("#3B82F6");
            healthDetails = String.format("Qənaət: %.1f%% | Yaxşı vəziyyət", savingsRate);
        } else if (savingsRate >= 0) {
            healthText = "⚠️ Orta";
            healthColor = Color.parseColor("#F59E0B");
            healthDetails = String.format("Qənaət: %.1f%% | Diqqətli olun", savingsRate);
        } else {
            healthText = "🔴 Zəif";
            healthColor = Color.parseColor("#EF4444");
            healthDetails = String.format("Zərər: %.1f%% | Təcili tədbir!", Math.abs(savingsRate));
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

            if (tvTotalIncome != null) {
                tvTotalIncome.setText(String.format(Locale.getDefault(), "₼%.2f", totalIncome));
            }

            if (tvTotalExpense != null) {
                tvTotalExpense.setText(String.format(Locale.getDefault(), "₼%.2f", totalExpense));
            }

            double netProfit = totalIncome - totalExpense;

            if (tvNetProfitValue != null) {
                tvNetProfitValue.setText(String.format(Locale.getDefault(), "₼%.2f", netProfit));

                if (netProfit >= 0) {
                    tvNetProfitValue.setTextColor(Color.parseColor("#10B981"));
                } else {
                    tvNetProfitValue.setTextColor(Color.parseColor("#EF4444"));
                }
            }

            if (tvNetProfit != null) {
                tvNetProfit.setText(netProfit >= 0 ? "Mənfəət" : "Zərər");
            }

            // Aylıq maaş
            if (monthlySalary > 0) {
                if (tvMonthlySalaryText != null) {
                    tvMonthlySalaryText.setVisibility(View.VISIBLE);
                }
                if (tvMonthlySalaryValue != null) {
                    tvMonthlySalaryValue.setVisibility(View.VISIBLE);
                    tvMonthlySalaryValue.setText(String.format(Locale.getDefault(), "₼%.2f", monthlySalary));
                }
            } else {
                if (tvMonthlySalaryText != null) {
                    tvMonthlySalaryText.setVisibility(View.GONE);
                }
                if (tvMonthlySalaryValue != null) {
                    tvMonthlySalaryValue.setVisibility(View.GONE);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}