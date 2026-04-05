package com.DiscountMarket.adapter;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.DiscountMarket.model.DiscountProduct;
import com.DiscountMarket.service.StoreDiscountFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscountPagerAdapter extends FragmentStateAdapter {

    private List<String> storeNames = new ArrayList<>();
    private Map<String, List<DiscountProduct>> storeProducts = new HashMap<>();
    private String searchQuery = "";
    private List<StoreDiscountFragment> fragments = new ArrayList<>();

    public DiscountPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void setStoreData(List<String> names, Map<String, List<DiscountProduct>> products) {
        this.storeNames = names;
        this.storeProducts = products;
        fragments.clear();
        notifyDataSetChanged();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query;
        for (StoreDiscountFragment fragment : fragments) {
            if (fragment != null) {
                fragment.setSearchQuery(query);
            }
        }
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        String storeName = storeNames.get(position);
        List<DiscountProduct> products = storeProducts.getOrDefault(storeName, new ArrayList<>());
        StoreDiscountFragment fragment = StoreDiscountFragment.newInstance(storeName, products);
        fragments.add(fragment);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return storeNames.size();
    }
}
