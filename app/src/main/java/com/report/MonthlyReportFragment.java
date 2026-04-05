package com.report;

import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.smart_ai_sales.R;
import java.text.SimpleDateFormat;
import java.util.*;

public class MonthlyReportFragment extends Fragment {

    private TextView     tvMonth, tvMonthlyExpense, tvMonthlyIncome,
            tvMonthlySavings, tvMonthlyDiscounts, tvMonthlyAvgDiscount;
    private LinearLayout monthlyStoreRanking;
    private ProgressBar  progressBar;
    private FirebaseReportService reportSvc;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_monthly_report, container, false);
        bindViews(root);
        reportSvc = new FirebaseReportService(requireContext());
        loadData();
        return root;
    }

    private void bindViews(View v) {
        tvMonth              = v.findViewById(R.id.tvMonth);
        tvMonthlyExpense     = v.findViewById(R.id.tvMonthlyExpense);
        tvMonthlyIncome      = v.findViewById(R.id.tvMonthlyIncome);
        tvMonthlySavings     = v.findViewById(R.id.tvMonthlySavings);
        tvMonthlyDiscounts   = v.findViewById(R.id.tvMonthlyDiscounts);
        tvMonthlyAvgDiscount = v.findViewById(R.id.tvMonthlyAvgDiscount);
        monthlyStoreRanking  = v.findViewById(R.id.monthlyStoreRanking);
        progressBar          = v.findViewById(R.id.progressBar);

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("az"));
        tvMonth.setText(sdf.format(new Date()).toUpperCase(new Locale("az")));
    }

    private void loadData() {
        setLoading(true);
        reportSvc.loadAllReports(new FirebaseReportService.ReportCallback() {
            @Override public void onSuccess(FirebaseReportService.ReportData d) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    renderSummary(d);
                    renderStoreRanking(d);
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
        tvMonthlyExpense    .setText(fmt(d.monthlyExpense));
        tvMonthlyIncome     .setText(fmt(d.monthlyIncome));
        tvMonthlySavings    .setText(fmt(d.monthlySavings));
        tvMonthlyDiscounts  .setText(String.valueOf(d.monthlyDiscounts));
        tvMonthlyAvgDiscount.setText(
                String.format(Locale.getDefault(), "%.1f%%", d.monthlyAvgDiscount));

        tvMonthlySavings.setTextColor(
                Color.parseColor(d.monthlySavings >= 0 ? "#4CAF50" : "#F44336"));
    }

    private void renderStoreRanking(FirebaseReportService.ReportData d) {
        monthlyStoreRanking.removeAllViews();

        if (d.storeRanking.isEmpty()) {
            TextView tv = new TextView(requireContext());
            tv.setText("📊 Bu ay xərc kateqoriyası yoxdur");
            tv.setTextColor(Color.parseColor("#64748B"));
            tv.setPadding(0, dp(24), 0, dp(24));
            tv.setGravity(Gravity.CENTER);
            monthlyStoreRanking.addView(tv);
            return;
        }

        // Find max for bar scaling
        double maxSpent = 0;
        for (FirebaseReportService.StoreStat s : d.storeRanking.values())
            if (s.totalSpent > maxSpent) maxSpent = s.totalSpent;

        int rank = 1;
        for (FirebaseReportService.StoreStat stat : d.storeRanking.values()) {
            monthlyStoreRanking.addView(buildRankCard(stat, rank++, maxSpent));
        }
    }

    private View buildRankCard(FirebaseReportService.StoreStat stat, int rank, double maxSpent) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(lp);
        card.setRadius(dp(14));
        card.setCardElevation(rank == 1 ? dp(4) : dp(1));
        card.setCardBackgroundColor(rank == 1
                ? Color.parseColor("#1A2F4A")
                : Color.parseColor("#1E293B"));
        if (rank == 1) {
            card.setStrokeColor(Color.parseColor("#3B82F6"));
            card.setStrokeWidth(dp(1));
        }

        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.addView(col);

        // Header row
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        col.addView(header);

        // Rank badge
        TextView rankBadge = new TextView(requireContext());
        rankBadge.setText(rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "  " + rank);
        rankBadge.setTextSize(18);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMarginEnd(dp(12));
        rankBadge.setLayoutParams(rlp);
        header.addView(rankBadge);

        // Store info
        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(info);

        TextView nameTV = new TextView(requireContext());
        nameTV.setText(stat.icon + "  " + stat.name);
        nameTV.setTextColor(Color.parseColor("#F1F5F9"));
        nameTV.setTextSize(14);
        nameTV.setTypeface(nameTV.getTypeface(), Typeface.BOLD);
        info.addView(nameTV);

        TextView subTV = new TextView(requireContext());
        subTV.setText(stat.productCount + " məhsul");
        subTV.setTextColor(Color.parseColor("#64748B"));
        subTV.setTextSize(11);
        info.addView(subTV);

        // Amount
        TextView amtTV = new TextView(requireContext());
        amtTV.setText(fmt(stat.totalSpent));
        amtTV.setTextColor(Color.parseColor("#F87171"));
        amtTV.setTextSize(15);
        amtTV.setTypeface(amtTV.getTypeface(), Typeface.BOLD);
        header.addView(amtTV);

        // Spend bar
        ProgressBar bar = new ProgressBar(requireContext(),
                null, android.R.attr.progressBarStyleHorizontal);
        int pct = maxSpent > 0 ? (int)(stat.totalSpent / maxSpent * 100) : 0;
        bar.setProgress(pct);
        bar.setMax(100);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(5));
        blp.topMargin = dp(10);
        bar.setLayoutParams(blp);

        android.graphics.drawable.LayerDrawable ld =
                (android.graphics.drawable.LayerDrawable) bar.getProgressDrawable();
        String barColor = rank == 1 ? "#3B82F6" : rank == 2 ? "#8B5CF6" : "#EC4899";
        ld.getDrawable(1).setColorFilter(
                Color.parseColor(barColor), android.graphics.PorterDuff.Mode.SRC_IN);
        ld.getDrawable(0).setColorFilter(
                Color.parseColor("#334155"), android.graphics.PorterDuff.Mode.SRC_IN);
        col.addView(bar);

        return card;
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