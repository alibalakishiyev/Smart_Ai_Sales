package com.DiscountMarket.service;

import android.os.AsyncTask;
import android.util.Log;
import com.DiscountMarket.model.DiscountProduct;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscountScraperService {

    private static final String TAG = "DiscountScraper";
    private static final int TIMEOUT = 15000;
    private static final String BAZARSTORE_API = "https://bazarstore.az/collections/endirimli-mehsullar/products.json?limit=250";
    private static final String ARAZ_URL = "https://www.arazmarket.az/en";
    private static final String BRAVO_URL = "https://bravo.az/az/endiriml%C9%99r";
    private static final String MEGASTORE_URL = "https://megastore.az/az/endiriml%C9%99r";

    public interface StoreScraperCallback {
        void onSuccess(List<DiscountProduct> products, int totalCount);
        void onError(String error);
        void onProgress(String message);
    }

    public static void scrapeStore(int storeIndex, StoreScraperCallback callback) {
        new StoreScrapeTask(callback, storeIndex).execute();
    }

    private static class StoreScrapeTask extends AsyncTask<Void, String, List<DiscountProduct>> {
        private final StoreScraperCallback callback;
        private final int storeIndex;
        private int totalCount = 0;

        StoreScrapeTask(StoreScraperCallback callback, int storeIndex) {
            this.callback = callback;
            this.storeIndex = storeIndex;
        }

        @Override
        protected List<DiscountProduct> doInBackground(Void... voids) {
            List<DiscountProduct> products = new ArrayList<>();
            try {
                switch (storeIndex) {
                    case 0:
                        publishProgress("BazarStore API-dən məlumatlar yüklənir...");
                        products = scrapeBazarStoreStatic();
                        break;
                    case 1:
                        publishProgress("Araz Market yüklənir...");
                        products = scrapeArazMarketStatic();
                        break;
                    case 2:
                        publishProgress("Bravo yüklənir...");
                        products = scrapeBravoMarketStatic();
                        break;
                    case 3:
                        publishProgress("Megastore yüklənir...");
                        products = scrapeMegastoreStatic();
                        break;
                }
                totalCount = products.size();
            } catch (Exception e) {
                Log.e(TAG, "Xəta: " + e.getMessage());
            }
            return products;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (callback != null) callback.onProgress(values[0]);
        }

        @Override
        protected void onPostExecute(List<DiscountProduct> products) {
            if (callback != null) {
                if (products.isEmpty()) {
                    callback.onError("Məhsul tapılmadı");
                } else {
                    callback.onSuccess(products, totalCount);
                }
            }
        }
    }

    // ==================== BAZARSTORE (JSON API) ====================
    private static List<DiscountProduct> scrapeBazarStoreStatic() throws Exception {
        List<DiscountProduct> products = new ArrayList<>();
        String response = fetchUrlStatic(BAZARSTORE_API);
        if (response == null) return products;

        JSONObject json = new JSONObject(response);
        JSONArray productsArray = json.getJSONArray("products");

        for (int i = 0; i < productsArray.length(); i++) {
            JSONObject product = productsArray.getJSONObject(i);
            DiscountProduct p = new DiscountProduct();
            p.setId(product.optString("id"));
            p.setStoreName("BazarStore");
            p.setStoreLogoRes("bazarstore");
            p.setProductName(product.optString("title"));

            // Kateqoriyanı tags-dan tap (azərbaycanca)
            JSONArray tags = product.optJSONArray("tags");
            String category = "Digər";
            if (tags != null) {
                for (int j = 0; j < tags.length(); j++) {
                    String tag = tags.getString(j);
                    if (isCategoryTag(tag)) {
                        category = tag;
                        break;
                    }
                }
            }
            p.setCategory(category);

            // Qiymətlər
            JSONArray variants = product.getJSONArray("variants");
            double minPrice = Double.MAX_VALUE;
            double comparePrice = 0;
            String unit = "";

            for (int j = 0; j < variants.length(); j++) {
                JSONObject variant = variants.getJSONObject(j);
                double price = variant.optDouble("price", 0);
                double compare = variant.optDouble("compare_at_price", 0);
                String title = variant.optString("title", "");

                if (price > 0 && price < minPrice) {
                    minPrice = price;
                    comparePrice = compare;
                    unit = title;
                }
            }

            p.setDiscountPrice(minPrice);
            p.setUnit(unit);

            if (comparePrice > minPrice && comparePrice > 0) {
                p.setOriginalPrice(comparePrice);
                double percent = ((comparePrice - minPrice) / comparePrice) * 100;
                p.setDiscountPercent(Math.round(percent));
            }

            // Şəkil URL - DÜZGÜN
            JSONArray images = product.optJSONArray("images");
            if (images != null && images.length() > 0) {
                JSONObject image = images.getJSONObject(0);
                String imgUrl = image.optString("src", "");
                if (imgUrl != null && !imgUrl.isEmpty()) {
                    if (imgUrl.startsWith("//")) imgUrl = "https:" + imgUrl;
                    p.setImageUrl(imgUrl);
                    Log.d(TAG, "Şəkil URL: " + imgUrl);
                }
            }

            // Məhsul URL
            String handle = product.optString("handle", "");
            if (!handle.isEmpty()) {
                p.setProductUrl("https://bazarstore.az/products/" + handle);
            }

            if (p.getProductName() != null && !p.getProductName().isEmpty() && p.getDiscountPrice() > 0) {
                products.add(p);
            }
        }
        Log.d(TAG, "BazarStore: " + products.size() + " məhsul yükləndi");
        return products;
    }

    // Kateqoriya tag-larını tanımaq
    private static boolean isCategoryTag(String tag) {
        String[] categories = {
                "Deterjan Təmizlik", "Ət Toyuq Balıq", "Təməl Qida", "Uşaq Məhsulları",
                "Atışdırmalıq", "Şəxsi Qulluq", "Dondurulmuş Qida", "Süd Səhər Yeməyi",
                "İçkilər", "Un Məmulatları", "Ev & Bağ", "Meyvə Tərəvəz"
        };
        for (String cat : categories) {
            if (tag.contains(cat)) return true;
        }
        return false;
    }

    // ==================== ARAZ MARKET ====================
    private static List<DiscountProduct> scrapeArazMarketStatic() throws Exception {
        List<DiscountProduct> products = new ArrayList<>();
        Document doc = Jsoup.connect(ARAZ_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(TIMEOUT)
                .get();

        Elements cards = doc.select("section[class*=page_discount_products] .products-card_card__UW0fA");
        if (cards.isEmpty()) cards = doc.select("[class*=products-card]");

        for (Element card : cards) {
            DiscountProduct p = new DiscountProduct();
            p.setStoreName("Araz Market");
            p.setStoreLogoRes("araz");

            Element nameElem = card.select("h2, .product-title, .product-name").first();
            if (nameElem != null) p.setProductName(nameElem.text().trim());
            else continue;

            Element priceBlock = card.select("[class*=price_discount]").first();
            if (priceBlock != null) {
                Element span = priceBlock.select("span").first();
                if (span != null) p.setDiscountPrice(parsePriceStatic(span.text()));
                Element del = priceBlock.select("del").first();
                if (del != null) p.setOriginalPrice(parsePriceStatic(del.text()));
            }

            Element badge = card.select("[class*=badge]").first();
            if (badge != null) {
                String badgeText = badge.text().replaceAll("[^0-9]", "");
                if (!badgeText.isEmpty()) p.setDiscountPercent(Double.parseDouble(badgeText));
            }

            if (p.getDiscountPercent() == 0 && p.getOriginalPrice() > 0 && p.getDiscountPrice() > 0) {
                double percent = ((p.getOriginalPrice() - p.getDiscountPrice()) / p.getOriginalPrice()) * 100;
                p.setDiscountPercent(Math.round(percent));
            }

            Element img = card.select("img").first();
            if (img != null) {
                String src = img.attr("data-src");
                if (src.isEmpty()) src = img.attr("src");
                if (!src.isEmpty() && !src.contains("load")) {
                    if (!src.startsWith("http")) src = "https://www.arazmarket.az" + src;
                    p.setImageUrl(src);
                }
            }
            products.add(p);
        }
        return products;
    }

    private static List<DiscountProduct> scrapeBravoMarketStatic() throws Exception {
        List<DiscountProduct> products = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(BRAVO_URL).userAgent("Mozilla/5.0").timeout(TIMEOUT).get();
            Elements cards = doc.select(".product-card, .product-item, .sale-item");
            for (Element card : cards) {
                DiscountProduct p = new DiscountProduct();
                p.setStoreName("Bravo");
                p.setStoreLogoRes("bravo");

                Element nameElem = card.select(".product-title, .product-name, h3").first();
                if (nameElem != null) p.setProductName(nameElem.text().trim());
                else continue;

                Element priceElem = card.select(".price, .current-price").first();
                if (priceElem != null) p.setDiscountPrice(parsePriceStatic(priceElem.text()));

                Element oldPriceElem = card.select(".old-price, del").first();
                if (oldPriceElem != null) p.setOriginalPrice(parsePriceStatic(oldPriceElem.text()));

                if (p.getOriginalPrice() > 0 && p.getDiscountPrice() > 0) {
                    double percent = ((p.getOriginalPrice() - p.getDiscountPrice()) / p.getOriginalPrice()) * 100;
                    p.setDiscountPercent(Math.round(percent));
                }
                products.add(p);
            }
        } catch (Exception e) { Log.e(TAG, "Bravo xətası: " + e.getMessage()); }
        return products;
    }

    private static List<DiscountProduct> scrapeMegastoreStatic() throws Exception {
        List<DiscountProduct> products = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(MEGASTORE_URL).userAgent("Mozilla/5.0").timeout(TIMEOUT).get();
            Elements cards = doc.select(".product, .product-item, .catalog-card");
            for (Element card : cards) {
                DiscountProduct p = new DiscountProduct();
                p.setStoreName("Megastore");
                p.setStoreLogoRes("megastore");

                Element nameElem = card.select(".product-name, .title, h3").first();
                if (nameElem != null) p.setProductName(nameElem.text().trim());
                else continue;

                Element priceElem = card.select(".price, .product-price").first();
                if (priceElem != null) p.setDiscountPrice(parsePriceStatic(priceElem.text()));

                products.add(p);
            }
        } catch (Exception e) { Log.e(TAG, "Megastore xətası: " + e.getMessage()); }
        return products;
    }

    private static String fetchUrlStatic(String urlString) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            return response.toString();
        } finally { if (conn != null) conn.disconnect(); }
    }

    private static double parsePriceStatic(String text) {
        if (text == null || text.isEmpty()) return 0;
        String clean = text.replace("₼", "").replace("AZN", "").replace("manat", "")
                .replace(",", ".").replaceAll("[^0-9.]", "").trim();
        if (clean.isEmpty()) return 0;
        try { return Double.parseDouble(clean); } catch (NumberFormatException e) { return 0; }
    }
}