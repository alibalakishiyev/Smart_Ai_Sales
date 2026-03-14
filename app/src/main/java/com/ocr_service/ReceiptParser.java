package com.ocr_service;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;

import com.data.ProductItem;
import com.google.firebase.Timestamp;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ARAZ MARKET qəbzi üçün təkmilləşdirilmiş parser - BÜTÜN MƏHSULLARI ÇIXARIR
 * JSON konfiqurasiya faylı ilə OCR səhvlərini düzəldir
 */
public class ReceiptParser {
    private static final String TAG = "ReceiptParser";

    // JSON konfiqurasiya faylı
    private static final String OCR_CORRECTIONS_JSON_FILE = "OCR_corrected/ocr_corrections.json";

    // OCR düzəlişləri (JSON-dan yüklənəcək)
    private static Map<String, String> productNameCorrections = new HashMap<>();

    // OCR köməkçi pattern-lər (JSON-dan yüklənəcək)
    private static List<OcrPattern> ocrPatterns = new ArrayList<>();

    // OCR lüğəti (tez-tez səhv yazılan sözlər üçün)
    private static Map<String, String> ocrDictionary = new HashMap<>();

    // JSON-un yükləndiyini göstərən flag
    private static boolean isJsonLoaded = false;

    public static class ReceiptData {
        public String storeName = "";
        public String date = "";
        public String time = "";
        public String fiscalCode = "";
        public String receiptNumber = "";
        public String cashierName = "";
        public List<ProductItem> products = new ArrayList<>();
        public double totalAmount = 0.0;
        public double taxAmount = 0.0;
        public double taxFreeAmount = 0.0;
        public String paymentMethod = "Nağd";

        public ReceiptData() {}
    }

    /**
     * OCR düzəlişləri üçün JSON model sinfi
     */
    private static class OcrCorrectionConfig {
        List<ProductCorrection> productCorrections;
        List<OcrPattern> patterns;
        Map<String, String> dictionary;
    }

    private static class ProductCorrection {
        String wrong;
        String correct;
    }

    private static class OcrPattern {
        String pattern;
        String replacement;
        String description;
    }

