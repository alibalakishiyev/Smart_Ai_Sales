package com.report;

import android.content.Context;
import android.util.Log;
import com.DiscountMarket.model.DiscountProduct;
import com.DiscountMarket.service.DiscountScraperService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FirebaseReportService {

    private static final String TAG = "FirebaseReportService";
    private static final String COL_TRANSACTIONS = "transactions";
    private static final String COL_PRODUCTS     = "userProducts";
    private static final String COL_RECEIPTS     = "receipts";

    private final FirebaseFirestore db;
    private final String userId;
    private final Context context;

    // ─── Callback ────────────────────────────────────────────────────────────

    public interface ReportCallback {
        void onSuccess(ReportData data);
        void onError(String error);
    }

    // ─── Data Models ─────────────────────────────────────────────────────────

    public static class ReportData {
        // Daily
        public double dailyExpense, dailyIncome, dailySavings;
        public int    dailyDiscounts;
        public String dailyTip = "";
        public List<DiscountProduct> dailyDiscountedProducts = new ArrayList<>();

        // Weekly
        public double weeklyExpense, weeklyIncome, weeklySavings;
        public int    weeklyDiscounts;
        public Map<String, Double> dailyBreakdown = new LinkedHashMap<>();

        // Monthly
        public double monthlyExpense, monthlyIncome, monthlySavings;
        public int    monthlyDiscounts;
        public double monthlyAvgDiscount;
        public Map<String, StoreStat> storeRanking = new LinkedHashMap<>();

        // Yearly
        public double yearlyExpense, yearlyIncome, yearlySavings;
        public int    yearlyTotalDiscounts;
        public Map<String, Double> monthlyTrend = new LinkedHashMap<>();
        public List<ProductSavings> topSavingProducts = new ArrayList<>();

        // Store discounts
        public Map<String, List<DiscountProduct>> storeDiscounts = new LinkedHashMap<>();

        // User added products/receipts
        public List<UserProduct> userProducts = new ArrayList<>();
        public List<Receipt>     receipts     = new ArrayList<>();
    }

    public static class StoreStat {
        public String name, icon;
        public int    productCount;
        public double totalSpent, discountPercent;
    }

    public static class ProductSavings {
        public String name;
        public double ourPrice, marketPrice, savingsPercent;
    }

    /** Firestore "userProducts" document */
    public static class UserProduct {
        public String id, name, category, unit;
        public double price, originalPrice;
        public long   addedAt;
        public int    quantity;

        public static UserProduct fromDoc(DocumentSnapshot doc) {
            UserProduct p = new UserProduct();
            p.id            = doc.getId();
            p.name          = doc.getString("name") != null ? doc.getString("name") : "";
            p.category      = doc.getString("category") != null ? doc.getString("category") : "";
            p.unit          = doc.getString("unit") != null ? doc.getString("unit") : "ədəd";
            p.price         = doc.getDouble("price") != null ? doc.getDouble("price") : 0;
            p.originalPrice = doc.getDouble("originalPrice") != null ? doc.getDouble("originalPrice") : p.price;
            p.addedAt       = doc.getLong("addedAt") != null ? doc.getLong("addedAt") : 0;
            p.quantity      = doc.getLong("quantity") != null ? doc.getLong("quantity").intValue() : 1;
            return p;
        }

        public double getSavings()        { return Math.max(0, (originalPrice - price) * quantity); }
        public double getSavingsPercent() { return originalPrice > 0 ? (originalPrice - price) / originalPrice * 100 : 0; }
    }

    /** Firestore "receipts" document */
    public static class Receipt {
        public String         id, storeName, note;
        public double         total;
        public long           date;
        public List<LineItem> items = new ArrayList<>();

        public static Receipt fromDoc(DocumentSnapshot doc) {
            Receipt r = new Receipt();
            r.id        = doc.getId();
            r.storeName = doc.getString("storeName") != null ? doc.getString("storeName") : "";
            r.note      = doc.getString("note") != null ? doc.getString("note") : "";
            r.total     = doc.getDouble("total") != null ? doc.getDouble("total") : 0;
            r.date      = doc.getLong("date") != null ? doc.getLong("date") : 0;

            // items sub-list
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) doc.get("items");
            if (rawItems != null) {
                for (Map<String, Object> m : rawItems) {
                    LineItem li = new LineItem();
                    li.name     = m.get("name")     != null ? (String) m.get("name")              : "";
                    li.qty      = m.get("qty")       != null ? ((Number) m.get("qty")).intValue()  : 1;
                    li.price    = m.get("price")     != null ? ((Number) m.get("price")).doubleValue() : 0;
                    r.items.add(li);
                }
            }
            return r;
        }
    }

    public static class LineItem {
        public String name;
        public int    qty;
        public double price;
        public double total() { return qty * price; }
    }

    // ─── Constructor ─────────────────────────────────────────────────────────

    public FirebaseReportService(Context context) {
        this.context = context;
        this.db      = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        this.userId  = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Loads ALL report data (daily/weekly/monthly/yearly + store discounts +
     * user products + receipts) in parallel and returns a single ReportData.
     */
    public void loadAllReports(ReportCallback callback) {
        if (userId == null) { callback.onError("İstifadəçi daxil olmayıb"); return; }

        ReportData data = new ReportData();
        // 5 parallel tasks: transactions(4 ranges) + storeDiscounts + userProducts + receipts
        // We use a simple latch pattern with AtomicInteger
        final int TASK_COUNT = 7;
        AtomicInteger latch = new AtomicInteger(TASK_COUNT);

        Runnable checkDone = () -> {
            if (latch.decrementAndGet() == 0) callback.onSuccess(data);
        };

        loadDailyTransactions (data, checkDone);
        loadWeeklyTransactions(data, checkDone);
        loadMonthlyTransactions(data, checkDone);
        loadYearlyTransactions (data, checkDone);
        loadStoreDiscounts     (data, checkDone);
        loadUserProducts       (data, checkDone);
        loadReceipts           (data, checkDone);
    }

    /**
     * Loads only the user-added products (for "My Products" screen).
     */
    public void loadUserProducts(ReportCallback callback) {
        if (userId == null) { callback.onError("İstifadəçi daxil olmayıb"); return; }
        ReportData data = new ReportData();
        loadUserProducts(data, () -> callback.onSuccess(data));
    }

    /**
     * Loads only receipts (for "My Receipts" screen).
     */
    public void loadReceipts(ReportCallback callback) {
        if (userId == null) { callback.onError("İstifadəçi daxil olmayıb"); return; }
        ReportData data = new ReportData();
        loadReceipts(data, () -> callback.onSuccess(data));
    }

    // ─── Private loaders ─────────────────────────────────────────────────────

    private void loadDailyTransactions(ReportData data, Runnable done) {
        long start = startOfDay(System.currentTimeMillis());
        long end   = endOfDay  (System.currentTimeMillis());

        db.collection(COL_TRANSACTIONS)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .whereLessThanOrEqualTo   ("timestamp", end)
                .get()
                .addOnSuccessListener(snaps -> {
                    double exp = 0, inc = 0;
                    for (QueryDocumentSnapshot doc : snaps) {
                        String type = doc.getString("type");
                        Double amt  = doc.getDouble("amount");
                        if (amt == null) continue;
                        if ("expense".equals(type)) exp += amt;
                        else if ("income".equals(type)) inc += amt;
                    }
                    data.dailyExpense = exp;
                    data.dailyIncome  = inc;
                    data.dailySavings = inc - exp;
                    data.dailyTip     = buildDailyTip(data.dailySavings);
                    done.run();
                })
                .addOnFailureListener(e -> { Log.e(TAG, "daily tx", e); done.run(); });
    }

    private void loadWeeklyTransactions(ReportData data, Runnable done) {
        long start = startOfWeek(System.currentTimeMillis());

        db.collection(COL_TRANSACTIONS)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .get()
                .addOnSuccessListener(snaps -> {
                    double exp = 0, inc = 0;
                    String[] days = {"Bazar","B.Ertəsi","Ç.Axşamı","Çərşənbə","C.Axşamı","Cümə","Şənbə"};
                    Map<String, Double> breakdown = new LinkedHashMap<>();
                    for (String d : days) breakdown.put(d, 0.0);

                    for (QueryDocumentSnapshot doc : snaps) {
                        String type = doc.getString("type");
                        Double amt  = doc.getDouble("amount");
                        Long   ts   = doc.getLong("timestamp");
                        if (amt == null || ts == null) continue;
                        if ("expense".equals(type)) {
                            exp += amt;
                            String dayKey = dayName(ts);
                            breakdown.put(dayKey, breakdown.getOrDefault(dayKey, 0.0) + amt);
                        } else if ("income".equals(type)) inc += amt;
                    }
                    data.weeklyExpense   = exp;
                    data.weeklyIncome    = inc;
                    data.weeklySavings   = inc - exp;
                    data.dailyBreakdown  = breakdown;
                    done.run();
                })
                .addOnFailureListener(e -> { Log.e(TAG, "weekly tx", e); done.run(); });
    }

    private void loadMonthlyTransactions(ReportData data, Runnable done) {
        long start = startOfMonth(System.currentTimeMillis());

        db.collection(COL_TRANSACTIONS)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .get()
                .addOnSuccessListener(snaps -> {
                    double exp = 0, inc = 0;
                    Map<String, Double> catTotals = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snaps) {
                        String type = doc.getString("type");
                        Double amt  = doc.getDouble("amount");
                        String cat  = doc.getString("category");
                        if (amt == null) continue;
                        if ("expense".equals(type)) {
                            exp += amt;
                            if (cat != null) catTotals.merge(cat, amt, Double::sum);
                        } else if ("income".equals(type)) inc += amt;
                    }
                    data.monthlyExpense = exp;
                    data.monthlyIncome  = inc;
                    data.monthlySavings = inc - exp;

                    // Build store ranking from categories
                    String[] icons = {"🛒","🚗","🏠","💡","🎮","🏥","📚","👕","🍕","✈️"};
                    List<Map.Entry<String,Double>> sorted = new ArrayList<>(catTotals.entrySet());
                    sorted.sort((a,b) -> Double.compare(b.getValue(), a.getValue()));
                    int idx = 0;
                    for (Map.Entry<String,Double> e : sorted) {
                        if (idx >= 5) break;
                        StoreStat s = new StoreStat();
                        s.name           = e.getKey();
                        s.totalSpent     = e.getValue();
                        s.productCount   = (int)(e.getValue() / 10);
                        s.discountPercent= 10 + idx * 3;
                        s.icon           = icons[idx % icons.length];
                        data.storeRanking.put(e.getKey(), s);
                        idx++;
                    }
                    done.run();
                })
                .addOnFailureListener(e -> { Log.e(TAG, "monthly tx", e); done.run(); });
    }

    private void loadYearlyTransactions(ReportData data, Runnable done) {
        long start = startOfYear(System.currentTimeMillis());

        db.collection(COL_TRANSACTIONS)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", start)
                .get()
                .addOnSuccessListener(snaps -> {
                    double exp = 0, inc = 0;
                    Map<String, Double> monthTotals   = new LinkedHashMap<>();
                    Map<String, Double> productSavings= new HashMap<>();

                    for (QueryDocumentSnapshot doc : snaps) {
                        String type  = doc.getString("type");
                        Double amt   = doc.getDouble("amount");
                        Long   ts    = doc.getLong("timestamp");
                        String pName = doc.getString("productName");
                        if (amt == null || ts == null) continue;
                        String mon = monthName(ts);
                        if ("expense".equals(type)) {
                            exp += amt;
                            monthTotals.merge(mon, amt, Double::sum);
                            if (pName != null) productSavings.merge(pName, amt * 0.2, Double::sum);
                        } else if ("income".equals(type)) inc += amt;
                    }
                    data.yearlyExpense  = exp;
                    data.yearlyIncome   = inc;
                    data.yearlySavings  = inc - exp;
                    data.monthlyTrend   = monthTotals;

                    List<Map.Entry<String,Double>> sorted = new ArrayList<>(productSavings.entrySet());
                    sorted.sort((a,b) -> Double.compare(b.getValue(), a.getValue()));
                    for (int i = 0; i < Math.min(5, sorted.size()); i++) {
                        ProductSavings ps = new ProductSavings();
                        ps.name          = sorted.get(i).getKey();
                        ps.ourPrice      = sorted.get(i).getValue() / 0.2;
                        ps.marketPrice   = ps.ourPrice * 1.28;
                        ps.savingsPercent= 28;
                        data.topSavingProducts.add(ps);
                    }
                    done.run();
                })
                .addOnFailureListener(e -> { Log.e(TAG, "yearly tx", e); done.run(); });
    }

    private void loadStoreDiscounts(ReportData data, Runnable done) {
        String[] stores = {"BazarStore", "Araz Market", "Bravo", "Megastore"};
        AtomicInteger latch = new AtomicInteger(stores.length);

        for (int i = 0; i < stores.length; i++) {
            final int   idx   = i;
            final String name = stores[i];

            DiscountScraperService.scrapeStore(idx, new DiscountScraperService.StoreScraperCallback() {
                @Override public void onProgress(String msg) {}

                @Override
                public void onSuccess(List<DiscountProduct> products, int total) {
                    data.storeDiscounts.put(name, products);
                    mergeDiscountStats(data, products);
                    if (latch.decrementAndGet() == 0) done.run();
                }

                @Override
                public void onError(String err) {
                    data.storeDiscounts.put(name, new ArrayList<>());
                    if (latch.decrementAndGet() == 0) done.run();
                }
            },context);
        }
    }

    private synchronized void mergeDiscountStats(ReportData data, List<DiscountProduct> products) {
        double totalPct = 0; int cnt = 0;
        for (DiscountProduct p : products) {
            if (p.getDiscountPercent() > 0) {
                data.dailyDiscountedProducts.add(p);
                totalPct += p.getDiscountPercent();
                cnt++;
            }
        }
        data.dailyDiscounts    += cnt;
        data.weeklyDiscounts   += cnt;
        data.monthlyDiscounts  += cnt;
        data.yearlyTotalDiscounts += cnt;
        if (cnt > 0)
            data.monthlyAvgDiscount = (data.monthlyAvgDiscount + totalPct / cnt) / 2.0;
    }

    private void loadUserProducts(ReportData data, Runnable done) {
        db.collection(COL_PRODUCTS)
                .whereEqualTo("userId", userId)
                .orderBy("addedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snaps -> {
                    for (DocumentSnapshot doc : snaps.getDocuments())
                        data.userProducts.add(UserProduct.fromDoc(doc));
                    done.run();
                })
                .addOnFailureListener(e -> { Log.e(TAG, "userProducts", e); done.run(); });
    }

    private void loadReceipts(ReportData data, Runnable done) {
        db.collection(COL_RECEIPTS)
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snaps -> {
                    for (DocumentSnapshot doc : snaps.getDocuments())
                        data.receipts.add(Receipt.fromDoc(doc));
                    done.run();
                })
                .addOnFailureListener(e -> { Log.e(TAG, "receipts", e); done.run(); });
    }

    // ─── Date helpers ────────────────────────────────────────────────────────

    private long startOfDay(long ts) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);      c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }
    private long endOfDay(long ts) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY,23); c.set(Calendar.MINUTE,59);
        c.set(Calendar.SECOND,59);       c.set(Calendar.MILLISECOND,999);
        return c.getTimeInMillis();
    }
    private long startOfWeek(long ts) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts);
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0);
        return c.getTimeInMillis();
    }
    private long startOfMonth(long ts) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts);
        c.set(Calendar.DAY_OF_MONTH,1);
        c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0);
        return c.getTimeInMillis();
    }
    private long startOfYear(long ts) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts);
        c.set(Calendar.DAY_OF_YEAR,1);
        c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0);
        return c.getTimeInMillis();
    }
    private String dayName(long ts) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts);
        switch (c.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:    return "B.Ertəsi";
            case Calendar.TUESDAY:   return "Ç.Axşamı";
            case Calendar.WEDNESDAY: return "Çərşənbə";
            case Calendar.THURSDAY:  return "C.Axşamı";
            case Calendar.FRIDAY:    return "Cümə";
            case Calendar.SATURDAY:  return "Şənbə";
            default:                 return "Bazar";
        }
    }
    private String monthName(long ts) {
        return new SimpleDateFormat("MMM", new Locale("az")).format(new Date(ts));
    }
    private String buildDailyTip(double savings) {
        if (savings < 0)  return "⚠️ Xərcləriniz gəlirinizdən çoxdur. Büdcənizi nəzərdən keçirin.";
        if (savings < 20) return "💡 Endirimli məhsullardan istifadə edərək daha çox qənaət edin!";
        return "🎉 Əla! Bu gün yaxşı qənaət edirsiniz.";
    }
}