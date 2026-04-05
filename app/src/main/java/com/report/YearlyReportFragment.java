package com.report;

import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.smart_ai_sales.R;
import java.util.*;

public class YearlyReportFragment extends Fragment {

    private TextView     tvYear, tvYearlyExpense, tvYearlyIncome,
            tvYearlySavings, tvYearlyTotalDiscounts;
    private LinearLayout yearlyTrendContainer, yearlyTopProducts;
    private ProgressBar  progressBar;
    private FirebaseReportService reportSvc;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_yearly_report, container, false);
        bindViews(root);
        reportSvc = new FirebaseReportService(requireContext());
        loadData();
        return root;
    }

    private void bindViews(View v) {
        tvYear                  = v.findViewById(R.id.tvYear);
        tvYearlyExpense         = v.findViewById(R.id.tvYearlyExpense);
        tvYearlyIncome          = v.findViewById(R.id.tvYearlyIncome);
        tvYearlySavings         = v.findViewById(R.id.tvYearlySavings);
        tvYearlyTotalDiscounts  = v.findViewById(R.id.tvYearlyTotalDiscounts);
        yearlyTrendContainer    = v.findViewById(R.id.yearlyTrendContainer);
        yearlyTopProducts       = v.findViewById(R.id.yearlyTopProducts);
        progressBar             = v.findViewById(R.id.progressBar);

        tvYear.setText(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
    }

    private void loadData() {
        setLoading(true);
        reportSvc.loadAllReports(new FirebaseReportService.ReportCallback() {
            @Override public void onSuccess(FirebaseReportService.ReportData d) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    renderSummary(d);
                    renderMonthlyTrend(d.monthlyTrend);
                    renderTopProducts(d.topSavingProducts);
                    setLoading(false);
                });
            }
            @Override public void onError(String err) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
                    setLoading(false);
                });
            }
        });
    }

    private void renderSummary(FirebaseReportService.ReportData d) {
        tvYearlyExpense        .setText(fmt(d.yearlyExpense));
        tvYearlyIncome         .setText(fmt(d.yearlyIncome));
        tvYearlySavings        .setText(fmt(d.yearlySavings));
        tvYearlyTotalDiscounts .setText(String.valueOf(d.yearlyTotalDiscounts));

        tvYearlySavings.setTextColor(
                Color.parseColor(d.yearlySavings >= 0 ? "#4CAF50" : "#F44336"));
    }

    private void renderMonthlyTrend(Map<String, Double> trend) {
        yearlyTrendContainer.removeAllViews();
        if (trend == null || trend.isEmpty()) {
            addEmpty(yearlyTrendContainer, "📈 Bu il üçün məlumat yoxdur");
            return;
        }
        double max = Collections.max(trend.values());
        for (Map.Entry<String, Double> e : trend.entrySet()) {
            yearlyTrendContainer.addView(buildTrendRow(e.getKey(), e.getValue(), max));
        }
    }

    private View buildTrendRow(String month, double amount, double max) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lp);
        row.setPadding(dp(4), dp(4), dp(4), dp(4));

        // Month label
        TextView mTv = new TextView(requireContext());
        mTv.setText(month);
        mTv.setTextColor(Color.parseColor("#94A3B8"));
        mTv.setTextSize(12);
        mTv.setLayoutParams(new LinearLayout.LayoutParams(dp(50),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(mTv);

        // Bar container
        FrameLayout barFrame = new FrameLayout(requireContext());
        barFrame.setLayoutParams(new LinearLayout.LayoutParams(0,
                dp(24), 1f));
        row.addView(barFrame);

        // Background track
        View track = new View(requireContext());
        track.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        android.graphics.drawable.GradientDrawable trackBg =
                new android.graphics.drawable.GradientDrawable();
        trackBg.setColor(Color.parseColor("#1E293B"));
        trackBg.setCornerRadius(dp(12));
        track.setBackground(trackBg);
        barFrame.addView(track);

        // Filled bar
        View fill = new View(requireContext());
        int w = barFrame.getLayoutParams().width;
        int fillPct = max > 0 ? (int)(amount / max * 100) : 0;
        FrameLayout.LayoutParams fillLp = new FrameLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT);  // 0 here, set after layout
        fill.setLayoutParams(fillLp);

        // Use percentage via weight trick — set programmatically after measure
        // Instead, use a horizontal ProgressBar styled similarly:
        ProgressBar bar = new ProgressBar(requireContext(),
                null, android.R.attr.progressBarStyleHorizontal);
        bar.setProgress(fillPct);
        bar.setMax(100);
        bar.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Determine color by relative amount
        String color = fillPct > 75 ? "#F87171" : fillPct > 40 ? "#FBBF24" : "#4ADE80";
        android.graphics.drawable.LayerDrawable ld =
                (android.graphics.drawable.LayerDrawable) bar.getProgressDrawable();
        ld.getDrawable(1).setColorFilter(
                Color.parseColor(color), android.graphics.PorterDuff.Mode.SRC_IN);
        ld.getDrawable(0).setColorFilter(
                Color.parseColor("#1E293B"), android.graphics.PorterDuff.Mode.SRC_IN);
        barFrame.addView(bar);

        // Amount label
        TextView aTv = new TextView(requireContext());
        aTv.setText(String.format(Locale.getDefault(), "₼%.0f", amount));
        aTv.setTextColor(Color.parseColor("#CBD5E1"));
        aTv.setTextSize(11);
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aLp.setMarginStart(dp(8));
        aTv.setLayoutParams(aLp);
        row.addView(aTv);

        return row;
    }

    private void renderTopProducts(List<FirebaseReportService.ProductSavings> products) {
        yearlyTopProducts.removeAllViews();
        if (products == null || products.isEmpty()) {
            addEmpty(yearlyTopProducts, "🏆 Qənaət məhsulları yoxdur");
            return;
        }
        int rank = 1;
        for (FirebaseReportService.ProductSavings ps : products) {
            yearlyTopProducts.addView(buildProductCard(ps, rank++));
        }
    }

    private View buildProductCard(FirebaseReportService.ProductSavings ps, int rank) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(lp);
        card.setRadius(dp(14));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(Color.parseColor("#1E293B"));

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(row);

        // Rank
        TextView rankTv = new TextView(requireContext());
        rankTv.setText(rank <= 3
                ? (rank == 1 ? "🥇" : rank == 2 ? "🥈" : "🥉")
                : "#" + rank);
        rankTv.setTextSize(rank <= 3 ? 20 : 14);
        rankTv.setTextColor(Color.parseColor("#F59E0B"));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMarginEnd(dp(12));
        rankTv.setLayoutParams(rlp);
        row.addView(rankTv);

        // Name
        TextView nameTv = new TextView(requireContext());
        String name = ps.name.length() > 22 ? ps.name.substring(0, 19) + "…" : ps.name;
        nameTv.setText(name);
        nameTv.setTextColor(Color.parseColor("#F1F5F9"));
        nameTv.setTextSize(13);
        nameTv.setTypeface(nameTv.getTypeface(), Typeface.BOLD);
        nameTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(nameTv);

        // Prices column
        LinearLayout priceCol = new LinearLayout(requireContext());
        priceCol.setOrientation(LinearLayout.VERTICAL);
        priceCol.setGravity(Gravity.END);
        row.addView(priceCol);

        TextView ourPriceTv = new TextView(requireContext());
        ourPriceTv.setText("Biz: " + fmt(ps.ourPrice));
        ourPriceTv.setTextColor(Color.parseColor("#4ADE80"));
        ourPriceTv.setTextSize(12);
        ourPriceTv.setTypeface(ourPriceTv.getTypeface(), Typeface.BOLD);
        ourPriceTv.setGravity(Gravity.END);
        priceCol.addView(ourPriceTv);

        TextView mktPriceTv = new TextView(requireContext());
        mktPriceTv.setText("Bazar: " + fmt(ps.marketPrice));
        mktPriceTv.setTextColor(Color.parseColor("#64748B"));
        mktPriceTv.setTextSize(11);
        mktPriceTv.setGravity(Gravity.END);
        mktPriceTv.setPaintFlags(
                mktPriceTv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        priceCol.addView(mktPriceTv);

        TextView savTv = new TextView(requireContext());
        savTv.setText(String.format(Locale.getDefault(),
                "+%.0f%% qənaət", ps.savingsPercent));
        savTv.setTextColor(Color.parseColor("#FBBF24"));
        savTv.setTextSize(10);
        savTv.setGravity(Gravity.END);
        priceCol.addView(savTv);

        return card;
    }

    private void addEmpty(LinearLayout container, String msg) {
        TextView tv = new TextView(requireContext());
        tv.setText(msg);
        tv.setTextColor(Color.parseColor("#64748B"));
        tv.setTextSize(13);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(24), 0, dp(24));
        container.addView(tv);
    }

    private void setLoading(boolean b) {
        if (progressBar != null)
            progressBar.setVisibility(b ? View.VISIBLE : View.GONE);
    }
    private String fmt(double v) {
        return String.format(Locale.getDefault(), "₼%.2f", v);
    }
    private int dp(int v) {
        return Math.round(v * requireContext().getResources().getDisplayMetrics().density);
    }
}