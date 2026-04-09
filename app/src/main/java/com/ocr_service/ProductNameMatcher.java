package com.ocr_service;


import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;

/**
 * bazarstore.az JSON məlumatından məhsul adlarını yükləyir
 * və OCR mətnini fuzzy match ilə düzgün adla əvəz edir.
 *
 * Match strategiyası (ardıcıl, birincisi qazanır):
 *   1. Exact match (normallaşdırdıqdan sonra)
 *   2. StartsWith — OCR mətni JSON adının başlanğıcında var
 *   3. Token overlap — ortaq söz sayı / ümumi söz sayı ≥ 0.6
 *   4. Levenshtein ≤ 2 (hər söz üçün)
 *   5. Heç biri — orijinalı qaytar
 */
public class ProductNameMatcher {

    private static final String TAG = "ProductNameMatcher";
    private static final String JSON_FILE = "data_store/bazarstore/bazarstore_all_products.json"; // assets-dəki fayl adı
    private static final float TOKEN_OVERLAP_THRESHOLD = 0.6f;
    private static final int MAX_LEVENSHTEIN = 2;

    // Yüklənmiş məhsul adları (normalize edilmiş → orijinal)
    private static final Map<String, String> normalizedToOriginal = new LinkedHashMap<>();
    // Tez axtarış üçün token indeksi: token → bu tokeni içərən adlar
    private static final Map<String, List<String>> tokenIndex = new HashMap<>();

    private static boolean loaded = false;

    // ── JSON modeli ───────────────────────────────────────────
    private static class BazarData {
        Map<String, BrandData> brands_with_products;
    }
    private static class BrandData {
        List<ProductData> products;
    }
    private static class ProductData {
        String title;
    }

    /**
     * JSON faylını bir dəfə yüklə.
     * Application.onCreate() və ya Activity.onCreate()-də çağır.
     */
    public static void load(Context context) {
        if (loaded) return;
        try {
            InputStream is = context.getAssets().open(JSON_FILE);
            InputStreamReader reader = new InputStreamReader(is);

            Gson gson = new Gson();
            Type type = new TypeToken<BazarData>(){}.getType();
            BazarData data = gson.fromJson(reader, type);
            reader.close();

            if (data == null || data.brands_with_products == null) {
                Log.e(TAG, "JSON boşdur və ya formatı yanlışdır");
                return;
            }

            int count = 0;
            for (BrandData brand : data.brands_with_products.values()) {
                if (brand.products == null) continue;
                for (ProductData p : brand.products) {
                    if (p.title == null || p.title.isEmpty()) continue;
                    // Uzun başlıqlardaki "..." sonekini kəs
                    String title = p.title.replaceAll("\\.{2,}$", "").trim();
                    String norm = normalize(title);
                    normalizedToOriginal.put(norm, title);
                    // Token indeksi
                    for (String token : norm.split("\\s+")) {
                        if (token.length() >= 3) {
                            tokenIndex.computeIfAbsent(token, k -> new ArrayList<>()).add(norm);
                        }
                    }
                    count++;
                }
            }

            loaded = true;
            Log.i(TAG, "Məhsul adları yükləndi: " + count);
        } catch (Exception e) {
            Log.e(TAG, "JSON yüklənmədi: " + e.getMessage());
        }
    }

    /**
     * OCR-dən gələn adı düzgün JSON adıyla əvəz et.
     * @param ocrText OCR-in oxuduğu xam mətn
     * @return Ən yaxın JSON adı, və ya orijinal (uyğunluq tapılmadıqda)
     */
    public static String match(String ocrText) {
        if (!loaded || ocrText == null || ocrText.trim().isEmpty()) return ocrText;

        String normInput = normalize(ocrText);

        // 1. Exact
        if (normalizedToOriginal.containsKey(normInput)) {
            String result = normalizedToOriginal.get(normInput);
            Log.d(TAG, "[EXACT] " + ocrText + " → " + result);
            return result;
        }

        // 2. StartsWith
        for (Map.Entry<String, String> entry : normalizedToOriginal.entrySet()) {
            if (entry.getKey().startsWith(normInput) && normInput.length() >= 4) {
                Log.d(TAG, "[STARTS] " + ocrText + " → " + entry.getValue());
                return entry.getValue();
            }
        }

        // 3 & 4. Token-based candidate seçimi, sonra Levenshtein
        String best = tokenOverlapAndLevenshtein(normInput);
        if (best != null) {
            String result = normalizedToOriginal.get(best);
            Log.d(TAG, "[FUZZY] " + ocrText + " → " + result);
            return result;
        }

        // Uyğunluq yoxdur
        Log.d(TAG, "[NONE ] " + ocrText + " saxlanıldı");
        return ocrText;
    }

    // ── Daxili metodlar ───────────────────────────────────────

    private static String tokenOverlapAndLevenshtein(String normInput) {
        String[] inputTokens = normInput.split("\\s+");

        // Token indeksindən namizədləri topla
        Set<String> candidates = new HashSet<>();
        for (String tok : inputTokens) {
            if (tok.length() < 3) continue;
            List<String> matches = tokenIndex.get(tok);
            if (matches != null) candidates.addAll(matches);
            // 3-hərf prefix ilə də axtar
            String prefix = tok.substring(0, Math.min(3, tok.length()));
            for (String idxTok : tokenIndex.keySet()) {
                if (idxTok.startsWith(prefix)) {
                    List<String> m = tokenIndex.get(idxTok);
                    if (m != null) candidates.addAll(m);
                }
            }
        }

        String bestCandidate = null;
        float bestScore = 0f;

        for (String candidate : candidates) {
            String[] candTokens = candidate.split("\\s+");

            // Token overlap hesabla
            float overlap = tokenOverlap(inputTokens, candTokens);

            // Levenshtein bonus — uyğun tokenler üçün əlavə xal
            float levBonus = levenshteinBonus(inputTokens, candTokens);

            float score = overlap * 0.7f + levBonus * 0.3f;

            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }

        return (bestScore >= TOKEN_OVERLAP_THRESHOLD) ? bestCandidate : null;
    }

    private static float tokenOverlap(String[] a, String[] b) {
        Set<String> setA = new HashSet<>(Arrays.asList(a));
        Set<String> setB = new HashSet<>(Arrays.asList(b));
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        return union.isEmpty() ? 0f : (float) intersection.size() / union.size();
    }

    private static float levenshteinBonus(String[] inputToks, String[] candToks) {
        int matched = 0;
        for (String it : inputToks) {
            for (String ct : candToks) {
                if (levenshtein(it, ct) <= MAX_LEVENSHTEIN) {
                    matched++;
                    break;
                }
            }
        }
        return inputToks.length == 0 ? 0f : (float) matched / inputToks.length;
    }

    /** Standart Levenshtein məsafəsi */
    private static int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.charAt(i-1) == b.charAt(j-1)) dp[i][j] = dp[i-1][j-1];
                else dp[i][j] = 1 + Math.min(dp[i-1][j-1], Math.min(dp[i-1][j], dp[i][j-1]));
            }
        }
        return dp[m][n];
    }

    /** Böyük hərf, boşluq normallaşdırma, Azərbaycanca hərflər qorunur */
    private static String normalize(String text) {
        return text.toUpperCase(new Locale("az"))
                .replaceAll("[^A-ZƏİĞÖÜÇŞ0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
