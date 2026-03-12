package com.ocr_service;

import android.util.Log;
import android.graphics.Rect;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Çek Parser - Böyük hərfli məhsul adlarına fokuslanır
 */
public class ReceiptParser {
    private static final String TAG = "ReceiptParser";

    public static class ReceiptData {
        public String storeName = "";
        public String date = "";
        public String time = "";
        public String fiscalCode = "";
        public List<ProductItem> products = new ArrayList<>();
        public double totalAmount = 0.0;

        public static class ProductItem {
            public String name = "";
            public double quantity = 1.0;
            public double price = 0.0;
            public double total = 0.0;
            public String unit = "ədəd";

            public ProductItem(String name, double quantity, double price, double total) {
                this.name = name;
                this.quantity = quantity;
                this.price = price;
                this.total = total;
            }

            public ProductItem(String name, double quantity, double price, double total, String unit) {
                this.name = name;
                this.quantity = quantity;
                this.price = price;
                this.total = total;
                this.unit = unit;
            }
        }
    }

    // Pattern-lər
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{2}[./-]\\d{2}[./-]\\d{4})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{2}:\\d{2})");
    private static final Pattern TOTAL_PATTERN = Pattern.compile("(?i)(?:cəmi|toplam|yekun|total|CƏMİ|TOPLAM|Yekun|Ümumi|ÜMUMİ)\\s*[:.]?\\s*(\\d+[.,]?\\d*)");

    // BÖYÜK HƏRFLƏRİ TANIMAQ ÜÇÜN PATTERN
    private static final Pattern UPPERCASE_WORDS_PATTERN = Pattern.compile("\\b([A-ZƏİĞÖÜÇŞ]{2,})\\b");

    /**
     * OCR nəticəsini pars edir
     */
    public static ReceiptData parseReceipt(List<RealOCRHelper.TextBlock> textBlocks) {
        ReceiptData data = new ReceiptData();

        if (textBlocks == null || textBlocks.isEmpty()) {
            return data;
        }

        // Mətn bloklarını Y koordinatına görə sırala (yuxarıdan aşağı)
        List<RealOCRHelper.TextBlock> sortedBlocks = new ArrayList<>(textBlocks);
        Collections.sort(sortedBlocks, (b1, b2) -> Integer.compare(b1.rect.top, b2.rect.top));

        // Sətirləri topla
        List<String> lines = new ArrayList<>();
        for (RealOCRHelper.TextBlock block : sortedBlocks) {
            lines.add(block.text);
            Log.d(TAG, "OCR Line: " + block.text);
        }

        // 1. Mağaza adı (ilk sətirlər)
        findStoreName(lines, data);

        // 2. Tarix və saat
        findDateAndTime(lines, data);

        // 3. MƏHSULLARI TAP - BÖYÜK HƏRFLƏRƏ FOKUSLAN
        findProductsByUppercase(sortedBlocks, data);

        // 4. Ümumi məbləğ
        findTotalAmount(lines, data);

        Log.d(TAG, "Tapılan məhsul sayı: " + data.products.size());
        return data;
    }

    /**
     * Mağaza adını tap
     */
    private static void findStoreName(List<String> lines, ReceiptData data) {
        for (int i = 0; i < Math.min(5, lines.size()); i++) {
            String line = lines.get(i).trim();
            if (line.length() >= 3 && line.length() <= 50 &&
                    !line.matches(".*\\d{4}.*") && !line.contains(":")) {
                data.storeName = line;
                break;
            }
        }
    }

    /**
     * Tarix və saat
     */
    private static void findDateAndTime(List<String> lines, ReceiptData data) {
        for (String line : lines) {
            if (data.date.isEmpty()) {
                Matcher m = DATE_PATTERN.matcher(line);
                if (m.find()) data.date = m.group();
            }
            if (data.time.isEmpty()) {
                Matcher m = TIME_PATTERN.matcher(line);
                if (m.find()) data.time = m.group();
            }
        }
    }

    /**
     * BÖYÜK HƏRFLƏRƏ ƏSASƏN MƏHSULLARI TAP
     */
    private static void findProductsByUppercase(List<RealOCRHelper.TextBlock> blocks, ReceiptData data) {
        List<ProductCandidate> candidates = new ArrayList<>();

        for (RealOCRHelper.TextBlock block : blocks) {
            String line = block.text.trim();

            // Məhsul olmayan sətirləri keç
            if (line.isEmpty() || line.length() < 5) continue;
            if (isNonProductLine(line)) continue;

            // 1. BÖYÜK HƏRFLƏ OLAN SÖZLƏRİ TAP (MƏHSUL ADI)
            String productName = extractUppercaseName(line);

            // 2. RƏQƏMLƏRİ TAP (MİQDAR, QİYMƏT, CƏM)
            List<Double> numbers = extractNumbers(line);

            if (numbers.isEmpty()) continue;

            // Əgər böyük hərfli ad tapılmadısa, bütün hərfləri yoxla
            if (productName.isEmpty()) {
                productName = extractAnyName(line);
            }

            if (productName.isEmpty()) continue;

            // 3. RƏQƏMLƏRDƏN MƏNTİQ ÇIXAR
            ProductCandidate candidate = createProductCandidate(productName, numbers);
            if (candidate != null) {
                candidates.add(candidate);
                Log.d(TAG, "BÖYÜK HƏRFLƏ MƏHSUL TAPILDI: '" + candidate.name + "' - " +
                        candidate.price + " x " + candidate.quantity + " = " + candidate.total);
            }
        }

        // Təkrarları təmizlə və əlavə et
        List<ProductCandidate> unique = removeDuplicates(candidates);
        for (ProductCandidate c : unique) {
            data.products.add(new ReceiptData.ProductItem(c.name, c.quantity, c.price, c.total, "ədəd"));
        }

        // Əgər məhsul tapılmadısa, fallback istifadə et
        if (data.products.isEmpty()) {
            fallbackProductExtraction(blocks, data);
        }
    }

    /**
     * Sətirdən BÖYÜK HƏRFLƏ OLAN SÖZLƏRİ ÇIXAR (MƏHSUL ADI)
     */
    private static String extractUppercaseName(String line) {
        // Əvvəlcə rəqəmləri sil
        String withoutNumbers = line.replaceAll("\\d+[.,]?\\d*", " ").trim();

        // Böyük hərfli sözləri tap
        Matcher m = UPPERCASE_WORDS_PATTERN.matcher(withoutNumbers);
        StringBuilder name = new StringBuilder();

        while (m.find()) {
            String word = m.group();
            // Tək hərfli sözləri keç (A, B, C kimi)
            if (word.length() >= 2) {
                if (name.length() > 0) name.append(" ");
                name.append(word);
            }
        }

        String result = name.toString().trim();

        // Əgər böyük hərfli söz tapılmadısa, bütün sözlərə bax
        if (result.isEmpty()) {
            String[] words = withoutNumbers.split("\\s+");
            for (String word : words) {
                if (word.length() >= 2 && !word.matches(".*[a-z].*")) {
                    if (name.length() > 0) name.append(" ");
                    name.append(word);
                }
            }
            result = name.toString().trim();
        }

        return result;
    }

    /**
     * Sətirdən İSTƏNİLƏN ADI ÇIXAR (böyük hərf tapılmadıqda)
     */
    private static String extractAnyName(String line) {
        // Rəqəmləri sil
        String withoutNumbers = line.replaceAll("\\d+[.,]?\\d*", " ").trim();

        // Xüsusi simvolları təmizlə
        withoutNumbers = withoutNumbers.replaceAll("[^A-Za-zAzəıiğöüçşƏİĞÖÜÇŞ\\s]", " ").trim();
        withoutNumbers = withoutNumbers.replaceAll("\\s+", " ").trim();

        // İlk 3 sözü götür
        String[] words = withoutNumbers.split("\\s+");
        StringBuilder name = new StringBuilder();
        int wordCount = Math.min(3, words.length);

        for (int i = 0; i < wordCount; i++) {
            if (words[i].length() >= 2) {
                if (name.length() > 0) name.append(" ");
                name.append(words[i]);
            }
        }

        return name.toString().trim();
    }

    /**
     * Sətirdən BÜTÜN RƏQƏMLƏRİ ÇIXAR
     */
    private static List<Double> extractNumbers(String line) {
        List<Double> numbers = new ArrayList<>();
        Matcher numMatcher = Pattern.compile("(\\d+[.,]?\\d*)").matcher(line);
        while (numMatcher.find()) {
            numbers.add(parseDouble(numMatcher.group()));
        }
        return numbers;
    }

    /**
     * RƏQƏMLƏRDƏN MƏSUL CANDİDATE YARAT
     */
    private static ProductCandidate createProductCandidate(String name, List<Double> numbers) {
        if (name.isEmpty() || numbers.isEmpty()) return null;

        ProductCandidate candidate = new ProductCandidate();
        candidate.name = name;

        if (numbers.size() == 1) {
            // Tək rəqəm - cəm
            candidate.total = numbers.get(0);
            candidate.price = candidate.total;
            candidate.quantity = 1.0;
        }
        else if (numbers.size() == 2) {
            // İki rəqəm - qiymət və cəm
            candidate.price = numbers.get(0);
            candidate.total = numbers.get(1);
            candidate.quantity = candidate.total / candidate.price;
        }
        else if (numbers.size() >= 3) {
            // Üç və ya daha çox rəqəm
            candidate.total = numbers.get(numbers.size() - 1); // sonuncu cəm
            candidate.price = numbers.get(numbers.size() - 2); // sondan əvvəlki qiymət

            // Miqdarı hesabla
            double calculatedQty = candidate.total / candidate.price;

            // Əgər ilk rəqəm say-a uyğundursa
            if (Math.abs(numbers.get(0) * candidate.price - candidate.total) < 0.5) {
                candidate.quantity = numbers.get(0);
            } else {
                candidate.quantity = calculatedQty;
            }
        }

        // Realistik dəyərlər yoxla
        if (candidate.price <= 0 || candidate.price > 100000) candidate.price = candidate.total;
        if (candidate.quantity <= 0 || candidate.quantity > 1000) candidate.quantity = 1.0;
        if (candidate.total <= 0 || candidate.total > 100000) return null;

        // Quantity tam ədədə yaxındırsa, yuvarlaqlaşdır
        if (Math.abs(candidate.quantity - Math.round(candidate.quantity)) < 0.01) {
            candidate.quantity = Math.round(candidate.quantity);
        }

        return candidate;
    }

    /**
     * Təkrarlanan məhsulları təmizlə
     */
    private static List<ProductCandidate> removeDuplicates(List<ProductCandidate> candidates) {
        List<ProductCandidate> unique = new ArrayList<>();

        for (ProductCandidate c : candidates) {
            boolean found = false;
            for (ProductCandidate u : unique) {
                if (u.name.equalsIgnoreCase(c.name) && Math.abs(u.total - c.total) < 0.1) {
                    found = true;
                    break;
                }
                if (u.name.contains(c.name) || c.name.contains(u.name)) {
                    if (Math.abs(u.total - c.total) < 0.5) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                unique.add(c);
            }
        }

        return unique;
    }

    /**
     * Ümumi məbləği tap
     */
    private static void findTotalAmount(List<String> lines, ReceiptData data) {
        // Əvvəlcə pattern ilə axtar
        for (String line : lines) {
            Matcher m = TOTAL_PATTERN.matcher(line);
            if (m.find()) {
                data.totalAmount = parseDouble(m.group(1));
                return;
            }
        }

        // Pattern tapılmadısa, ən böyük rəqəmi götür
        if (data.totalAmount == 0 && !data.products.isEmpty()) {
            for (ReceiptData.ProductItem p : data.products) {
                data.totalAmount += p.total;
            }
        } else if (data.totalAmount == 0) {
            double maxAmount = 0;
            for (String line : lines) {
                Matcher m = Pattern.compile("(\\d+[.,]\\d{2})").matcher(line);
                while (m.find()) {
                    double val = parseDouble(m.group());
                    if (val > maxAmount) maxAmount = val;
                }
            }
            data.totalAmount = maxAmount;
        }
    }

    /**
     * FALLBACK METOD - heç nə tapılmadıqda
     */
    private static void fallbackProductExtraction(List<RealOCRHelper.TextBlock> blocks, ReceiptData data) {
        Log.d(TAG, "Fallback metod işləyir");

        for (RealOCRHelper.TextBlock block : blocks) {
            String line = block.text.trim();
            if (line.isEmpty() || line.length() < 5) continue;
            if (isNonProductLine(line)) continue;

            List<Double> numbers = extractNumbers(line);
            if (numbers.isEmpty()) continue;

            String name = extractAnyName(line);
            if (name.isEmpty()) name = "Məhsul";

            ProductCandidate candidate = createProductCandidate(name, numbers);
            if (candidate != null) {
                data.products.add(new ReceiptData.ProductItem(
                        candidate.name, candidate.quantity, candidate.price, candidate.total, "ədəd"));
                Log.d(TAG, "Fallback məhsul: " + name + " - " + candidate.total);
            }
        }
    }

    /**
     * Məhsul olmayan sətirləri yoxla
     */
    private static boolean isNonProductLine(String line) {
        String lower = line.toLowerCase();

        String[] nonProductWords = {
                "cəmi", "toplam", "yekun", "total", "verg", "ƏDV", "nağd", "nagd",
                "kart", "qalıq", "qaliq", "endirim", "fiskal", "terminal", "kassir",
                "ünvan", "unvan", "telefon", "voen", "vöen", "tarix", "saat",
                "qəbz", "qebz", "cek", "çek", "mağaza", "magaza", "tarix:", "saat:"
        };

        for (String word : nonProductWords) {
            if (lower.contains(word)) return true;
        }

        if (line.matches(".*\\d{2}[./-]\\d{2}[./-]\\d{4}.*")) return true;
        if (line.matches(".*\\d{2}:\\d{2}.*")) return true;

        return false;
    }

    private static double parseDouble(String str) {
        try {
            return Double.parseDouble(str.replace(",", ".").replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Məhsul namizədi üçün daxili sinif
     */
    private static class ProductCandidate {
        String name = "";
        double quantity = 1.0;
        double price = 0.0;
        double total = 0.0;
    }
}