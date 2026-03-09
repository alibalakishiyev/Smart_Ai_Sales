package com.data;


import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ProductStorage {
    private static final String PREF_NAME = "product_prefs";
    private static final String KEY_PRODUCTS = "saved_products";
    private SharedPreferences preferences;
    private Gson gson;

    public ProductStorage(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveProducts(List<ProductItem> products) {
        String json = gson.toJson(products);
        preferences.edit().putString(KEY_PRODUCTS, json).apply();
    }

    public List<ProductItem> loadProducts() {
        String json = preferences.getString(KEY_PRODUCTS, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<ProductItem>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void addProduct(ProductItem product) {
        List<ProductItem> products = loadProducts();
        products.add(product);
        saveProducts(products);
    }

    public void removeProduct(int position) {
        List<ProductItem> products = loadProducts();
        if (position >= 0 && position < products.size()) {
            products.remove(position);
            saveProducts(products);
        }
    }
}