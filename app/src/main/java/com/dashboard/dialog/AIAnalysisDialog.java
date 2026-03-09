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
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dashboard.ProductAnalysis;
import com.smart_ai_sales.R;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AIAnalysisDialog extends AppCompatDialogFragment {

    private String analysisText;
    private String title;
    private List<ProductAnalysis> topProducts;
    private Map<String, Double> categoryData;
    private double totalIncome;
    private double totalExpense;

    public AIAnalysisDialog(String analysisText, String title, List<ProductAnalysis> topProducts,
                            Map<String, Double> categoryData, double totalIncome, double totalExpense) {
        this.analysisText = analysisText;
        this.title = title;
        this.topProducts = topProducts;
        this.categoryData = categoryData;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_ai_analysis, container, false);

        // Dialog görünüşünü tənzimlə
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Başlıq
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        tvTitle.setText(title);

        // Bağlama düyməsi
        ImageView btnClose = view.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        // AI Analiz Text
        TextView tvAnalysis = view.findViewById(R.id.tvAnalysis);
        tvAnalysis.setText(analysisText);
        tvAnalysis.setMovementMethod(new ScrollingMovementMethod());

        // Ən yaxşı məhsullar
        setupTopProducts(view);

        // Kateqoriya məlumatları
        setupCategoryData(view);

        // Ümumi statistika
        setupSummary(view);

        // Bağlama düyməsi 2
        TextView btnClose2 = view.findViewById(R.id.btnClose2);
        btnClose2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return view;
    }

    private void setupTopProducts(View view) {
        LinearLayout container = view.findViewById(R.id.topProductsContainer);
        container.removeAllViews();

        if (topProducts == null || topProducts.isEmpty()) {
            TextView emptyView = new TextView(getContext());
            emptyView.setText("Məhsul məlumatı yoxdur");
            emptyView.setTextColor(Color.parseColor("#B3FFFFFF"));
            emptyView.setTextSize(14);
            emptyView.setPadding(16, 16, 16, 16);
            container.addView(emptyView);
            return;
        }

        int count = 0;
        for (ProductAnalysis product : topProducts) {
            if (count >= 5) break;

            View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_analysis_product, container, false);

            TextView tvRank = itemView.findViewById(R.id.tvRank);
            TextView tvProductName = itemView.findViewById(R.id.tvProductName);
            TextView tvAmount = itemView.findViewById(R.id.tvAmount);
            TextView tvSales = itemView.findViewById(R.id.tvSales);
            TextView tvTrend = itemView.findViewById(R.id.tvTrend);

            tvRank.setText(String.valueOf(count + 1));
            tvProductName.setText(product.getProductName());
            tvAmount.setText(String.format(Locale.getDefault(), "₼%.2f", product.getTotalAmount()));
            tvSales.setText(product.getTransactionCount() + " satış");
            tvTrend.setText(product.getTrend());

            // Trend rəngi
            int trendColor;
            if (product.getTrend().contains("Sürətli artım")) {
                trendColor = Color.parseColor("#10B981");
            } else if (product.getTrend().contains("Yavaş artım")) {
                trendColor = Color.parseColor("#34D399");
            } else if (product.getTrend().contains("Stabil")) {
                trendColor = Color.parseColor("#F59E0B");
            } else if (product.getTrend().contains("Yavaş eniş")) {
                trendColor = Color.parseColor("#F97316");
            } else {
                trendColor = Color.parseColor("#EF4444");
            }
            tvTrend.setTextColor(trendColor);

            container.addView(itemView);
            count++;
        }
    }

    private void setupCategoryData(View view) {
        LinearLayout container = view.findViewById(R.id.categoryContainer);
        container.removeAllViews();

        if (categoryData == null || categoryData.isEmpty()) {
            TextView emptyView = new TextView(getContext());
            emptyView.setText("Kateqoriya məlumatı yoxdur");
            emptyView.setTextColor(Color.parseColor("#B3FFFFFF"));
            emptyView.setTextSize(14);
            emptyView.setPadding(16, 16, 16, 16);
            container.addView(emptyView);
            return;
        }

        double total = 0;
        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            total += entry.getValue();
        }

        int count = 0;
        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            if (count >= 5) break;

            View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_analysis_category, container, false);

            TextView tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            TextView tvCategoryAmount = itemView.findViewById(R.id.tvCategoryAmount);
            TextView tvCategoryPercent = itemView.findViewById(R.id.tvCategoryPercent);

            String category = entry.getKey();
            double amount = entry.getValue();
            int percent = total > 0 ? (int) ((amount / total) * 100) : 0;

            tvCategoryName.setText(category);
            tvCategoryAmount.setText(String.format(Locale.getDefault(), "₼%.2f", amount));
            tvCategoryPercent.setText(percent + "%");

            container.addView(itemView);
            count++;
        }
    }

    private void setupSummary(View view) {
        TextView tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        TextView tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        TextView tvNetProfit = view.findViewById(R.id.tvNetProfit);
        TextView tvNetProfitValue = view.findViewById(R.id.tvNetProfitValue);

        tvTotalIncome.setText(String.format(Locale.getDefault(), "₼%.2f", totalIncome));
        tvTotalExpense.setText(String.format(Locale.getDefault(), "₼%.2f", totalExpense));

        double netProfit = totalIncome - totalExpense;
        tvNetProfitValue.setText(String.format(Locale.getDefault(), "₼%.2f", netProfit));

        if (netProfit >= 0) {
            tvNetProfitValue.setTextColor(Color.parseColor("#10B981"));
        } else {
            tvNetProfitValue.setTextColor(Color.parseColor("#EF4444"));
        }

        tvNetProfit.setText(netProfit >= 0 ? "Mənfəət" : "Zərər");
    }
}