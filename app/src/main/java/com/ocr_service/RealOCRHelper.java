package com.ocr_service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════
 *  RealOCRHelper — Adaptiv Multi-Pass OCR Mühərriki
 *  Hər ölçüdəki qəbzi maksimum dəqiqliklə oxuyur
 *
 *  Dəstəklənən ölçülər:
 *    • XS  — height < 1300px  → scale 2.0x + güclü kontrast
 *    • S   — height < 1500px  → scale 1.7x + kontrast
 *    • M   — height < 1800px  → scale 1.5x + standart
 *    • L   — height < 2200px  → scale 1.3x + yüngül
 *    • XL  — height >= 2200px → scale 1.1x + minimal
 *
 *  6 Preprocessing Metodu:
 *    0 - Orijinal (böyüdülmüş)
 *    1 - Adaptiv eşik (Otsu-benzər)
 *    2 - Yüksək kontrast ağ-qara
 *    3 - Tersine çevrilmiş + kontrast
 *    4 - Kəskinləşdirilmiş (Laplacian-benzər)
 *    5 - Gamma düzəltmə + parlaq
 * ═══════════════════════════════════════════════════════════════
 */
public class RealOCRHelper {

    private static final String TAG = "RealOCRHelper";

    // ── Ölçü Həddləri ──────────────────────────────────────────
    private static final int HEIGHT_XS  = 1300;
    private static final int HEIGHT_S   = 1500;
    private static final int HEIGHT_M   = 1800;
    private static final int HEIGHT_L   = 2200;

    // ── Hər ölçü üçün optimal böyütmə faktoru ──────────────────
    private static final float SCALE_XS = 2.0f;
    private static final float SCALE_S  = 1.7f;
    private static final float SCALE_M  = 1.5f;
    private static final float SCALE_L  = 1.3f;
    private static final float SCALE_XL = 1.1f;

    // ── Minimum tələb olunan blok sayları ──────────────────────
    private static final int MIN_BLOCKS_XS = 3;
    private static final int MIN_BLOCKS_S  = 5;
    private static final int MIN_BLOCKS_M  = 7;
    private static final int MIN_BLOCKS_L  = 10;
    private static final int MIN_BLOCKS_XL = 12;

    // ── Maksimum preprocessing keçiş sayı ─────────────────────
    private static final int MAX_ATTEMPTS = 6;

    private TextRecognizer textRecognizer;
    private final Context context;

    // ── TextBlock modeli ──────────────────────────────────────
    public static class TextBlock {
        public String text;
        public Rect rect;
        public float confidence;
        public int lineIndex;

        public TextBlock(String text, Rect rect, float confidence) {
            this.text = text;
            this.rect = rect;
            this.confidence = confidence;
        }

        public TextBlock(String text, Rect rect) {
            this(text, rect, 1.0f);
        }

        @Override
        public String toString() {
            return String.format("'%s' @ [%d,%d] conf=%.2f", text, rect.top, rect.left, confidence);
        }
    }

    // ── Şəkil metadata modeli ─────────────────────────────────
    private static class ImageProfile {
        int width, height;
        float scaleFactor;
        int minRequiredBlocks;
        String category;

        ImageProfile(int w, int h) {
            this.width = w;
            this.height = h;
            if (h < HEIGHT_XS) {
                scaleFactor = SCALE_XS;
                minRequiredBlocks = MIN_BLOCKS_XS;
                category = "XS";
            } else if (h < HEIGHT_S) {
                scaleFactor = SCALE_S;
                minRequiredBlocks = MIN_BLOCKS_S;
                category = "S";
            } else if (h < HEIGHT_M) {
                scaleFactor = SCALE_M;
                minRequiredBlocks = MIN_BLOCKS_M;
                category = "M";
            } else if (h < HEIGHT_L) {
                scaleFactor = SCALE_L;
                minRequiredBlocks = MIN_BLOCKS_L;
                category = "L";
            } else {
                scaleFactor = SCALE_XL;
                minRequiredBlocks = MIN_BLOCKS_XL;
                category = "XL";
            }
        }
    }

