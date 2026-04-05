package com.DiscountMarket;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.DiscountMarket.adapter.DiscountAdapter;
import com.DiscountMarket.model.DiscountProduct;
import com.DiscountMarket.service.DiscountScraperService;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.smart_ai_sales.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscountActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DiscountAdapter adapter;
    private List<DiscountProduct> currentProducts = new ArrayList<>();
    private FrameLayout loadingOverlay;
    private TextView tvLoadingStatus, tvProductCount;
    private TextInputEditText etSearch;
    private TabLayout tabLayout;
    private ChipGroup chipGroupCategories;
    private View categoryScrollView;

    private String[] stores = {"BazarStore", "Araz Market", "Bravo", "Megastore"};
    private int currentStoreIndex = 0;
    private Map<Integer, List<DiscountProduct>> cache = new HashMap<>();
    private Map<Integer, Map<String, List<DiscountProduct>>> categoryCache = new HashMap<>();
    private boolean isLoading = false;
    private String currentCategory = "Bütün Məhsullar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discount);

        initViews();
        setupToolbar();
        setupSearch();
        setupTabLayout();
        setupCategories();

        loadCurrentStore();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus);
        tvProductCount = findViewById(R.id.tvProductCount);
        etSearch = findViewById(R.id.etSearch);
        tabLayout = findViewById(R.id.tabLayout);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        categoryScrollView = findViewById(R.id.categoryScrollView);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new DiscountAdapter();
        recyclerView.setAdapter(adapter);

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds != null && !checkedIds.isEmpty()) {
                int chipId = checkedIds.get(0);
                Chip chip = group.findViewById(chipId);
                if (chip != null) {
                    currentCategory = chip.getText().toString();
                    filterByCategory();
                }
            }
        });
    }

    private void setupToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Endirimlər");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterBySearch(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupTabLayout() {
        for (String store : stores) tabLayout.addTab(tabLayout.newTab().setText(store));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() != currentStoreIndex) {
                    currentStoreIndex = tab.getPosition();
                    loadCurrentStore();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupCategories() {
        // Kateqoriyalar
        String[][] categories = {
                {"Bütün Məhsullar", "Deterjan Təmizlik", "Ət Toyuq Balıq", "Təməl Qida", "Uşaq Məhsulları", "Atışdırmalıq", "Şəxsi Qulluq", "Dondurulmuş Qida", "Süd Səhər Yeməyi", "İçkilər", "Un Məmulatları", "Ev & Bağ"},
                {"Bütün Məhsullar", "Qida", "Təmizlik", "Kosmetika"},
                {"Bütün Məhsullar", "Endirimlər"}

        };

        chipGroupCategories.removeAllViews();
        for (String cat : categories[currentStoreIndex]) {
            Chip chip = new Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setTextColor(getResources().getColor(android.R.color.white));
            chip.setChipStrokeColorResource(android.R.color.darker_gray);
            chip.setChipStrokeWidth(1f);
            if (cat.equals("Bütün Məhsullar")) chip.setChecked(true);
            chipGroupCategories.addView(chip);
        }
        categoryScrollView.setVisibility(View.VISIBLE);
    }

    private void loadCurrentStore() {
        if (cache.containsKey(currentStoreIndex)) {
            currentProducts = cache.get(currentStoreIndex);
            updateCategoryCache();
            filterByCategory();
            updateProductCount(currentProducts.size());
            return;
        }
        if (isLoading) return;

        isLoading = true;
        showLoading(stores[currentStoreIndex] + " yüklənir...");

        DiscountScraperService.scrapeStore(currentStoreIndex, new DiscountScraperService.StoreScraperCallback() {
            @Override
            public void onSuccess(List<DiscountProduct> products, int totalCount) {
                runOnUiThread(() -> {
                    cache.put(currentStoreIndex, products);
                    currentProducts = products;
                    updateCategoryCache();
                    filterByCategory();
                    isLoading = false;
                    hideLoading();
                    updateProductCount(totalCount);
                    Toast.makeText(DiscountActivity.this, stores[currentStoreIndex] + ": " + totalCount + " məhsul", Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> { isLoading = false; hideLoading(); Toast.makeText(DiscountActivity.this, error, Toast.LENGTH_LONG).show(); });
            }
            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> { if (tvLoadingStatus != null) tvLoadingStatus.setText(message); });
            }
        });
    }

    private void updateCategoryCache() {
        Map<String, List<DiscountProduct>> catMap = new HashMap<>();
        for (DiscountProduct p : currentProducts) {
            String cat = p.getCategory() != null ? p.getCategory() : "Digər";
            if (!catMap.containsKey(cat)) catMap.put(cat, new ArrayList<>());
            catMap.get(cat).add(p);
        }
        categoryCache.put(currentStoreIndex, catMap);
    }

    private void filterByCategory() {
        if (currentProducts == null) return;
        List<DiscountProduct> filtered;
        if (currentCategory.equals("Bütün Məhsullar")) {
            filtered = new ArrayList<>(currentProducts);
        } else {
            filtered = new ArrayList<>();
            for (DiscountProduct p : currentProducts) {
                String cat = p.getCategory() != null ? p.getCategory() : "Digər";
                if (cat.equals(currentCategory)) filtered.add(p);
            }
        }
        String searchQuery = etSearch.getText().toString();
        if (!searchQuery.isEmpty()) {
            List<DiscountProduct> searchFiltered = new ArrayList<>();
            for (DiscountProduct p : filtered) {
                if (p.getProductName().toLowerCase().contains(searchQuery.toLowerCase())) searchFiltered.add(p);
            }
            filtered = searchFiltered;
        }
        adapter.setProducts(filtered);
        updateProductCount(filtered.size());
    }

    private void filterBySearch(String query) {
        if (currentProducts == null) return;
        List<DiscountProduct> categoryFiltered;
        if (currentCategory.equals("Bütün Məhsullar")) {
            categoryFiltered = new ArrayList<>(currentProducts);
        } else {
            categoryFiltered = new ArrayList<>();
            for (DiscountProduct p : currentProducts) {
                String cat = p.getCategory() != null ? p.getCategory() : "Digər";
                if (cat.equals(currentCategory)) categoryFiltered.add(p);
            }
        }
        if (query.isEmpty()) {
            adapter.setProducts(categoryFiltered);
            updateProductCount(categoryFiltered.size());
        } else {
            List<DiscountProduct> filtered = new ArrayList<>();
            for (DiscountProduct p : categoryFiltered) {
                if (p.getProductName().toLowerCase().contains(query.toLowerCase())) filtered.add(p);
            }
            adapter.setProducts(filtered);
            updateProductCount(filtered.size());
        }
    }

    private void updateProductCount(int count) {
        if (tvProductCount != null) tvProductCount.setText(count + " məhsul");
    }

    private void showLoading(String message) {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
        if (tvLoadingStatus != null) tvLoadingStatus.setText(message);
    }

    private void hideLoading() {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
    }
}