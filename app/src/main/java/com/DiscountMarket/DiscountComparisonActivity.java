package com.DiscountMarket;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.DiscountMarket.model.DiscountProduct;
import com.DiscountMarket.service.PriceComparisonService;
import com.data.ProductItem;
import com.smart_ai_sales.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DiscountComparisonActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "discount_alert_channel";
    private static final int NOTIFICATION_ID = 2001;

    private RecyclerView recyclerView;
    private TextView tvStatus, tvSummary;
    private ProgressBar progressBar;
    private ComparisonAdapter adapter;
    private PriceComparisonService comparisonService;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discount_comparison);

        initViews();
        createNotificationChannel();

        comparisonService = new PriceComparisonService(this);
        startComparison();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.recyclerView);
        tvStatus = findViewById(R.id.tvStatus);
        tvSummary = findViewById(R.id.tvSummary);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ComparisonAdapter();
        recyclerView.setAdapter(adapter);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Endirim Xəbərdarlıqları",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Məhsullarınız endirimdə olduqda bildiriş göndərir");
            channel.enableVibration(true);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void startComparison() {
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("🔄 Müqayisə aparılır...");
        tvSummary.setVisibility(View.GONE);

        comparisonService.comparePrices(new PriceComparisonService.ComparisonCallback() {
            @Override
            public void onComparisonComplete(List<PriceComparisonService.ComparisonResult> results) {
                progressBar.setVisibility(View.GONE);

                if (results.isEmpty()) {
                    tvStatus.setText("📭 Sizin məhsullarınızdan heç biri BazarStore-da endirimli deyil");
                    tvSummary.setVisibility(View.GONE);
                } else {
                    double totalSavings = 0;
                    for (PriceComparisonService.ComparisonResult r : results) {
                        totalSavings += r.getSavings();
                    }

                    tvStatus.setText(String.format("🎉 %d məhsul BazarStore-da UCUZ!", results.size()));
                    tvSummary.setVisibility(View.VISIBLE);
                    tvSummary.setText(String.format(Locale.getDefault(),
                            "💰 Ümumi qənaət potensialınız: %.2f AZN\n🏪 BazarStore-da alış-veriş edin!",
                            totalSavings));

                    adapter.setResults(results);
                    sendDiscountNotification(results, totalSavings);
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                tvStatus.setText("❌ Xəta: " + error);
                Toast.makeText(DiscountComparisonActivity.this, error, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProgress(String message) {
                tvStatus.setText(message);
            }
        });
    }

    private void sendDiscountNotification(List<PriceComparisonService.ComparisonResult> results, double totalSavings) {
        if (results.isEmpty()) return;

        String title = String.format("🛍️ %d məhsul ENDİRİMDƏ!", results.size());
        String content = String.format("BazarStore-da %d məhsulunuz UCUZ! Ümumi qənaət: %.2f AZN",
                results.size(), totalSavings);

        Intent intent = new Intent(this, DiscountComparisonActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    // ==================== ADAPTER ====================

    private class ComparisonAdapter extends RecyclerView.Adapter<ComparisonAdapter.ViewHolder> {
        private List<PriceComparisonService.ComparisonResult> results = new ArrayList<>();

        public void setResults(List<PriceComparisonService.ComparisonResult> results) {
            this.results = results;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_price_comparison, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PriceComparisonService.ComparisonResult result = results.get(position);
            ProductItem local = result.getLocalProduct();
            DiscountProduct bazar = result.getBazarProduct();

            holder.tvProductName.setText(local.getName());
            holder.tvLocalPrice.setText(String.format("💰 Sizin qiymət: ₼%.2f", result.getLocalPrice()));
            holder.tvBazarPrice.setText(String.format("🏪 BazarStore: ₼%.2f", result.getBazarPrice()));
            holder.tvSavings.setText(String.format("✅ Qənaət: ₼%.2f (%.0f%%)",
                    result.getSavings(), result.getSavingsPercent()));
            holder.tvStore.setText(String.format("🏷️ Endirim: %.0f%% - BazarStore",
                    bazar.getDiscountPercent()));
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvProductName, tvLocalPrice, tvBazarPrice, tvSavings, tvStore;

            ViewHolder(View itemView) {
                super(itemView);
                tvProductName = itemView.findViewById(R.id.tvProductName);
                tvLocalPrice = itemView.findViewById(R.id.tvLocalPrice);
                tvBazarPrice = itemView.findViewById(R.id.tvBazarPrice);
                tvSavings = itemView.findViewById(R.id.tvSavings);
                tvStore = itemView.findViewById(R.id.tvStore);
            }
        }
    }
}