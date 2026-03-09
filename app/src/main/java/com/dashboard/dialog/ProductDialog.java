package com.dashboard.dialog;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dashboard.ProductAnalysis;  // DÜZƏLDİ: DashboardActivity.ProductAnalysis əvəzinə birbaşa ProductAnalysis
import com.smart_ai_sales.R;

import java.util.List;
import java.util.Locale;

public class ProductDialog extends AppCompatDialogFragment {

    // DÜZƏLDİ: List<ProductAnalysis> olaraq dəyişdirildi
    private List<ProductAnalysis> products;
    private String title;

    // DÜZƏLDİ: Constructor parametri düzəldildi
    public ProductDialog(List<ProductAnalysis> products, String title) {
        this.products = products;
        this.title = title;
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
        View view = inflater.inflate(R.layout.dialog_products, container, false);

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

        // Ümumi statistik məlumat
        TextView tvStats = view.findViewById(R.id.tvStats);
        if (tvStats != null) {
            tvStats.setText("Ümumi " + products.size() + " məhsul");
        }

        // Bağlama düyməsi
        ImageView btnClose = view.findViewById(R.id.btnClose);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        // Bağlama düyməsi 2
        TextView btnClose2 = view.findViewById(R.id.btnClose2);
        if (btnClose2 != null) {
            btnClose2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }

        // RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recyclerProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        ProductAdapter adapter = new ProductAdapter(products);
        recyclerView.setAdapter(adapter);

        return view;
    }

    // Adapter class
    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

        // DÜZƏLDİ: Tip düzəldildi
        private List<ProductAnalysis> productList;

        // DÜZƏLDİ: Constructor parametri düzəldildi
        ProductAdapter(List<ProductAnalysis> productList) {
            this.productList = productList;
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product_detail, parent, false);
            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            // DÜZƏLDİ: Tip düzəldildi
            ProductAnalysis product = productList.get(position);

            // Sıra nömrəsi
            holder.tvRank.setText(String.valueOf(position + 1));

            // Məhsul adı - Getter istifadə edirik
            holder.tvProductName.setText(product.getProductName());

            // Kateqoriya - Getter istifadə edirik
            holder.tvCategory.setText(product.getCategory() != null ? product.getCategory() : "Kateqoriyasız");

            // Ümumi məbləğ - Getter istifadə edirik
            holder.tvTotalAmount.setText(String.format(Locale.getDefault(), "₼%.2f", product.getTotalAmount()));

            // Satış sayı - Getter istifadə edirik
            holder.tvSalesCount.setText(product.getTransactionCount() + " satış");

            // Orta qiymət - Getter istifadə edirik
            holder.tvAvgPrice.setText(String.format(Locale.getDefault(), "Orta: ₼%.2f", product.getAvgAmount()));

            // Trend - Getter istifadə edirik
            String trend = product.getTrend();
            holder.tvTrend.setText(trend);

            // Trend rəngi
            int trendColor;
            if (trend.contains("Sürətli artım")) {
                trendColor = Color.parseColor("#10B981");
            } else if (trend.contains("Yavaş artım")) {
                trendColor = Color.parseColor("#34D399");
            } else if (trend.contains("Stabil")) {
                trendColor = Color.parseColor("#F59E0B");
            } else if (trend.contains("Yavaş eniş")) {
                trendColor = Color.parseColor("#F97316");
            } else {
                trendColor = Color.parseColor("#EF4444");
            }
            holder.tvTrend.setTextColor(trendColor);

            // Proqnoz - Getter istifadə edirik
            holder.tvPrediction.setText(String.format(Locale.getDefault(), "Proqnoz: ₼%.2f", product.getPredictedNextAmount()));
        }

        @Override
        public int getItemCount() {
            return productList.size();
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvProductName, tvCategory, tvTotalAmount, tvSalesCount, tvAvgPrice, tvTrend, tvPrediction;

            ProductViewHolder(View itemView) {
                super(itemView);
                tvRank = itemView.findViewById(R.id.tvRank);
                tvProductName = itemView.findViewById(R.id.tvProductName);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
                tvSalesCount = itemView.findViewById(R.id.tvSalesCount);
                tvAvgPrice = itemView.findViewById(R.id.tvAvgPrice);
                tvTrend = itemView.findViewById(R.id.tvTrend);
                tvPrediction = itemView.findViewById(R.id.tvPrediction);
            }
        }
    }
}