package com.DiscountMarket.service;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.DiscountMarket.adapter.DiscountAdapter;
import com.DiscountMarket.model.DiscountProduct;
import com.smart_ai_sales.R;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StoreDiscountFragment extends Fragment {

    private static final String ARG_STORE_NAME = "store_name";

    private String storeName;
    private RecyclerView recyclerView;
    private LinearLayout llEmpty;
    private DiscountAdapter adapter;

    private List<DiscountProduct> allProducts = new ArrayList<>();
    private String searchQuery = "";

    // ─────────────────────────────────────────────
    public static StoreDiscountFragment newInstance(String storeName,
                                                    List<DiscountProduct> products) {
        StoreDiscountFragment f = new StoreDiscountFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STORE_NAME, storeName);
        f.setArguments(args);
        f.allProducts = new ArrayList<>(products);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            storeName = getArguments().getString(ARG_STORE_NAME);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_store_discounts, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        llEmpty      = view.findViewById(R.id.llEmpty);

        adapter = new DiscountAdapter();

        // 2-column grid for a richer visual
        GridLayoutManager layoutManager =
                new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);

        filterProducts();
        return view;
    }

    public void updateProducts(List<DiscountProduct> products) {
        this.allProducts = new ArrayList<>(products);
        filterProducts();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query == null ? "" : query;
        filterProducts();
    }

    private void filterProducts() {
        if (adapter == null) return;

        List<DiscountProduct> filtered = allProducts.stream()
                .filter(p -> p.getStoreName().equals(storeName))
                .filter(p -> searchQuery.isEmpty() ||
                        p.getProductName().toLowerCase()
                                .contains(searchQuery.toLowerCase()))
                .collect(Collectors.toList());

        adapter.setProducts(filtered);

        if (llEmpty != null) {
            llEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}