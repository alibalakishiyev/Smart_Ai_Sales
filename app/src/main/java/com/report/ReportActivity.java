package com.report;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.DiscountMarket.model.DiscountProduct;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.smart_ai_sales.R;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportActivity extends AppCompatActivity {

    private ViewPager2  viewPager;
    private TabLayout   tabLayout;
    private ImageView   btnRefresh, btnExport;
    private FloatingActionButton fabShare;
    private ReportPagerAdapter adapter;
    private FirebaseReportService reportSvc;

    // ─── Shared state (for share/export) ────────────────────────────────────
    private FirebaseReportService.ReportData latestData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        reportSvc = new FirebaseReportService(this);
        initViews();
        setupViewPager();
        setupListeners();
    }

    private void initViews() {
        viewPager  = findViewById(R.id.viewPager);
        tabLayout  = findViewById(R.id.tabLayout);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnExport  = findViewById(R.id.btnExport);
        fabShare   = findViewById(R.id.fabShare);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Hesabat");
    }

    private void setupViewPager() {
        adapter = new ReportPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3); // preload tabs

        String[][] tabs = {
                {"📅", "Günlük"},
                {"📆", "Həftəlik"},
                {"📊", "Aylıq"},
                {"🏆", "İllik"},
                {"📦", "Məhsullar"},
                {"🧾", "Qəbzlər"},
        };

        new TabLayoutMediator(tabLayout, viewPager, (tab, pos) -> {
            tab.setText(tabs[pos][0] + " " + tabs[pos][1]);
        }).attach();
    }

    private void setupListeners() {
        btnRefresh.setOnClickListener(v -> refreshData());
        fabShare.setOnClickListener(v -> shareReport());
        btnExport.setOnClickListener(v -> exportReport());
    }

    private void refreshData() {
        Toast.makeText(this, "🔄 Yenilənir…", Toast.LENGTH_SHORT).show();
        reportSvc.loadAllReports(new FirebaseReportService.ReportCallback() {
            @Override public void onSuccess(FirebaseReportService.ReportData d) {
                latestData = d;
                runOnUiThread(() -> {
                    Toast.makeText(ReportActivity.this,
                            "✅ Məlumatlar yeniləndi", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                });
            }
            @Override public void onError(String err) {
                runOnUiThread(() ->
                        Toast.makeText(ReportActivity.this, "Xəta: " + err, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void shareReport() {
        String text = buildShareText();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Hesabatı Paylaş"));
    }

    private void exportReport() {
        // In a real implementation, write to Downloads/Documents
        Toast.makeText(this, "📁 Hesabat saxlanıldı", Toast.LENGTH_SHORT).show();
    }

    private String buildShareText() {
        if (latestData == null) return "Hesabat məlumatları yüklənməyib.";

        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        sb.append("📊 Smart AI Sales — Hesabat\n");
        sb.append("📅 ").append(sdf.format(new Date())).append("\n\n");

        sb.append("── Günlük ──\n");
        sb.append("Xərc:  ₼").append(f2(latestData.dailyExpense)).append("\n");
        sb.append("Gəlir: ₼").append(f2(latestData.dailyIncome)).append("\n");
        sb.append("Qənaət: ₼").append(f2(latestData.dailySavings)).append("\n\n");

        sb.append("── Aylıq ──\n");
        sb.append("Xərc:  ₼").append(f2(latestData.monthlyExpense)).append("\n");
        sb.append("Gəlir: ₼").append(f2(latestData.monthlyIncome)).append("\n");
        sb.append("Endirimlər: ").append(latestData.monthlyDiscounts).append("\n\n");

        // Store discounts summary
        sb.append("── Endirimli mağazalar ──\n");
        for (Map.Entry<String, List<DiscountProduct>> e : latestData.storeDiscounts.entrySet()) {
            long cnt = e.getValue().stream().filter(p -> p.getDiscountPercent() > 0).count();
            sb.append("• ").append(e.getKey()).append(": ").append(cnt).append(" endirim\n");
        }

        return sb.toString();
    }

    private String f2(double v) {
        return String.format(Locale.getDefault(), "%.2f", v);
    }

    // ─── ViewPager Adapter ───────────────────────────────────────────────────

    class ReportPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {

        ReportPagerAdapter(@NonNull AppCompatActivity a) { super(a); }

        @NonNull @Override
        public Fragment createFragment(int pos) {
            switch (pos) {
                case 0: return new DailyReportFragment();
                case 1: return new WeeklyReportFragment();
                case 2: return new MonthlyReportFragment();
                case 3: return new YearlyReportFragment();
                case 4: return new UserProductsFragment();
                case 5: return new ReceiptsFragment();
                default: return new DailyReportFragment();
            }
        }

        @Override public int getItemCount() { return 6; }
    }
}