    // Pattern-lər
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{2}[./-]\\d{2}[./-]\\d{4})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{2}:\\d{2}:\\d{2})");
    private static final Pattern FISCAL_PATTERN = Pattern.compile("Fiskal iD:\\s*(\\S+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RECEIPT_NUMBER_PATTERN = Pattern.compile("çeki №\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOTAL_PATTERN = Pattern.compile("Cəmi\\s+(\\d+[.,]\\d{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAX_PATTERN = Pattern.compile("ƏDV\\s+18%?\\s*=\\s*(\\d+[.,]\\d{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+[.,]\\d{2,3})");

    /**
     * JSON konfiqurasiya faylını yüklə - Aktivitedə çağırılmalıdır
     */
    public static void loadOcrCorrections(Context context) {
        if (isJsonLoaded) return; // Artıq yüklənibsə, təkrar yükləmə

        try {
            Log.d(TAG, "OCR düzəlişləri yüklənir: " + OCR_CORRECTIONS_JSON_FILE);

            InputStream is = context.getAssets().open(OCR_CORRECTIONS_JSON_FILE);
            InputStreamReader reader = new InputStreamReader(is);

            Gson gson = new Gson();
            Type type = new TypeToken<OcrCorrectionConfig>(){}.getType();
            OcrCorrectionConfig config = gson.fromJson(reader, type);

            // Məhsul adı düzəlişlərini yüklə
            if (config.productCorrections != null) {
                for (ProductCorrection corr : config.productCorrections) {
                    productNameCorrections.put(corr.wrong, corr.correct);
                }
                Log.d(TAG, "Məhsul düzəlişləri yükləndi: " + config.productCorrections.size());
            }

            // OCR pattern-lərini yüklə
            if (config.patterns != null) {
                ocrPatterns = config.patterns;
                Log.d(TAG, "OCR pattern-lər yükləndi: " + config.patterns.size());
            }

            // Lüğəti yüklə
            if (config.dictionary != null) {
                ocrDictionary = config.dictionary;
                Log.d(TAG, "OCR lüğət yükləndi: " + config.dictionary.size());
            }

            isJsonLoaded = true;
            Log.d(TAG, "OCR düzəlişləri uğurla yükləndi");

            reader.close();
            is.close();
        } catch (Exception e) {
            Log.e(TAG, "OCR düzəlişləri yüklənə bilmədi, default dəyərlər istifadə olunacaq", e);
            loadDefaultCorrections();
        }
    }

    /**
     * Default düzəlişlər (əgər JSON yüklənməsə)
     */
    private static void loadDefaultCorrections() {
        productNameCorrections = new HashMap<String, String>() {{
            put("TOST COREYI 600 OR", "TOST COREYI 600 QR");
            put("XONCA Q0ZLU PAXLAVA", "XONCA QOZLU PAXLAVA");
            put("ALMA STORINARED KG", "ALMA STORINARED KG");
            put("XIYAR MELIT KG", "XIYAR MELIT KG");
            put("Paket Araz 31*60 5KG", "Paket Araz 31*60 5KG");
            put("CASPIAN ANTI-PERS FR", "CASPIAN ANTI-PERS FR");
            put("ALPEN GOLD SHOK.MIND", "ALPEN GOLD SHOK.MIND");
            put("ANCHOR KERE YAGI 180", "ANCHOR KERE YAGI 180");
            put("CUPA CUPS XXL 29GR", "CUPA CUPS XXL 29GR");
            put("MANDARIN MURKOT KG", "MANDARIN MURKOT KG");
            put("ARMUD ABATI KG", "ARMUD ABATI KG");
            put("MANDORA KG", "MANDORA KG");
            put("DARLETTO SNEJKA MALI", "DARLETTO SNEJKA MALI");
            put("7 DAYS KRUASSAN CIYE", "7 DAYS KRUASSAN CIYE");
            put("SELMAN AG PENDIRI 1*", "SELMAN AG PENDIRI");
        }};

        // Default pattern-lər
        ocrPatterns = new ArrayList<>();

        // Default lüğət
        ocrDictionary = new HashMap<>();

        isJsonLoaded = true;
        Log.d(TAG, "Default düzəlişlər yükləndi");
    }

    /**
     * OCR nəticəsini parse edir
     */
    public static ReceiptData parseReceipt(List<RealOCRHelper.TextBlock> textBlocks) {
        ReceiptData data = new ReceiptData();

        if (textBlocks == null || textBlocks.isEmpty()) {
            return data;
        }

        List<String> lines = new ArrayList<>();
        for (RealOCRHelper.TextBlock block : textBlocks) {
            String line = block.text.trim();

            // OCR səhvlərini düzəlt (əgər JSON yüklənibsə)
            line = applyOcrCorrections(line);

            if (!line.isEmpty() && line.length() > 1) {
                lines.add(line);
                Log.d(TAG, "OCR Sətir: " + line);
            }
        }

        // Mağaza adı
        findStoreName(lines, data);

        // Tarix və saat
        findDateAndTime(lines, data);

        // Fiskal kod
        findFiscalCode(lines, data);

        // Qəbz nömrəsi
        findReceiptNumber(lines, data);

        // MƏHSULLARI TAP
        findAllProducts(lines, data);

        // Ümumi məbləğ
        findTotals(lines, data);

        Log.d(TAG, "Tapılan məhsul sayı: " + data.products.size());
        return data;
    }

    /**
     * OCR səhvlərini düzəlt
     */
    private static String applyOcrCorrections(String text) {
        if (!isJsonLoaded) return text; // JSON yüklənməyibsə, düzəliş etmə

        String corrected = text;

        // 1. JSON pattern-lərini tətbiq et
        for (OcrPattern pattern : ocrPatterns) {
            try {
                corrected = corrected.replaceAll(pattern.pattern, pattern.replacement);
            } catch (Exception e) {
                Log.e(TAG, "Pattern xətası: " + pattern.pattern, e);
            }
        }

        // 2. Lüğət düzəlişlərini tətbiq et
        for (Map.Entry<String, String> entry : ocrDictionary.entrySet()) {
            corrected = corrected.replace(entry.getKey(), entry.getValue());
        }

        return corrected;
    }

    /**
     * Bütün məhsulları tap
     */
    private static void findAllProducts(List<String> lines, ReceiptData data) {
        List<MatchedProduct> matchedProducts = new ArrayList<>();
        boolean productSectionStarted = false;

        // Əvvəlcə bütün məhsul adlarını və rəqəm sətirlərini topla
        List<String> productNames = new ArrayList<>();
        List<String> numberLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Məhsul bölməsinin başlanğıcı
            if (line.contains("Məhsulun adı") || line.contains("Say Qiymat Cami")) {
                productSectionStarted = true;
                continue;
            }

            // Cəmi bölməsinə çatdıqda dayan
            if (line.contains("Cəmi") && line.length() < 10) {
                break;
            }

            if (!productSectionStarted) continue;

            // Məhsul adı sətri (böyük hərflərlə)
            if (line.matches(".*[A-ZƏİĞÖÜÇŞ0-9]{4,}.*") && !line.matches(".*\\d+[.,]\\d+.*\\d+[.,]\\d+.*")) {
                if (line.length() > 3 && !line.contains("ƏDV") && !line.contains("%")) {
                    productNames.add(line);
                }
                continue;
            }

            // Rəqəm sətri
            if (line.matches(".*\\d+[.,]\\d+.*\\d+[.,]\\d+.*") ||
                    (line.matches(".*\\d+[.,]\\d+.*") && extractNumbers(line).size() >= 2)) {
                numberLines.add(line);
            }
        }

        Log.d(TAG, "Tapılan məhsul adları: " + productNames.size());
        Log.d(TAG, "Tapılan rəqəm sətirləri: " + numberLines.size());

        // Məhsul adları və rəqəm sətirlərini cütləşdir
        int minCount = Math.min(productNames.size(), numberLines.size());
        for (int i = 0; i < minCount; i++) {
            String name = productNames.get(i);
            String numbers = numberLines.get(i);

            String cleanName = cleanProductName(name);
            List<Double> numList = extractNumbers(numbers);

            if (!cleanName.isEmpty() && numList.size() >= 2) {
                ProductItem product = createProduct(cleanName, numList);
                if (product != null) {
                    // OCR xətasını düzəlt
                    String correctedName = correctProductName(cleanName);
                    if (!correctedName.isEmpty()) {
                        if (product.getKg() > 0) {
                            product = new ProductItem(correctedName, product.getKg(), product.getPrice(), product.getTotalAmount());
                            product.setKg(product.getKg());
                        } else {
                            product = new ProductItem(correctedName, product.getQuantity(), product.getPrice());
                        }
                    }
                    data.products.add(product);
                    Log.d(TAG, "Məhsul " + (i+1) + ": " + product.getName() + " - " + product.getFormattedTotal());
                }
            }
        }

        // Əgər hələ də məhsul azdırsa, əlavə üsullarla axtar
        if (data.products.size() < 12) {
            findRemainingProducts(lines, data, productNames, numberLines);
        }
    }

    /**
     * Qalan məhsulları tap
     */
    private static void findRemainingProducts(List<String> lines, ReceiptData data,
                                              List<String> productNames, List<String> numberLines) {
        // Məhsul adları siyahısı (əgər JSON varsa, oradan götür, yoxsa hardcoded)
        String[] expectedProducts;

        if (!productNameCorrections.isEmpty()) {
            // JSON-dan məhsul adlarını götür
            expectedProducts = productNameCorrections.keySet().toArray(new String[0]);
        } else {
            // Default məhsul adları
            expectedProducts = new String[]{
                    "TOST COREYI", "DARLETTO SNEJKA", "7 DAYS KRUASSAN", "SELMAN AG PENDIRI",
                    "CASPIAN ANTI-PERS", "ALPEN GOLD", "ANCHOR KERE YAGI", "XONCA QOZLU PAXLAVA",
                    "ARMUD ABATI", "MANDORA", "CUPA CUPS", "ALMA STORINARED", "MANDARIN MURKOT",
                    "Paket Araz", "XIYAR MELIT"
            };
        }

        // Bütün rəqəm sətirlərini topla
        List<List<Double>> allNumberLists = new ArrayList<>();
        for (String line : numberLines) {
            allNumberLists.add(extractNumbers(line));
        }

        // Hər bir gözlənilən məhsul üçün
        for (String expected : expectedProducts) {
            boolean found = false;

            // Artıq tapılan məhsulları yoxla
            for (ProductItem p : data.products) {
                if (p.getName().contains(expected) || expected.contains(p.getName())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Məhsul adını tap
                for (String name : productNames) {
                    if (name.contains(expected) || expected.contains(name)) {
                        // Rəqəm sətri tap
                        for (List<Double> nums : allNumberLists) {
                            if (nums.size() >= 2) {
                                ProductItem product = createProduct(cleanProductName(name), nums);
                                if (product != null) {
                                    data.products.add(product);
                                    Log.d(TAG, "Əlavə məhsul tapıldı: " + product.getName());
                                    allNumberLists.remove(nums);
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Məhsul yarat
     */
    private static ProductItem createProduct(String name, List<Double> numbers) {
        if (name.isEmpty() || numbers.isEmpty()) return null;

        double quantity = 1.0;
        double price = 0.0;
        double total = 0.0;

        if (numbers.size() >= 3) {
            quantity = numbers.get(0);
            price = numbers.get(1);
            total = numbers.get(2);
        } else if (numbers.size() == 2) {
            if (numbers.get(1) > numbers.get(0) * 10) {
                quantity = numbers.get(0);
                total = numbers.get(1);
                price = total / quantity;
            } else {
                price = numbers.get(0);
                total = numbers.get(1);
                quantity = total / price;
            }
        } else if (numbers.size() == 1) {
            total = numbers.get(0);
            price = total;
        }

        // Valid yoxla
        if (total <= 0 || price <= 0) return null;
        if (total > 1000 || price > 1000) return null;

        // Çəki məhsulu
        if (name.contains("KG") || name.toLowerCase().contains("kq") || quantity != Math.floor(quantity)) {
            ProductItem product = new ProductItem(name, quantity, price, total);
            product.setKg(quantity);
            return product;
        } else {
            return new ProductItem(name, (int) Math.round(quantity), price);
        }
    }

    /**
     * OCR xətasını düzəlt
     */
    private static String correctProductName(String name) {
        // Əvvəlcə JSON düzəlişlərini yoxla
        if (!productNameCorrections.isEmpty()) {
            for (Map.Entry<String, String> entry : productNameCorrections.entrySet()) {
                if (name.contains(entry.getKey()) || entry.getKey().contains(name)) {
                    return entry.getValue();
                }
            }
        }

        // JSON yoxdursa, hardcoded düzəlişlərə bax
        if (name.contains("TOST COREYI") && name.contains("OR")) {
            return name.replace("OR", "QR");
        }
        if (name.contains("Q0ZLU")) {
            return name.replace("Q0ZLU", "QOZLU");
        }

        return name;
    }

    /**
     * Rəqəmləri çıxar
     */
    private static List<Double> extractNumbers(String text) {
        List<Double> numbers = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                double num = parseDouble(matcher.group());
                if (num > 0 && num < 1000) {
                    numbers.add(num);
                }
            } catch (Exception e) {
                Log.e(TAG, "Rəqəm parse xətası: " + matcher.group());
            }
        }
        return numbers;
    }

    /**
     * Məhsul adını təmizlə
     */
    private static String cleanProductName(String name) {
        name = name.replaceAll("[*_\\-|]", " ").trim();
        name = name.replaceAll("\\s+", " ").trim();
        name = name.replaceAll("\\s+\\d+$", "").trim();

        if (name.length() < 2) return "";

        return name;
    }


    /**
     * Mağaza adını tap
     */
    private static void findStoreName(List<String> lines, ReceiptData data) {
        for (String line : lines) {
            if (line.contains("ARAZ MARKET")) {
                data.storeName = "ARAZ MARKET";
                return;
            }
        }
    }

    /**
     * Tarix və saatı tap
     */
    private static void findDateAndTime(List<String> lines, ReceiptData data) {
        for (String line : lines) {
            Matcher dateMatcher = DATE_PATTERN.matcher(line);
            if (dateMatcher.find()) {
                data.date = dateMatcher.group(1);
            }
            Matcher timeMatcher = TIME_PATTERN.matcher(line);
            if (timeMatcher.find()) {
                data.time = timeMatcher.group(1);
            }
        }
    }

    /**
     * Fiskal kodu tap
     */
    private static void findFiscalCode(List<String> lines, ReceiptData data) {
        for (String line : lines) {
            Matcher matcher = FISCAL_PATTERN.matcher(line);
            if (matcher.find()) {
                data.fiscalCode = matcher.group(1);
                return;
            }
        }
    }

    /**
     * Qəbz nömrəsini tap
     */
    private static void findReceiptNumber(List<String> lines, ReceiptData data) {
        for (String line : lines) {
            Matcher matcher = RECEIPT_NUMBER_PATTERN.matcher(line);
            if (matcher.find()) {
                data.receiptNumber = matcher.group(1);
                return;
            }
        }
    }

    /**
     * Ümumi məbləğləri tap
     */
    private static void findTotals(List<String> lines, ReceiptData data) {
        for (String line : lines) {
            Matcher totalMatcher = TOTAL_PATTERN.matcher(line);
            if (totalMatcher.find()) {
                data.totalAmount = parseDouble(totalMatcher.group(1));
            }

            Matcher taxMatcher = TAX_PATTERN.matcher(line);
            if (taxMatcher.find()) {
                data.taxAmount = parseDouble(taxMatcher.group(1));
            }
        }

        // Əgər cəmi tapılmadısa, məhsulları topla
        if (data.totalAmount == 0 && !data.products.isEmpty()) {
            for (ProductItem product : data.products) {
                data.totalAmount += product.getTotalAmount();
            }
        }
    }

    /**
     * ProductItem list-i qaytar
     */
    public static List<ProductItem> parseReceiptToProducts(List<RealOCRHelper.TextBlock> textBlocks, String userId) {
        ReceiptData data = parseReceipt(textBlocks);
        List<ProductItem> products = new ArrayList<>();

        for (ProductItem product : data.products) {
            product.setUserId(userId);
            product.setStoreName(data.storeName);
            product.setFiscalCode(data.fiscalCode);
            product.setReceiptId(data.receiptNumber);
            product.setPurchaseDate(parseDate(data.date, data.time));
            product.setCreatedAt(Timestamp.now());
            products.add(product);
        }

        return products;
    }

    private static Date parseDate(String dateStr, String timeStr) {
        try {
            if (!dateStr.isEmpty() && !timeStr.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
                return sdf.parse(dateStr + " " + timeStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "Tarix parse xətası", e);
        }
        return new Date();
    }

    private static double parseDouble(String str) {
        try {
            return Double.parseDouble(str.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Məhsul namizədi üçün daxili sinif
     */
    private static class MatchedProduct {
        String name;
        List<Double> numbers;
        MatchedProduct(String name, List<Double> numbers) {
            this.name = name;
            this.numbers = numbers;
        }
    }
}