package com.DiscountMarket;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
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
    private SwipeRefreshLayout swipeRefreshLayout;

    private String[] stores = {"BazarStore", "Araz Market", "Bravo", "Megastore"};
    private int currentStoreIndex = 0;
    private Map<Integer, List<DiscountProduct>> cache = new HashMap<>();
    private Map<Integer, Map<String, List<DiscountProduct>>> categoryCache = new HashMap<>();
    private boolean isLoading = false;
    private String currentCategory = "Bütün Məhsullar";

    // Force refresh flag - swipe ilə gələndə cache istifadə etmə
    private boolean forceRefresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discount);

        initViews();
        setupToolbar();
        setupSearch();
        setupTabLayout();
        setupCategories();
        setupSwipeToRefresh();

        DiscountScraperService.loadCache(this);

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
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

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
                    forceRefresh = false; // Tab dəyişəndə force refresh-i söndür
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    loadCurrentStore();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupCategories() {
        String[][] categories = {
                {"Bütün Məhsullar", "Deterjan Təmizlik", "Ət Toyuq Balıq", "Təməl Qida", "Uşaq Məhsulları", "Atışdırmalıq", "Şəxsi Qulluq", "Dondurulmuş Qida", "Süd Səhər Yeməyi", "İçkilər", "Un Məmulatları", "Ev & Bağ"},
                {"Bütün Məhsullar", "Meyvə Tərəvəz", "Ət Məhsulları", "Süd Məhsulları", "İçkilər", "Qida Məhsulları", "Təmizlik", "Kosmetika", "Hazır Yemək", "Şərbətlər", "Soslar", "Digər"},
                {"Bütün Məhsullar", "Endirimlər"},
                {"Bütün Məhsullar", "Endirimlər"}
        };

        chipGroupCategories.removeAllViews();
        String[] cats = categories[currentStoreIndex];
        if (cats == null) cats = new String[]{"Bütün Məhsullar"};

        for (String cat : cats) {
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

    // ==================== SWIPE-TO-REFRESH ====================

    private void setupSwipeToRefresh() {
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_green_dark);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d("DiscountActivity", "🔄 Swipe-to-refresh: əl ilə yeniləmə başladı");

            // 1. Force refresh flagini true et - cache istifadə etmə
            forceRefresh = true;

            // 2. Cari mağazanın cache-ni təmizlə
            cache.remove(currentStoreIndex);
            categoryCache.remove(currentStoreIndex);

            // 3. Disk cache-ni də təmizlə (DiscountScraperService-dəki cache)
            DiscountScraperService.clearCache();

            // 4. Cari məhsulları təmizlə
            currentProducts.clear();
            adapter.setProducts(new ArrayList<>());
            updateProductCount(0);

            // 5. Kateqoriyaları sıfırla
            currentCategory = "Bütün Məhsullar";
            setupCategories();

            // 6. Yenidən yüklə
            isLoading = false;
            loadCurrentStore();
        });
    }

    // ==================== LOAD METHODS ====================

    private void loadCurrentStore() {
        // Əgər forceRefresh false-dursa və cache varsa, cache-dən göstər
        if (!forceRefresh && cache.containsKey(currentStoreIndex) && !isLoading && !swipeRefreshLayout.isRefreshing()) {
            currentProducts = cache.get(currentStoreIndex);
            setupCategories();
            updateCategoryCache();
            filterByCategory();
            updateProductCount(currentProducts.size());
            swipeRefreshLayout.setRefreshing(false);
            Log.d("DiscountActivity", "📦 Cache-dən yükləndi: " + currentProducts.size() + " məhsul");
            return;
        }

        if (isLoading) return;

        // Yükləmə başlamazdan əvvəl köhnə məlumatları təmizlə
        currentProducts = new ArrayList<>();
        adapter.setProducts(new ArrayList<>());
        updateProductCount(0);
        currentCategory = "Bütün Məhsullar";
        setupCategories();

        isLoading = true;
        showLoading(stores[currentStoreIndex] + " yüklənir...");

        if (!swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
        }

        DiscountScraperService.scrapeStore(currentStoreIndex,
                new DiscountScraperService.StoreScraperCallback() {

                    @Override
                    public void onSuccess(List<DiscountProduct> products, int totalCount) {
                        runOnUiThread(() -> {
                            cache.put(currentStoreIndex, products);
                            currentProducts = products;
                            setupCategories();
                            updateCategoryCache();
                            filterByCategory();
                            isLoading = false;
                            forceRefresh = false; // Yükləmə bitdi, force refresh-i söndür
                            hideLoading();
                            updateProductCount(totalCount);
                            swipeRefreshLayout.setRefreshing(false);

                            String message = stores[currentStoreIndex] + ": " + totalCount + " məhsul";
                            Toast.makeText(DiscountActivity.this, message, Toast.LENGTH_SHORT).show();
                            Log.d("DiscountActivity", "✅ " + message);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            currentProducts = new ArrayList<>();
                            adapter.setProducts(new ArrayList<>());
                            updateProductCount(0);
                            isLoading = false;
                            forceRefresh = false;
                            hideLoading();
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(DiscountActivity.this, error, Toast.LENGTH_LONG).show();
                            Log.e("DiscountActivity", "❌ " + error);
                        });
                    }

                    @Override
                    public void onProgress(String message) {
                        runOnUiThread(() -> {
                            if (tvLoadingStatus != null) tvLoadingStatus.setText(message);
                            Log.d("DiscountActivity", "📡 " + message);
                        });
                    }
                }, this);
    }

    private void updateCategoryCache() {
        Map<String, List<DiscountProduct>> catMap = new HashMap<>();

        for (DiscountProduct p : currentProducts) {
            String cat = p.getCategory();
            if (cat == null || cat.isEmpty()) {
                cat = p.getStoreName();
            }
            if (!catMap.containsKey(cat)) {
                catMap.put(cat, new ArrayList<>());
            }
            catMap.get(cat).add(p);
        }
        categoryCache.put(currentStoreIndex, catMap);
    }

    private void filterByCategory() {
        if (currentProducts == null || currentProducts.isEmpty()) {
            adapter.setProducts(new ArrayList<>());
            updateProductCount(0);
            return;
        }

        List<DiscountProduct> filtered;

        if (currentCategory.equals("Bütün Məhsullar")) {
            filtered = new ArrayList<>(currentProducts);
        } else {
            filtered = new ArrayList<>();
            for (DiscountProduct p : currentProducts) {
                String cat = p.getCategory();
                if (cat == null || cat.isEmpty()) {
                    cat = p.getStoreName();
                }
                if (cat != null && cat.equals(currentCategory)) {
                    filtered.add(p);
                }
            }
        }

        String searchQuery = etSearch.getText().toString();
        if (!searchQuery.isEmpty()) {
            List<DiscountProduct> searchFiltered = new ArrayList<>();
            for (DiscountProduct p : filtered) {
                if (p.getProductName() != null &&
                        p.getProductName().toLowerCase().contains(searchQuery.toLowerCase())) {
                    searchFiltered.add(p);
                }
            }
            filtered = searchFiltered;
        }

        adapter.setProducts(filtered);
        updateProductCount(filtered.size());
        Log.d("DiscountActivity", "Filtered: " + filtered.size() + " products for: " + currentCategory);
    }

    private void filterBySearch(String query) {
        if (currentProducts == null) return;

        List<DiscountProduct> categoryFiltered;
        if (currentCategory.equals("Bütün Məhsullar")) {
            categoryFiltered = new ArrayList<>(currentProducts);
        } else {
            categoryFiltered = new ArrayList<>();
            for (DiscountProduct p : currentProducts) {
                String cat = p.getCategory();
                if (cat == null) cat = p.getStoreName();
                if (cat != null && cat.equals(currentCategory)) {
                    categoryFiltered.add(p);
                }
            }
        }

        if (query.isEmpty()) {
            adapter.setProducts(categoryFiltered);
            updateProductCount(categoryFiltered.size());
        } else {
            List<DiscountProduct> filtered = new ArrayList<>();
            for (DiscountProduct p : categoryFiltered) {
                if (p.getProductName() != null &&
                        p.getProductName().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(p);
                }
            }
            adapter.setProducts(filtered);
            updateProductCount(filtered.size());
        }
    }

    private void updateProductCount(int count) {
        if (tvProductCount != null) tvProductCount.setText(count + " məhsul");
    }

    private void showLoading(String message) {
        if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
        }
        if (tvLoadingStatus != null) tvLoadingStatus.setText(message);
    }

    private void hideLoading() {
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
    }
}