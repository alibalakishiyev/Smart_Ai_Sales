package com.data.list;



import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.data.ProductItem;
import com.smart_ai_sales.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ViewHolder> {

    private List<ProductItem> products;
    private Context context;
    private NumberFormat currencyFormat;
    private OnProductSelectedListener listener;

    public interface OnProductSelectedListener {
        void onProductSelected(ProductItem product, boolean isSelected);
        void onSelectionChanged(int selectedCount, double totalAmount);
    }

    public ProductListAdapter(Context context, OnProductSelectedListener listener) {
        this.context = context;
        this.products = new ArrayList<>();
        this.listener = listener;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("az", "AZ"));
        currencyFormat.setMaximumFractionDigits(2);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductItem product = products.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void setProducts(List<ProductItem> products) {
        this.products.clear();
        this.products.addAll(products);
        notifyDataSetChanged();
    }

    public void addProduct(ProductItem product) {
        this.products.add(product);
        notifyItemInserted(products.size() - 1);
    }

    public void clearProducts() {
        this.products.clear();
        notifyDataSetChanged();
    }

    public List<ProductItem> getSelectedProducts() {
        List<ProductItem> selected = new ArrayList<>();
        for (ProductItem product : products) {
            if (product.isSelected()) {
                selected.add(product);
            }
        }
        return selected;
    }

    public double getSelectedTotalAmount() {
        double total = 0;
        for (ProductItem product : products) {
            if (product.isSelected()) {
                if (product.getKg() > 0) {
                    total += product.getKg() * product.getPrice();
                } else if (product.getLiter() > 0) {
                    total += product.getLiter() * product.getPrice();
                } else {
                    total += product.getPrice() * product.getQuantity();
                }
            }
        }
        return total;
    }

    public int getSelectedCount() {
        int count = 0;
        for (ProductItem product : products) {
            if (product.isSelected()) {
                count++;
            }
        }
        return count;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvProductName, tvProductPrice, tvProductQuantity, tvProductTotal;
        private CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvProductQuantity = itemView.findViewById(R.id.tvProductQuantity);
            tvProductTotal = itemView.findViewById(R.id.tvProductTotal);
            checkBox = itemView.findViewById(R.id.checkBox);
        }

        void bind(final ProductItem product) {
            tvProductName.setText(product.getName());

            if (product.getKg() > 0) {
                tvProductQuantity.setText(String.format("%.2f kq", product.getKg()));
            } else if (product.getLiter() > 0) {
                tvProductQuantity.setText(String.format("%.2f L", product.getLiter()));
            } else {
                tvProductQuantity.setText(String.format("%d ədəd", product.getQuantity()));
            }

            tvProductPrice.setText(currencyFormat.format(product.getPrice()));

            double total;
            if (product.getKg() > 0) {
                total = product.getKg() * product.getPrice();
            } else if (product.getLiter() > 0) {
                total = product.getLiter() * product.getPrice();
            } else {
                total = product.getPrice() * product.getQuantity();
            }
            tvProductTotal.setText(currencyFormat.format(total));

            checkBox.setChecked(product.isSelected());

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    product.setSelected(isChecked);
                    if (listener != null) {
                        listener.onProductSelected(product, isChecked);
                        listener.onSelectionChanged(getSelectedCount(), getSelectedTotalAmount());
                    }
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkBox.setChecked(!checkBox.isChecked());
                }
            });
        }
    }
}