    public interface OCRCallback {
        void onSuccess(List<TextBlock> textBlocks);
        void onError(String error);
    }

    // ── OCR keçidi nəticəsi ───────────────────────────────────
    private static class AttemptResult {
        List<TextBlock> blocks;
        int methodUsed;
        AttemptResult(List<TextBlock> blocks, int method) {
            this.blocks = blocks;
            this.methodUsed = method;
        }
    }

    public RealOCRHelper(Context context) {
        this.context = context;
        try {
            textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            Log.i(TAG, "✅ ML Kit TextRecognizer uğurla işə salındı");
        } catch (Exception e) {
            Log.e(TAG, "❌ ML Kit başlanğıc xətası: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Ana giriş nöqtəsi — şəkili analiz et və OCR uygula
     */
    public void detectText(Bitmap originalBitmap, OCRCallback callback) {
        if (originalBitmap == null) {
            callback.onError("Bitmap null-dur");
            return;
        }
        if (textRecognizer == null) {
            callback.onError("TextRecognizer başlanmayıb");
            return;
        }

        ImageProfile profile = new ImageProfile(originalBitmap.getWidth(), originalBitmap.getHeight());
        Log.i(TAG, String.format("📏 Şəkil: %dx%d [%s] → scale=%.1fx, minBlocks=%d",
                profile.width, profile.height, profile.category,
                profile.scaleFactor, profile.minRequiredBlocks));

        // Ön böyütmə — bütün metodlar üçün base kimi istifadə olunur
        Bitmap scaledBase = scaleBitmap(originalBitmap, profile.scaleFactor);
        Log.d(TAG, String.format("   Böyüdülmüş baza: %dx%d", scaledBase.getWidth(), scaledBase.getHeight()));

        // Multi-pass başlat
        runMultiPass(scaledBase, originalBitmap, profile, callback, 0, null);
    }

    /**
     * Rekursiv Multi-Pass OCR
     * Hər keçiddə fərqli preprocessing metodunu sınayır,
     * ən yaxşı nəticəni qaytarır.
     */
    private void runMultiPass(Bitmap scaledBase, Bitmap originalBitmap,
                              ImageProfile profile, OCRCallback callback,
                              int attempt, AttemptResult bestSoFar) {

        if (attempt >= MAX_ATTEMPTS) {
            // Bütün cəhdlər bitdi — ən yaxşı nəticəni qaytar
            if (bestSoFar != null && !bestSoFar.blocks.isEmpty()) {
                Log.i(TAG, String.format("✅ Final: %d blok (method %d)", bestSoFar.blocks.size(), bestSoFar.methodUsed));
                callback.onSuccess(bestSoFar.blocks);
            } else {
                // Son çarə: originalı birbaşa ver
                runFallback(originalBitmap, callback);
            }
            return;
        }

        Bitmap processedBitmap = applyPreprocessing(scaledBase, attempt, profile);
        InputImage image = InputImage.fromBitmap(processedBitmap, 0);

        final int currentAttempt = attempt;
        final Bitmap finalProcessed = processedBitmap;

        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<TextBlock> blocks = extractTextBlocks(visionText, attempt);

                    Log.d(TAG, String.format("   Cəhd %d [%s]: %d blok",
                            currentAttempt, getMethodName(currentAttempt), blocks.size()));

                    // Bitmap-i yaddaşdan sil (əgər base ilə fərqlidirsə)
                    if (finalProcessed != scaledBase) {
                        finalProcessed.recycle();
                    }

                    // Ən yaxşı nəticəni yenilə
                    AttemptResult current = new AttemptResult(blocks, currentAttempt);
                    AttemptResult newBest = chooseBetter(bestSoFar, current, profile.minRequiredBlocks);

                    // Kifayət qədər blok tapıldısa, bitir
                    if (blocks.size() >= profile.minRequiredBlocks + 3) {
                        Log.i(TAG, String.format("✅ Kifayətdir! %d blok (method %d)", blocks.size(), currentAttempt));
                        callback.onSuccess(blocks);
                        return;
                    }

                    // Növbəti metodla davam et
                    runMultiPass(scaledBase, originalBitmap, profile, callback, currentAttempt + 1, newBest);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "   Cəhd " + currentAttempt + " uğursuz: " + e.getMessage());
                    if (finalProcessed != scaledBase) finalProcessed.recycle();
                    runMultiPass(scaledBase, originalBitmap, profile, callback, currentAttempt + 1, bestSoFar);
                });
    }

