package com.DiscountMarket.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.DiscountMarket.model.DiscountProduct;
import com.smart_ai_sales.R;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DiscountAdapter extends RecyclerView.Adapter<DiscountAdapter.ViewHolder> {

    private List<DiscountProduct> products = new ArrayList<>();
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("az", "AZ"));

    public void setProducts(List<DiscountProduct> products) {
        this.products = products;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_discount_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DiscountProduct product = products.get(position);

        // Mağaza adı
        holder.tvStore.setText(product.getStoreName());

        // Məhsul adı
        holder.tvName.setText(product.getProductName());

        // Endirimli qiymət
        if (product.getDiscountPrice() > 0) {
            holder.tvDiscountPrice.setText(String.format(Locale.US, "%.2f ₼", product.getDiscountPrice()));
        }

        // Köhnə qiymət (üstündən xətt çəkilmiş)
        if (product.getOriginalPrice() > product.getDiscountPrice() && product.getOriginalPrice() > 0) {
            holder.tvOriginalPrice.setVisibility(View.VISIBLE);
            holder.tvOriginalPrice.setText(String.format(Locale.US, "%.2f ₼", product.getOriginalPrice()));
            holder.tvOriginalPrice.setPaintFlags(holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.tvOriginalPrice.setVisibility(View.GONE);
        }

        // Endirim faizi
        if (product.getDiscountPercent() > 0) {
            holder.tvDiscountBadge.setVisibility(View.VISIBLE);
            holder.tvDiscountBadge.setText(String.format("-%d%%", (int) product.getDiscountPercent()));
        } else {
            holder.tvDiscountBadge.setVisibility(View.GONE);
        }

        // Ən ucuz badge
        if (product.isBestPrice()) {
            holder.tvBestPrice.setVisibility(View.VISIBLE);
            holder.tvBestPrice.setText("⭐ Ən ucuz");
        } else {
            holder.tvBestPrice.setVisibility(View.GONE);
        }

        // Şəkil yükləmə (Glide ilə)
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            holder.progressImage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_product_placeholder)
                            .error(R.drawable.ic_product_error)
                            .transform(new RoundedCorners(8)))
                    .into(holder.ivProduct);
            holder.progressImage.setVisibility(View.GONE);
        } else {
            holder.ivProduct.setImageResource(R.drawable.ic_product_placeholder);
            holder.progressImage.setVisibility(View.GONE);
        }

        // onBindViewHolder metodunda şəkil yükləmə hissəsi:
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_product_placeholder)
                    .error(R.drawable.ic_product_error)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.ivProduct);
        } else {
            holder.ivProduct.setImageResource(R.drawable.ic_product_placeholder);
        }

        // Məhsulun vahidi (KG, AD, ML)
        if (product.getUnit() != null && !product.getUnit().isEmpty()) {
            holder.tvUnit.setVisibility(View.VISIBLE);
            holder.tvUnit.setText(product.getUnit());
        } else {
            holder.tvUnit.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (product.getProductUrl() != null && !product.getProductUrl().isEmpty()) {
                // Browser ilə aç
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(product.getProductUrl()));
                holder.itemView.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }



    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStore, tvName, tvDiscountPrice, tvOriginalPrice, tvDiscountBadge, tvBestPrice, tvUnit;
        ImageView ivProduct;
        ProgressBar progressImage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStore = itemView.findViewById(R.id.tvStore);
            tvName = itemView.findViewById(R.id.tvName);
            tvDiscountPrice = itemView.findViewById(R.id.tvDiscountPrice);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            tvDiscountBadge = itemView.findViewById(R.id.tvDiscountBadge);
            tvBestPrice = itemView.findViewById(R.id.tvBestPrice);
            tvUnit = itemView.findViewById(R.id.tvUnit);
            ivProduct = itemView.findViewById(R.id.ivProduct);
            progressImage = itemView.findViewById(R.id.progressImage);
        }
    }
}