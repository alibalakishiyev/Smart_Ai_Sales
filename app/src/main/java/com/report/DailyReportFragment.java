package com.report;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.smart_ai_sales.R;
import java.text.SimpleDateFormat;
import java.util.*;

public class DailyReportFragment extends Fragment {

    // ─── Views ───────────────────────────────────────────────────────────────
    private TextView tvDate, tvDailyExpense, tvDailyIncome, tvDailySavings;
    private TextView tvDailyDiscounts, tvDailyTip, tvDiscountHeader;
    private LinearLayout discountsContainer;
    private ImageButton  btnPrevDay, btnNextDay, btnToday;
    private ProgressBar  progressBar;
    private View         savingsIndicator;

    // ─── State ───────────────────────────────────────────────────────────────
    private Calendar               currentCal   = Calendar.getInstance();
    private FirebaseReportService  reportSvc;
    private final SimpleDateFormat dateFmt =
            new SimpleDateFormat("dd MMMM yyyy, EEEE", new Locale("az"));

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_daily_report, container, false);
        bindViews(root);
        reportSvc = new FirebaseReportService(requireContext());
        setupNavigation();
        loadData();
        return root;
    }

    // ─── View binding ────────────────────────────────────────────────────────

    private void bindViews(View v) {
        tvDate           = v.findViewById(R.id.tvDate);
        tvDailyExpense   = v.findViewById(R.id.tvDailyExpense);
        tvDailyIncome    = v.findViewById(R.id.tvDailyIncome);
        tvDailySavings   = v.findViewById(R.id.tvDailySavings);
        tvDailyDiscounts = v.findViewById(R.id.tvDailyDiscounts);
        tvDailyTip       = v.findViewById(R.id.tvDailyTip);
        tvDiscountHeader = v.findViewById(R.id.tvDiscountHeader);
        discountsContainer = v.findViewById(R.id.dailyDiscountsContainer);
        btnPrevDay       = v.findViewById(R.id.btnPrevDay);
        btnNextDay       = v.findViewById(R.id.btnNextDay);
        btnToday         = v.findViewById(R.id.btnToday);
        progressBar      = v.findViewById(R.id.progressBar);
        savingsIndicator = v.findViewById(R.id.savingsIndicator);
    }

    // ─── Date navigation ─────────────────────────────────────────────────────

    private void setupNavigation() {
        updateDateLabel();

        btnPrevDay.setOnClickListener(v -> {
            currentCal.add(Calendar.DAY_OF_YEAR, -1);
            updateDateLabel();
            loadData();
        });
        btnNextDay.setOnClickListener(v -> {
            // Don't allow future dates
            Calendar now = Calendar.getInstance();
            if (!isSameDay(currentCal, now)) {
                currentCal.add(Calendar.DAY_OF_YEAR, 1);
                updateDateLabel();
                loadData();
            }
        });
        btnToday.setOnClickListener(v -> {
            currentCal = Calendar.getInstance();
            updateDateLabel();
            loadData();
        });
    }

    private void updateDateLabel() {
        tvDate.setText(dateFmt.format(currentCal.getTime()));
        // Disable next button on today
        Calendar now = Calendar.getInstance();
        btnNextDay.setAlpha(isSameDay(currentCal, now) ? 0.4f : 1f);
        btnNextDay.setEnabled(!isSameDay(currentCal, now));
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    // ─── Data loading ────────────────────────────────────────────────────────

    private void loadData() {
        setLoading(true);
        discountsContainer.removeAllViews();

        reportSvc.loadAllReports(new FirebaseReportService.ReportCallback() {
            @Override public void onSuccess(FirebaseReportService.ReportData data) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    renderStats(data);
                    renderDiscounts(data);
                    renderTip(data);
                    setLoading(false);
                });
            }
            @Override public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    showError(error);
                    setLoading(false);
                });
            }
        });
    }

    // ─── Rendering ───────────────────────────────────────────────────────────

    private void renderStats(FirebaseReportService.ReportData data) {
        tvDailyExpense.setText(formatMoney(data.dailyExpense));
        tvDailyIncome .setText(formatMoney(data.dailyIncome));
        tvDailySavings.setText(formatMoney(data.dailySavings));
        tvDailyDiscounts.setText(String.valueOf(data.dailyDiscounts));

        // Color savings based on positive/negative
        boolean positive = data.dailySavings >= 0;
        int savColor = positive ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
        tvDailySavings.setTextColor(savColor);
        if (savingsIndicator != null)
            savingsIndicator.setBackgroundColor(savColor);
    }

    private void renderDiscounts(FirebaseReportService.ReportData data) {
        discountsContainer.removeAllViews();
        List<com.DiscountMarket.model.DiscountProduct> products = data.dailyDiscountedProducts;

        if (tvDiscountHeader != null)
            tvDiscountHeader.setText(products.size() + " endirimli məhsul tapıldı");

        if (products.isEmpty()) {
            addEmptyView("📭 Bu gün endirimli məhsul yoxdur");
            return;
        }

        // Show top 8 discounts sorted by discount percent
        products.sort((a, b) -> Double.compare(b.getDiscountPercent(), a.getDiscountPercent()));
        int limit = Math.min(8, products.size());
        for (int i = 0; i < limit; i++) {
            discountsContainer.addView(buildDiscountCard(products.get(i)));
        }

        if (products.size() > limit) {
            addShowMoreChip(products.size() - limit);
        }
    }

    private void renderTip(FirebaseReportService.ReportData data) {
        if (tvDailyTip == null) return;
        tvDailyTip.setText(data.dailyTip);

        int color;
        if (data.dailySavings < 0)   color = Color.parseColor("#F44336");
        else if (data.dailySavings < 20) color = Color.parseColor("#FF9800");
        else                             color = Color.parseColor("#4CAF50");
        tvDailyTip.setTextColor(color);
    }

    // ─── Card builders ───────────────────────────────────────────────────────

    private View buildDiscountCard(com.DiscountMarket.model.DiscountProduct p) {
        // Root card
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(lp);
        card.setRadius(dp(16));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(Color.parseColor("#1E293B"));
        card.setStrokeColor(Color.parseColor("#334155"));
        card.setStrokeWidth(dp(1));

        // Inner horizontal layout
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(row);

        // Badge (discount %)
        TextView badge = new TextView(requireContext());
        badge.setText(String.format(Locale.getDefault(), "-%.0f%%", p.getDiscountPercent()));
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(11);
        badge.setTypeface(badge.getTypeface(), android.graphics.Typeface.BOLD);
        badge.setBackgroundColor(Color.parseColor("#E53E3E"));
        badge.setPadding(dp(7), dp(4), dp(7), dp(4));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.setMarginEnd(dp(12));
        badge.setLayoutParams(badgeLp);
        // Round badge
        android.graphics.drawable.GradientDrawable badgeBg =
                new android.graphics.drawable.GradientDrawable();
        badgeBg.setColor(Color.parseColor("#E53E3E"));
        badgeBg.setCornerRadius(dp(8));
        badge.setBackground(badgeBg);
        row.addView(badge);

        // Info column
        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(info);

        // Store name
        TextView store = new TextView(requireContext());
        store.setText("🏪  " + p.getStoreName());
        store.setTextColor(Color.parseColor("#94A3B8"));
        store.setTextSize(11);
        info.addView(store);

        // Product name
        String name = p.getProductName();
        if (name.length() > 32) name = name.substring(0, 29) + "…";
        TextView pname = new TextView(requireContext());
        pname.setText(name);
        pname.setTextColor(Color.parseColor("#F1F5F9"));
        pname.setTextSize(13);
        pname.setTypeface(pname.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.topMargin = dp(2);
        pname.setLayoutParams(nameLp);
        info.addView(pname);

        // Price column
        LinearLayout prices = new LinearLayout(requireContext());
        prices.setOrientation(LinearLayout.VERTICAL);
        prices.setGravity(Gravity.END);
        row.addView(prices);

        TextView discPrice = new TextView(requireContext());
        discPrice.setText(String.format(Locale.getDefault(), "₼%.2f", p.getDiscountPrice()));
        discPrice.setTextColor(Color.parseColor("#4ADE80"));
        discPrice.setTextSize(15);
        discPrice.setTypeface(discPrice.getTypeface(), android.graphics.Typeface.BOLD);
        discPrice.setGravity(Gravity.END);
        prices.addView(discPrice);

        if (p.getOriginalPrice() > 0 && p.getOriginalPrice() != p.getDiscountPrice()) {
            TextView origPrice = new TextView(requireContext());
            origPrice.setText(String.format(Locale.getDefault(), "₼%.2f", p.getOriginalPrice()));
            origPrice.setTextColor(Color.parseColor("#64748B"));
            origPrice.setTextSize(11);
            origPrice.setGravity(Gravity.END);
            origPrice.setPaintFlags(
                    origPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            prices.addView(origPrice);
        }

        return card;
    }

    private void addEmptyView(String msg) {
        TextView tv = new TextView(requireContext());
        tv.setText(msg);
        tv.setTextColor(Color.parseColor("#64748B"));
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(32), 0, dp(32));
        discountsContainer.addView(tv);
    }

    private void addShowMoreChip(int remaining) {
        Chip chip = new Chip(requireContext());
        chip.setText("+ " + remaining + " məhsul daha");
        chip.setChipBackgroundColor(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#1E3A5F")));
        chip.setTextColor(Color.parseColor("#60A5FA"));
        chip.setClickable(false);
        discountsContainer.addView(chip);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String msg) {
        Toast.makeText(requireContext(), "Xəta: " + msg, Toast.LENGTH_SHORT).show();
    }

    private String formatMoney(double v) {
        return String.format(Locale.getDefault(), "₼%.2f", v);
    }

    private int dp(int val) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(val * density);
    }
}