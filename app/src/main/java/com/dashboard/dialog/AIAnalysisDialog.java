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

import java.util.Locale;
import java.util.Map;

public class AIAnalysisDialog extends AppCompatDialogFragment {

    private String analysisText;
    private String title;
    private Map<String, Double> categoryData;
    private double totalIncome;
    private double totalExpense;
    private double monthlySalary;

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

            // AI Analiz Mətni
            TextView tvAnalysis = view.findViewById(R.id.tvAnalysis);
            if (tvAnalysis != null) {
                tvAnalysis.setText(analysisText);
                tvAnalysis.setMovementMethod(new ScrollingMovementMethod());
            }

            // Kateqoriya məlumatları
            setupCategoryData(view);

            // Ümumi statistika
            setupSummary(view);

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

                // Parent layout-i göstər
                View monthlySalaryLayout = view.findViewById(R.id.tvMonthlySalary);
                if (monthlySalaryLayout != null) {
                    monthlySalaryLayout.setVisibility(View.VISIBLE);
                }
            } else {
                if (tvMonthlySalaryText != null) {
                    tvMonthlySalaryText.setVisibility(View.GONE);
                }
                if (tvMonthlySalaryValue != null) {
                    tvMonthlySalaryValue.setVisibility(View.GONE);
                }
                View monthlySalaryLayout = view.findViewById(R.id.tvMonthlySalary);
                if (monthlySalaryLayout != null) {
                    monthlySalaryLayout.setVisibility(View.GONE);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Dialog ölçüsünü tənzimlə
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}