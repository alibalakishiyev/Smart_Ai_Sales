package com.DiscountMarket.service;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.DiscountMarket.store.BazarCategory;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.smart_ai_sales.R;

import java.util.ArrayList;
import java.util.List;

public class CategorySelectionDialog extends BottomSheetDialogFragment {

    private CategorySelectionListener listener;
    private List<BazarCategory> categories = new ArrayList<>();

    public interface CategorySelectionListener {
        void onCategorySelected(BazarCategory category);
    }

    public static CategorySelectionDialog newInstance() {
        return new CategorySelectionDialog();
    }

    public void setCategorySelectionListener(CategorySelectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof CategorySelectionListener) {
            listener = (CategorySelectionListener) getParentFragment();
        } else if (context instanceof CategorySelectionListener) {
            listener = (CategorySelectionListener) context;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setContentView(R.layout.dialog_category_selection);
        return dialog;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_category_selection, container, false);

        // Kateqoriyaları əl ilə yarat
        createCategories();

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new CategoryAdapter(categories, category -> {
            if (listener != null) {
                listener.onCategorySelected(category);
            }
            dismiss();
        }));

        return view;
    }

    private void createCategories() {
        categories.clear();
        categories.add(new BazarCategory("all", "Bütün Məhsullar", ""));
        categories.add(new BazarCategory("deterjan", "Deterjan & Təmizlik", ""));
        categories.add(new BazarCategory("et-toyuq", "Ət, Toyuq & Balıq", ""));
        categories.add(new BazarCategory("temel-qida", "Təməl Qida Məhsulları", ""));
        categories.add(new BazarCategory("usaq", "Uşaq Məhsulları", ""));
        categories.add(new BazarCategory("atistirmaliq", "Atışdırmalıq Məhsullar", ""));
        categories.add(new BazarCategory("sexsi-qulluq", "Şəxsi Qulluq Məhsulları", ""));
        categories.add(new BazarCategory("dondurulmus", "Dondurulmuş & Hazır Qida", ""));
        categories.add(new BazarCategory("sud-seher", "Süd & Səhər Yeməyi", ""));
        categories.add(new BazarCategory("icki", "İçki Məhsulları", ""));
        categories.add(new BazarCategory("un-unlu", "Un & Unlu Məhsullar", ""));
        categories.add(new BazarCategory("ev-bag", "Ev & Bağ Məhsulları", ""));
    }

    private static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final List<BazarCategory> categories;
        private final OnCategoryClickListener listener;

        interface OnCategoryClickListener {
            void onCategoryClick(BazarCategory category);
        }

        CategoryAdapter(List<BazarCategory> categories, OnCategoryClickListener listener) {
            this.categories = categories;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BazarCategory category = categories.get(position);
            holder.tvCategoryName.setText(category.getName());
            holder.itemView.setOnClickListener(v -> listener.onCategoryClick(category));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCategoryName;
            ViewHolder(View itemView) {
                super(itemView);
                tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            }
        }
    }
}