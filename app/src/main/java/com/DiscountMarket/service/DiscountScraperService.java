package com.DiscountMarket.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.DiscountMarket.model.DiscountProduct;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscountScraperService {

    private static final String TAG = "DiscountScraper";
    private static final String CACHE_FILE_NAME = "araz_market_cache.json";
    private static final String PREF_NAME = "araz_market_prefs";
    private static final String KEY_LAST_UPDATE = "last_update_date";
    private static final String ARAZ_COOKIE = "araz_market_session=eyJpdiI6IlNMeUZuUzNTOUpWVWI1WmgzQmVTSlE9PSIsInZhbHVlIjoiMWplQ0FaQ1REaFo3WlMvUTZuRHNHeXZJTEI3S0JqZkhYQmpxQWF3VzBKUWdOdWN0UkwvcmhQNU01N2pyWWUzcHBoVThTWFVYZlR5RlVQRE4xYjZ1akJtYzJlbmpzTVpoc0ZFZ0ZZa0Z2Yzg4VnVJZTg5VGM5U1RJdi9vM3ZoaisiLCJtYWMiOiJjNmY4NWIzYTZhNjkyOGM3N2Q3YjBkNTQ5ZTNiMDU0NTlmNWY0NzVkMjM3MjIyYTI0MjYzOTIxZTJhMTM4ZGUxIiwidGFnIjoiIn0%3D";

    private static List<DiscountProduct> cachedProducts = null;

    private static final String BAZARSTORE_API = "https://bazarstore.az/collections/endirimli-mehsullar/products.json?limit=250";
    private static final String BRAVO_URL = "https://bravo.az/az/endiriml%C9%99r";
    private static final String MEGASTORE_URL = "https://megastore.az/az/endiriml%C9%99r";

    public interface StoreScraperCallback {
        void onSuccess(List<DiscountProduct> products, int totalCount);
        void onError(String error);
        void onProgress(String message);
    }

    // ==================== CACHE METODLARI ====================

    public static void loadCache(Context context) {
        try {
            File cacheFile = new File(context.getFilesDir(), CACHE_FILE_NAME);
            if (cacheFile.exists()) {
                FileInputStream fis = new FileInputStream(cacheFile);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                fis.close();

                Gson gson = new Gson();
                Type type = new TypeToken<List<DiscountProduct>>(){}.getType();
                cachedProducts = gson.fromJson(sb.toString(), type);
                Log.d(TAG, "✅ Cache yükləndi: " + (cachedProducts != null ? cachedProducts.size() : 0) + " məhsul");
            }
        } catch (Exception e) {
            Log.e(TAG, "Cache yükləmə xətası: " + e.getMessage());
        }
    }

    private static void saveCache(Context context, List<DiscountProduct> products) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(products);
            FileOutputStream fos = context.openFileOutput(CACHE_FILE_NAME, Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(json);
            writer.close();
            fos.close();
            cachedProducts = new ArrayList<>(products);

            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_LAST_UPDATE, today).apply();
            Log.d(TAG, "✅ Cache saxlanıldı: " + products.size() + " məhsul");
        } catch (Exception e) {
            Log.e(TAG, "Cache yazma xətası: " + e.getMessage());
        }
    }

    private static boolean isTodayUpdated(Context context) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lastUpdate = prefs.getString(KEY_LAST_UPDATE, "");
        return today.equals(lastUpdate);
    }

    // ==================== ARAZ MARKET - BÜTÜN SƏHİFƏLƏRİ SKRAP EDİR ====================

    private static List<DiscountProduct> scrapeArazMarketHttp(Context context, StoreScraperCallback callback) throws Exception {
        List<DiscountProduct> allProducts = new ArrayList<>();
        Set<String> productUrls = new HashSet<>();

        int page = 1;
        boolean hasNextPage = true;

        callback.onProgress("🔍 Araz Market skrap edilir...");

        while (hasNextPage && page <= 20) { // Maksimum 20 səhifə
            callback.onProgress("📄 Səhifə " + page + " yüklənir...");

            // Hər səhifə üçün URL
            String categoryUrl = "https://arazmarket.az/az/categories/meyve-terevez-bitkiler-1325?page=" + page;

            String html = fetchUrlWithCookie(categoryUrl, ARAZ_COOKIE);

            if (html == null) {
                Log.e(TAG, "Səhifə " + page + " yüklənmədi");
                break;
            }

            // Regex ilə məhsul linklərini tap
            Pattern pattern = Pattern.compile("/az/products/[a-z0-9-]+-\\d+");
            Matcher matcher = pattern.matcher(html);

            int foundOnPage = 0;
            while (matcher.find()) {
                String url = "https://arazmarket.az" + matcher.group();
                if (productUrls.add(url)) {
                    foundOnPage++;
                }
            }

            Log.d(TAG, "📄 Səhifə " + page + ": " + foundOnPage + " yeni məhsul linki");

            // Növbəti səhifəni yoxla
            if (html.contains("next") && html.contains("pagination")) {
                hasNextPage = true;
                page++;
            } else {
                // Alternativ: əgər "next" class-ı yoxdursa, səhifədə məhsul sayına bax
                if (foundOnPage < 20) { // Əgər az məhsul varsa, son səhifədir
                    hasNextPage = false;
                } else {
                    page++;
                }
            }

            Thread.sleep(500); // Serveri yormamaq üçün
        }

        callback.onProgress("📦 Ümumilikdə " + productUrls.size() + " məhsul linki tapıldı");
        Log.d(TAG, "📦 Ümumilikdə " + productUrls.size() + " məhsul linki tapıldı");

        // 2. Hər məhsul səhifəsini ayrıca yüklə
        int count = 0;
        List<String> urlList = new ArrayList<>(productUrls);

        for (String productUrl : urlList) {
            try {
                count++;
                callback.onProgress("🔄 " + count + "/" + urlList.size() + " yüklənir: " +
                        (count * 100 / urlList.size()) + "%");

                String productHtml = fetchUrlWithCookie(productUrl, ARAZ_COOKIE);
                if (productHtml != null) {
                    DiscountProduct p = parseProductFromHtml(productHtml, productUrl);
                    if (p != null && p.getProductName() != null && !p.getProductName().isEmpty()) {
                        allProducts.add(p);
                        if (count % 10 == 0) {
                            Log.d(TAG, "✅ " + count + "/" + urlList.size() + " məhsul yükləndi");
                        }
                    }
                }
                Thread.sleep(100); // Rate limiting
            } catch (Exception e) {
                Log.e(TAG, "Məhsul xətası: " + e.getMessage());
            }
        }

        callback.onProgress("✅ " + allProducts.size() + " məhsul yükləndi");
        return allProducts;
    }

    private static String fetchUrlWithCookie(String urlString, String cookie) {
        HttpURLConnection conn = null;
        int retryCount = 0;

        while (retryCount < 3) {
            try {
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                conn.setRequestProperty("Accept-Language", "az,en;q=0.9");
                conn.setRequestProperty("Cookie", cookie);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    return sb.toString();
                } else if (responseCode == 429 || responseCode == 503) {
                    // Rate limiting - gözlə və təkrar et
                    Thread.sleep(2000);
                    retryCount++;
                    continue;
                } else {
                    Log.e(TAG, "HTTP " + responseCode + " -> " + urlString);
                    return null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Fetch xətası (cəhd " + (retryCount + 1) + "): " + e.getMessage());
                retryCount++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        return null;
    }

    private static DiscountProduct parseProductFromHtml(String html, String url) {
        try {
            DiscountProduct p = new DiscountProduct();
            p.setStoreName("Araz Market");
            p.setStoreLogoRes("araz");
            p.setProductUrl(url);

            // Məhsul adı - h1 tag
            Pattern namePattern = Pattern.compile("<h1[^>]*>([^<]+)</h1>");
            Matcher nameMatcher = namePattern.matcher(html);
            if (nameMatcher.find()) {
                String name = nameMatcher.group(1).trim();
                // HTML entity-ləri təmizlə
                name = name.replace("&nbsp;", " ").replace("&amp;", "&");
                p.setProductName(name);
            } else {
                // Title-dən
                Pattern titlePattern = Pattern.compile("<title>([^<]+)</title>");
                Matcher titleMatcher = titlePattern.matcher(html);
                if (titleMatcher.find()) {
                    String title = titleMatcher.group(1);
                    title = title.replace(" | Araz Supermarket", "").trim();
                    p.setProductName(title);
                }
            }

            if (p.getProductName() == null || p.getProductName().isEmpty()) return null;

            // Qiymət - JSON-LD-dən
            Pattern jsonLdPattern = Pattern.compile("<script type=\"application/ld\\+json\">(.*?)</script>", Pattern.DOTALL);
            Matcher jsonLdMatcher = jsonLdPattern.matcher(html);
            while (jsonLdMatcher.find()) {
                try {
                    String jsonStr = jsonLdMatcher.group(1);
                    org.json.JSONObject json = new org.json.JSONObject(jsonStr);
                    if (json.has("offers")) {
                        Object offers = json.get("offers");
                        if (offers instanceof org.json.JSONObject) {
                            org.json.JSONObject offerObj = (org.json.JSONObject) offers;
                            if (offerObj.has("price")) {
                                double price = offerObj.getDouble("price");
                                p.setDiscountPrice(price);
                                p.setOriginalPrice(price);
                                break;
                            }
                        } else if (offers instanceof org.json.JSONArray) {
                            org.json.JSONArray offersArray = (org.json.JSONArray) offers;
                            if (offersArray.length() > 0) {
                                org.json.JSONObject offerObj = offersArray.getJSONObject(0);
                                if (offerObj.has("price")) {
                                    double price = offerObj.getDouble("price");
                                    p.setDiscountPrice(price);
                                    p.setOriginalPrice(price);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // JSON parse xətası - davam et
                }
            }

            // Qiymət - alternativ (meta og:price)
            if (p.getDiscountPrice() == 0) {
                Pattern pricePattern = Pattern.compile("<meta[^>]*property=\"product:price:amount\"[^>]*content=\"([^\"]+)\"");
                Matcher priceMatcher = pricePattern.matcher(html);
                if (priceMatcher.find()) {
                    try {
                        double price = Double.parseDouble(priceMatcher.group(1));
                        p.setDiscountPrice(price);
                        p.setOriginalPrice(price);
                    } catch (Exception e) {}
                }
            }

            // Qiymət - span ilə axtar
            if (p.getDiscountPrice() == 0) {
                Pattern priceSpanPattern = Pattern.compile("<span[^>]*class=\"[^\"]*price[^\"]*\"[^>]*>([^<]+)</span>", Pattern.CASE_INSENSITIVE);
                Matcher priceSpanMatcher = priceSpanPattern.matcher(html);
                if (priceSpanMatcher.find()) {
                    String priceText = priceSpanMatcher.group(1);
                    priceText = priceText.replace("₼", "").replace("AZN", "").trim();
                    try {
                        double price = Double.parseDouble(priceText);
                        p.setDiscountPrice(price);
                        p.setOriginalPrice(price);
                    } catch (Exception e) {}
                }
            }

            // Şəkil
            Pattern imgPattern = Pattern.compile("<img[^>]*src=\"([^\"]+)\"[^>]*>");
            Matcher imgMatcher = imgPattern.matcher(html);
            if (imgMatcher.find()) {
                String imgUrl = imgMatcher.group(1);
                if (imgUrl.startsWith("//")) imgUrl = "https:" + imgUrl;
                if (imgUrl.startsWith("/")) imgUrl = "https://arazmarket.az" + imgUrl;

                // Next.js formatını decode et
                if (imgUrl.contains("/_next/image?url=")) {
                    try {
                        String encoded = imgUrl.substring(imgUrl.indexOf("url=") + 4);
                        int amp = encoded.indexOf("&");
                        if (amp > 0) encoded = encoded.substring(0, amp);
                        imgUrl = URLDecoder.decode(encoded, "UTF-8");
                    } catch (Exception e) {}
                }
                p.setImageUrl(imgUrl);
            }

            // Vahid
            String name = p.getProductName().toLowerCase();
            if (name.contains("kq")) p.setUnit("kq");
            else if (name.contains("ml")) p.setUnit("ml");
            else if (name.contains("litr") || name.contains("l")) p.setUnit("l");
            else if (name.contains("qr") || name.contains("qram")) p.setUnit("qr");
            else p.setUnit("əd");

            return p;

        } catch (Exception e) {
            Log.e(TAG, "Parse xətası: " + e.getMessage());
            return null;
        }
    }

    // ==================== DİGƏR MAĞAZALAR ====================

    private static List<DiscountProduct> scrapeBazarStore() throws Exception {
        List<DiscountProduct> products = new ArrayList<>();
        String response = fetchUrl(BAZARSTORE_API);
        if (response == null) return products;

        org.json.JSONObject json = new org.json.JSONObject(response);
        org.json.JSONArray productsArray = json.getJSONArray("products");

        for (int i = 0; i < productsArray.length(); i++) {
            try {
                org.json.JSONObject product = productsArray.getJSONObject(i);
                DiscountProduct p = new DiscountProduct();
                p.setStoreName("BazarStore");
                p.setStoreLogoRes("bazarstore");
                p.setProductName(product.optString("title"));

                org.json.JSONArray images = product.optJSONArray("images");
                if (images != null && images.length() > 0) {
                    String imgUrl = images.getJSONObject(0).optString("src", "");
                    if (!imgUrl.isEmpty()) {
                        if (imgUrl.startsWith("//")) imgUrl = "https:" + imgUrl;
                        p.setImageUrl(imgUrl);
                    }
                }

                org.json.JSONArray tags = product.optJSONArray("tags");
                if (tags != null && tags.length() > 0) {
                    p.setCategory(tags.getString(0));
                }

                org.json.JSONArray variants = product.optJSONArray("variants");
                double minPrice = Double.MAX_VALUE;
                double comparePrice = 0;

                if (variants != null) {
                    for (int j = 0; j < variants.length(); j++) {
                        org.json.JSONObject variant = variants.getJSONObject(j);
                        double price = variant.optDouble("price", 0);
                        double compare = variant.optDouble("compare_at_price", 0);
                        if (price > 0 && price < minPrice) {
                            minPrice = price;
                            comparePrice = compare;
                        }
                    }
                }

                if (minPrice == Double.MAX_VALUE) continue;
                p.setDiscountPrice(minPrice);

                if (comparePrice > minPrice && comparePrice > 0) {
                    p.setOriginalPrice(comparePrice);
                    double percent = ((comparePrice - minPrice) / comparePrice) * 100;
                    p.setDiscountPercent(Math.round(percent));
                } else {
                    p.setOriginalPrice(minPrice);
                }

                products.add(p);

            } catch (Exception e) {
                Log.e(TAG, "BazarStore xətası: " + e.getMessage());
            }
        }
        return products;
    }

    private static List<DiscountProduct> scrapeBravoMarket() throws Exception {
        List<DiscountProduct> products = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(BRAVO_URL).userAgent("Mozilla/5.0").timeout(30000).get();
            Elements productCards = doc.select(".product-card, .product-item, .col-lg-3");

            for (Element card : productCards) {
                try {
                    DiscountProduct p = new DiscountProduct();
                    p.setStoreName("Bravo");
                    p.setStoreLogoRes("bravo");

                    Element nameElem = card.select(".product-title, .product-name, h3, .title").first();
                    if (nameElem != null && !nameElem.text().trim().isEmpty()) {
                        p.setProductName(nameElem.text().trim());
                    } else continue;

                    Element priceElem = card.select(".price, .current-price, .product-price, .offer-price").first();
                    if (priceElem != null) {
                        double price = parsePrice(priceElem.text());
                        p.setDiscountPrice(price);
                        p.setOriginalPrice(price);
                    } else continue;

                    // Şəkil URL
                    Element imgElem = card.select("img").first();
                    if (imgElem != null && imgElem.hasAttr("src")) {
                        String imgUrl = imgElem.attr("src");
                        if (!imgUrl.startsWith("http")) {
                            imgUrl = "https://bravo.az" + imgUrl;
                        }
                        p.setImageUrl(imgUrl);
                    }

                    products.add(p);
                } catch (Exception e) {
                    Log.e(TAG, "Bravo məhsul xətası: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Bravo xətası: " + e.getMessage());
        }
        return products;
    }

    private static List<DiscountProduct> scrapeMegastore() throws Exception {
        List<DiscountProduct> products = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(MEGASTORE_URL).userAgent("Mozilla/5.0").timeout(30000).get();
            Elements productCards = doc.select(".product, .product-item, .product-card");

            for (Element card : productCards) {
                try {
                    DiscountProduct p = new DiscountProduct();
                    p.setStoreName("Megastore");
                    p.setStoreLogoRes("megastore");

                    Element nameElem = card.select(".product-name, .title, h3").first();
                    if (nameElem != null && !nameElem.text().trim().isEmpty()) {
                        p.setProductName(nameElem.text().trim());
                    } else continue;

                    Element priceElem = card.select(".price, .product-price, .current-price").first();
                    if (priceElem != null) {
                        double price = parsePrice(priceElem.text());
                        p.setDiscountPrice(price);
                        p.setOriginalPrice(price);
                    } else continue;

                    products.add(p);
                } catch (Exception e) {
                    Log.e(TAG, "Megastore məhsul xətası: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Megastore xətası: " + e.getMessage());
        }
        return products;
    }

    // ==================== KÖMƏKÇİ METODLAR ====================

    private static String fetchUrl(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            if (conn.getResponseCode() != 200) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static double parsePrice(String text) {
        if (text == null || text.isEmpty()) return 0;
        String clean = text.replace("₼", "").replace("AZN", "").replace("manat", "")
                .replace(",", ".").replaceAll("[^0-9.]", "").trim();
        if (clean.isEmpty()) return 0;
        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ==================== PUBLIC API ====================

    public static void scrapeStore(int storeIndex, StoreScraperCallback callback, Context context) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String[] storeNames = {"BazarStore", "Araz Market", "Bravo", "Megastore"};
            String name = storeIndex < storeNames.length ? storeNames[storeIndex] : "Mağaza";
            mainHandler.post(() -> callback.onProgress(name + " yüklənir..."));

            List<DiscountProduct> products = new ArrayList<>();

            try {
                switch (storeIndex) {
                    case 0:
                        products = scrapeBazarStore();
                        break;
                    case 1:
                        // Cache-dən yüklə
                        if (isTodayUpdated(context) && cachedProducts != null && !cachedProducts.isEmpty()) {
                            List<DiscountProduct> cached = new ArrayList<>(cachedProducts);
                            final int cachedSize = cached.size();
                            products = cached;
                            mainHandler.post(() -> callback.onProgress("✅ Cache-dən " + cachedSize + " məhsul"));
                        } else {
                            products = scrapeArazMarketHttp(context, callback);
                            if (!products.isEmpty()) {
                                saveCache(context, products);
                            }
                        }
                        break;
                    case 2:
                        products = scrapeBravoMarket();
                        break;
                    case 3:
                        products = scrapeMegastore();
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Xəta: " + e.getMessage());
                final String errorMsg = e.getMessage();
                mainHandler.post(() -> callback.onError(name + ": " + errorMsg));
            }

            final List<DiscountProduct> finalProducts = products;
            mainHandler.post(() -> {
                if (finalProducts == null || finalProducts.isEmpty()) {
                    callback.onError(name + ": məhsul tapılmadı");
                } else {
                    callback.onSuccess(finalProducts, finalProducts.size());
                }
            });
        });

        executor.shutdown();
    }

    public static void clearCache() {
        cachedProducts = null;
        Log.d(TAG, "🗑️ Cache təmizləndi");
    }
}