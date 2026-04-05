package com.report;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.smart_ai_sales.R;
import java.text.SimpleDateFormat;
import java.util.*;

public class WeeklyReportFragment extends Fragment {

    private TextView     tvWeekRange, tvWeeklyExpense, tvWeeklyIncome,
            tvWeeklySavings, tvWeeklyDiscounts;
    private LinearLayout weeklyStatsContainer;
    private ProgressBar  progressBar;
    private FirebaseReportService reportSvc;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_weekly_report, container, false);
        bindViews(root);
        reportSvc = new FirebaseReportService(requireContext());
        loadData();
        return root;
    }

    private void bindViews(View v) {
        tvWeekRange        = v.findViewById(R.id.tvWeekRange);
        tvWeeklyExpense    = v.findViewById(R.id.tvWeeklyExpense);
        tvWeeklyIncome     = v.findViewById(R.id.tvWeeklyIncome);
        tvWeeklySavings    = v.findViewById(R.id.tvWeeklySavings);
        tvWeeklyDiscounts  = v.findViewById(R.id.tvWeeklyDiscounts);
        weeklyStatsContainer = v.findViewById(R.id.weeklyStatsContainer);
        progressBar        = v.findViewById(R.id.progressBar);

        // Week range label
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", new Locale("az"));
        String start = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, 6);
        tvWeekRange.setText(start + " – " + sdf.format(cal.getTime()));
    }

    private void loadData() {
        setLoading(true);
        reportSvc.loadAllReports(new FirebaseReportService.ReportCallback() {
            @Override public void onSuccess(FirebaseReportService.ReportData d) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    renderSummary(d);
                    renderDailyBreakdown(d.dailyBreakdown);
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
        tvWeeklyExpense   .setText(fmt(d.weeklyExpense));
        tvWeeklyIncome    .setText(fmt(d.weeklyIncome));
        tvWeeklySavings   .setText(fmt(d.weeklySavings));
        tvWeeklyDiscounts .setText(String.valueOf(d.weeklyDiscounts));

        boolean pos = d.weeklySavings >= 0;
        tvWeeklySavings.setTextColor(
                Color.parseColor(pos ? "#4CAF50" : "#F44336"));
    }

    private void renderDailyBreakdown(Map<String, Double> breakdown) {
        weeklyStatsContainer.removeAllViews();
        if (breakdown == null || breakdown.isEmpty()) return;

        double maxAmt = Collections.max(breakdown.values());

        for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
            weeklyStatsContainer.addView(buildDayRow(entry.getKey(), entry.getValue(), maxAmt));
        }
    }

    private View buildDayRow(String day, double amount, double maxAmt) {
        // Card wrapper
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        card.setLayoutParams(lp);
        card.setRadius(dp(12));
        card.setCardElevation(dp(1));
        card.setCardBackgroundColor(Color.parseColor("#1E293B"));

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.addView(row);

        // Top: day name + amount
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(header);

        TextView dayTv = new TextView(requireContext());
        dayTv.setText(day);
        dayTv.setTextColor(Color.parseColor("#94A3B8"));
        dayTv.setTextSize(13);
        dayTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(dayTv);

        TextView amtTv = new TextView(requireContext());
        amtTv.setText(fmt(amount));
        boolean high = amount > (maxAmt * 0.6);
        amtTv.setTextColor(Color.parseColor(high ? "#F87171" : "#4ADE80"));
        amtTv.setTextSize(13);
        amtTv.setTypeface(amtTv.getTypeface(), android.graphics.Typeface.BOLD);
        header.addView(amtTv);

        // Progress bar
        ProgressBar bar = new ProgressBar(requireContext(),
                null, android.R.attr.progressBarStyleHorizontal);
        int ratio = maxAmt > 0 ? (int) (amount / maxAmt * 100) : 0;
        bar.setProgress(ratio);
        bar.setMax(100);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        barLp.topMargin = dp(8);
        bar.setLayoutParams(barLp);

        android.graphics.drawable.LayerDrawable ld =
                (android.graphics.drawable.LayerDrawable) bar.getProgressDrawable();
        ld.getDrawable(1).setColorFilter(
                Color.parseColor(high ? "#F87171" : "#4ADE80"),
                android.graphics.PorterDuff.Mode.SRC_IN);
        ld.getDrawable(0).setColorFilter(
                Color.parseColor("#334155"),
                android.graphics.PorterDuff.Mode.SRC_IN);
        row.addView(bar);

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