    /**
     * 6 Preprocessing Metodu
     */
    private Bitmap applyPreprocessing(Bitmap base, int method, ImageProfile profile) {
        switch (method) {
            case 0:
                // Orijinal böyüdülmüş — heç bir əməliyyat yoxdur
                return base;

            case 1:
                // Adaptiv eşik (Otsu-benzər) — Ağ-qara + adaptiv kəsmə
                return applyAdaptiveThreshold(base);

            case 2:
                // Yüksək kontrast ağ-qara — kontrast gücləndirilir
                return applyHighContrastGrayscale(base, 1.8f);

            case 3:
                // Tersine çevrilmiş + kontrast — tünd fon, açıq mətn üçün
                return applyInvertedContrast(base);

            case 4:
                // Kəskinləşdirilmiş — kənar aşkarlama artırılmış
                return applySharpening(base);

            case 5:
                // Gamma düzəltmə + parlaq
                return applyGammaCorrection(base, 0.7f);

            default:
                return base;
        }
    }

    // ── Preprocessing: Adaptiv Eşik ──────────────────────────
    private Bitmap applyAdaptiveThreshold(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        // Ortalama parlaqlığı hesabla
        long sum = 0;
        for (int p : pixels) {
            sum += luminance(p);
        }
        int avgLum = (int) (sum / pixels.length);
        int threshold = Math.max(100, Math.min(160, avgLum));

        for (int i = 0; i < pixels.length; i++) {
            int lum = luminance(pixels[i]);
            pixels[i] = lum < threshold ? 0xFF000000 : 0xFFFFFFFF;
        }

        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, w, 0, 0, w, h);
        return result;
    }

    // ── Preprocessing: Yüksək Kontrast ───────────────────────
    private Bitmap applyHighContrastGrayscale(Bitmap src, float contrastFactor) {
        int w = src.getWidth(), h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int i = 0; i < pixels.length; i++) {
            int lum = luminance(pixels[i]);
            // S-əyri kontrast artırması
            float norm = lum / 255f;
            norm = (float) Math.pow(norm, 1.0 / contrastFactor);
            int gray = (int) (norm * 255);
            // Güclü kontrast — açıqları daha açıq, qaranlıqları daha qaranlıq
            if (gray > 160) gray = Math.min(255, (int)(gray * 1.1f));
            else if (gray < 80) gray = Math.max(0, (int)(gray * 0.7f));
            gray = clamp(gray);
            pixels[i] = Color.argb(Color.alpha(pixels[i]), gray, gray, gray);
        }

        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, w, 0, 0, w, h);
        return result;
    }

    // ── Preprocessing: Tersine + Kontrast ────────────────────
    private Bitmap applyInvertedContrast(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int i = 0; i < pixels.length; i++) {
            int lum = luminance(pixels[i]);
            int inverted = 255 - lum;
            // Kontrast artır
            if (inverted > 128) inverted = clamp((int)(inverted * 1.3f));
            else inverted = clamp((int)(inverted * 0.7f));
            pixels[i] = Color.argb(Color.alpha(pixels[i]), inverted, inverted, inverted);
        }

        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, w, 0, 0, w, h);
        return result;
    }

    // ── Preprocessing: Kəskinləşdirmə (Unsharp Mask) ─────────
    private Bitmap applySharpening(Bitmap src) {
        // Sadə kəskinləşdirmə: orijinal + (orijinal - bulanıq) * faktor
        int w = src.getWidth(), h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        int[] output = new int[w * h];

        // 3x3 kəskinləşdirmə kernel
        int[][] kernel = {
                {0, -1, 0},
                {-1, 5, -1},
                {0, -1, 0}
        };

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int sumR = 0, sumG = 0, sumB = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int p = pixels[(y + ky) * w + (x + kx)];
                        int k = kernel[ky + 1][kx + 1];
                        sumR += Color.red(p) * k;
                        sumG += Color.green(p) * k;
                        sumB += Color.blue(p) * k;
                    }
                }
                // Ağ-qarayə çevir + kəskin
                int lum = (clamp(sumR) + clamp(sumG) + clamp(sumB)) / 3;
                output[y * w + x] = Color.argb(255, lum, lum, lum);
            }
        }

        // Kənar piksellər
        for (int x = 0; x < w; x++) {
            int lum = luminance(pixels[x]);
            output[x] = Color.argb(255, lum, lum, lum);
            lum = luminance(pixels[(h-1)*w+x]);
            output[(h-1)*w+x] = Color.argb(255, lum, lum, lum);
        }
        for (int y = 0; y < h; y++) {
            int lum = luminance(pixels[y*w]);
            output[y*w] = Color.argb(255, lum, lum, lum);
            lum = luminance(pixels[y*w+w-1]);
            output[y*w+w-1] = Color.argb(255, lum, lum, lum);
        }

        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        result.setPixels(output, 0, w, 0, 0, w, h);
        return result;
    }

    // ── Preprocessing: Gamma Düzəltmə ────────────────────────
    private Bitmap applyGammaCorrection(Bitmap src, float gamma) {
        int w = src.getWidth(), h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        // Gamma LUT (lookup table) — sürətli
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            lut[i] = clamp((int)(255 * Math.pow(i / 255.0, gamma)));
        }

        for (int i = 0; i < pixels.length; i++) {
            int lum = luminance(pixels[i]);
            int corrected = lut[lum];
            // Parlaq et
            corrected = clamp((int)(corrected * 1.15f));
            pixels[i] = Color.argb(Color.alpha(pixels[i]), corrected, corrected, corrected);
        }

        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, w, 0, 0, w, h);
        return result;
    }

    /**
     * ML Kit nəticəsindən TextBlock siyahısı çıxar
     * Sətirləri top koordinatına görə sırala
     */
    private List<TextBlock> extractTextBlocks(Text visionText, int attempt) {
        List<TextBlock> blocks = new ArrayList<>();

        for (Text.TextBlock block : visionText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText().trim();
                Rect box = line.getBoundingBox();

                if (box == null || lineText.isEmpty() || lineText.length() < 2) continue;

                // Çox qısa və yalnız rəqəmsiz sətirləri süz
                if (lineText.length() == 2 && !lineText.matches(".*\\d.*")) continue;

                TextBlock tb = new TextBlock(lineText, box);
                // ML Kit confidence (əgər varsa)
                tb.confidence = calculateLineConfidence(line);
                blocks.add(tb);
            }
        }

        // Top koordinatına görə sırala
        Collections.sort(blocks, (a, b) -> Integer.compare(a.rect.top, b.rect.top));
        return blocks;
    }

    /**
     * Sətrin etimad dərəcəsini hesabla
     */
    private float calculateLineConfidence(Text.Line line) {
        float totalConf = 0;
        int count = 0;
        for (Text.Element elem : line.getElements()) {
            Float conf = elem.getConfidence();
            if (conf != null) {
                totalConf += conf;
                count++;
            }
        }
        return count > 0 ? totalConf / count : 0.85f;
    }

    /**
     * İki nəticəni müqayisə et — daha çox bloklu olanı seç
     */
    private AttemptResult chooseBetter(AttemptResult a, AttemptResult b, int minRequired) {
        if (a == null) return b;
        if (b == null) return a;
        // Minimum tələbi keçəni üstün tut
        boolean aOk = a.blocks.size() >= minRequired;
        boolean bOk = b.blocks.size() >= minRequired;
        if (aOk && !bOk) return a;
        if (!aOk && bOk) return b;
        return a.blocks.size() >= b.blocks.size() ? a : b;
    }

    /**
     * Son çarə: originalı birbaşa ML Kit-ə ver
     */
    private void runFallback(Bitmap original, OCRCallback callback) {
        Log.w(TAG, "⚠️ Fallback: original bitmap ilə cəhd");
        InputImage image = InputImage.fromBitmap(original, 0);
        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    List<TextBlock> blocks = extractTextBlocks(visionText, -1);
                    Log.i(TAG, "Fallback: " + blocks.size() + " blok");
                    callback.onSuccess(blocks);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fallback da uğursuz: " + e.getMessage());
                    callback.onError("OCR tam uğursuz oldu: " + e.getMessage());
                });
    }

    // ── Yardımçı metodlar ─────────────────────────────────────

    private Bitmap scaleBitmap(Bitmap src, float factor) {
        if (factor <= 1.0f || factor > 3.0f) return src;
        try {
            int nw = (int)(src.getWidth() * factor);
            int nh = (int)(src.getHeight() * factor);
            // Maksimum ölçü məhdudiyyəti (OOM qoruma)
            if ((long) nw * nh > 25_000_000L) {
                float safeScale = (float) Math.sqrt(25_000_000.0 / (src.getWidth() * src.getHeight()));
                nw = (int)(src.getWidth() * safeScale);
                nh = (int)(src.getHeight() * safeScale);
                Log.w(TAG, "OOM qoruma: scale " + factor + "x → " + safeScale + "x");
            }
            return Bitmap.createScaledBitmap(src, nw, nh, true);
        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "OOM! Orijinal qaytarılır");
            return src;
        }
    }

    private int luminance(int pixel) {
        // ITU-R BT.601 standartı
        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);
        return (int)(0.299f * r + 0.587f * g + 0.114f * b);
    }

    private int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }

    private String getMethodName(int method) {
        switch (method) {
            case 0: return "Orijinal";
            case 1: return "AdaptivEşik";
            case 2: return "YüksəkKontrast";
            case 3: return "Tersine+Kontrast";
            case 4: return "Kəskinləşdirmə";
            case 5: return "GammaDüzəltmə";
            default: return "Fallback";
        }
    }

    /**
     * Fayldan bitmap yüklə — EXIF orientasiyanı avtomatik düzəldir
     */
    public static Bitmap loadBitmapFromPath(String path) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

            // İlk mərhələ: ölçüyü öyrən (yaddaşa yükləmədən)
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);

            // Çox böyük şəkillər üçün əvvəlcədən kiçilt
            opts.inJustDecodeBounds = false;
            if (opts.outWidth * opts.outHeight > 30_000_000) {
                opts.inSampleSize = 2;
            }

            Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
            if (bitmap == null) return null;

            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            return rotateBitmap(bitmap, orientation);
        } catch (Exception e) {
            Log.e(TAG, "loadBitmapFromPath xətası: " + e.getMessage());
            return null;
        }
    }

    private static Bitmap rotateBitmap(Bitmap src, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:  matrix.postRotate(90);  break;
            case ExifInterface.ORIENTATION_ROTATE_180: matrix.postRotate(180); break;
            case ExifInterface.ORIENTATION_ROTATE_270: matrix.postRotate(270); break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.preScale(-1, 1); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.preScale(1, -1); break;
            default: return src;
        }
        try {
            Bitmap rotated = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
            if (rotated != src) src.recycle();
            return rotated;
        } catch (Exception e) {
            Log.e(TAG, "rotateBitmap xətası: " + e.getMessage());
            return src;
        }
    }

    public void close() {
        if (textRecognizer != null) {
            try {
                textRecognizer.close();
                Log.i(TAG, "TextRecognizer bağlandı");
            } catch (Exception e) {
                Log.e(TAG, "close() xətası: " + e.getMessage());
            }
        }
    }
}