package com.report;


import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.smart_ai_sales.R;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Shows all user-added products from Firestore.
 * Tapping a product opens a detail BottomSheet.
 */
public class UserProductsFragment extends Fragment {

    private RecyclerView     recyclerView;
    private ProgressBar      progressBar;
    private TextView         tvEmpty, tvProductCount;
    private ProductAdapter   adapter;
    private FirebaseReportService reportSvc;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_user_products, container, false);
        bindViews(root);
        reportSvc = new FirebaseReportService(requireContext());
        loadProducts();
        return root;
    }

    private void bindViews(View v) {
        recyclerView   = v.findViewById(R.id.rvProducts);
        progressBar    = v.findViewById(R.id.progressBar);
        tvEmpty        = v.findViewById(R.id.tvEmpty);
        tvProductCount = v.findViewById(R.id.tvProductCount);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProductAdapter(new ArrayList<>(), this::showProductDetail);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new ItemSpacing(dp(8)));
    }

    private void loadProducts() {
        setLoading(true);
        reportSvc.loadUserProducts(new FirebaseReportService.ReportCallback() {
            @Override public void onSuccess(FirebaseReportService.ReportData d) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    adapter.setData(d.userProducts);
                    int cnt = d.userProducts.size();
                    tvProductCount.setText(cnt + " məhsul");
                    tvEmpty.setVisibility(cnt == 0 ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(cnt == 0 ? View.GONE : View.VISIBLE);
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

    // ─── Detail BottomSheet ──────────────────────────────────────────────────

    private void showProductDetail(FirebaseReportService.UserProduct product) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                R.style.Theme_App_BottomSheetDialog);

        ScrollView sv = new ScrollView(requireContext());
        LinearLayout sheet = new LinearLayout(requireContext());
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(24), dp(24), dp(24), dp(40));
        sheet.setBackgroundColor(Color.parseColor("#0F172A"));
        sv.addView(sheet);

        // Handle bar
        View handle = new View(requireContext());
        handle.setBackgroundColor(Color.parseColor("#334155"));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(48), dp(4));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.bottomMargin = dp(20);
        handle.setLayoutParams(hlp);
        android.graphics.drawable.GradientDrawable hBg =
                new android.graphics.drawable.GradientDrawable();
        hBg.setColor(Color.parseColor("#334155"));
        hBg.setCornerRadius(dp(2));
        handle.setBackground(hBg);
        sheet.addView(handle);

        // Category chip
        TextView catChip = new TextView(requireContext());
        catChip.setText(product.category.toUpperCase(new Locale("az")));
        catChip.setTextColor(Color.parseColor("#60A5FA"));
        catChip.setTextSize(11);
        catChip.setTypeface(catChip.getTypeface(), Typeface.BOLD);
        catChip.setLetterSpacing(0.12f);
        sheet.addView(catChip);

        // Product name
        TextView nameTv = new TextView(requireContext());
        nameTv.setText(product.name);
        nameTv.setTextColor(Color.parseColor("#F1F5F9"));
        nameTv.setTextSize(22);
        nameTv.setTypeface(nameTv.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = dp(6);
        nlp.bottomMargin = dp(20);
        nameTv.setLayoutParams(nlp);
        sheet.addView(nameTv);

        // Divider
        sheet.addView(divider());

        // Info rows
        sheet.addView(infoRow("💰 Qiymət", fmt(product.price)));
        if (product.originalPrice > product.price)
            sheet.addView(infoRow("🏷️ Orijinal qiymət", fmt(product.originalPrice)));
        sheet.addView(infoRow("📦 Miqdar", product.quantity + " " + product.unit));
        if (product.getSavings() > 0) {
            sheet.addView(infoRow("✅ Qənaət",
                    fmt(product.getSavings()) +
                            String.format(Locale.getDefault(), " (%.1f%%)", product.getSavingsPercent())));
        }
        if (product.addedAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("az"));
            sheet.addView(infoRow("📅 Əlavə edilib", sdf.format(new Date(product.addedAt))));
        }

        sheet.addView(divider());

        // Total card
        MaterialCardView totalCard = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(12);
        totalCard.setLayoutParams(tlp);
        totalCard.setRadius(dp(16));
        totalCard.setCardElevation(dp(0));
        totalCard.setCardBackgroundColor(Color.parseColor("#1E3A5F"));

        LinearLayout totalInner = new LinearLayout(requireContext());
        totalInner.setOrientation(LinearLayout.HORIZONTAL);
        totalInner.setPadding(dp(20), dp(16), dp(20), dp(16));
        totalInner.setGravity(Gravity.CENTER_VERTICAL);
        totalCard.addView(totalInner);

        TextView totalLabel = new TextView(requireContext());
        totalLabel.setText("Ümumi dəyər");
        totalLabel.setTextColor(Color.parseColor("#94A3B8"));
        totalLabel.setTextSize(14);
        totalLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        totalInner.addView(totalLabel);

        TextView totalAmt = new TextView(requireContext());
        totalAmt.setText(fmt(product.price * product.quantity));
        totalAmt.setTextColor(Color.parseColor("#4ADE80"));
        totalAmt.setTextSize(20);
        totalAmt.setTypeface(totalAmt.getTypeface(), Typeface.BOLD);
        totalInner.addView(totalAmt);

        sheet.addView(totalCard);
        dialog.setContentView(sv);
        dialog.show();
    }

    private View infoRow(String label, String value) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        row.setLayoutParams(lp);

        TextView lTv = new TextView(requireContext());
        lTv.setText(label);
        lTv.setTextColor(Color.parseColor("#64748B"));
        lTv.setTextSize(13);
        lTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(lTv);

        TextView vTv = new TextView(requireContext());
        vTv.setText(value);
        vTv.setTextColor(Color.parseColor("#E2E8F0"));
        vTv.setTextSize(13);
        vTv.setTypeface(vTv.getTypeface(), Typeface.BOLD);
        row.addView(vTv);
        return row;
    }

    private View divider() {
        View d = new View(requireContext());
        d.setBackgroundColor(Color.parseColor("#1E293B"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = dp(14);
        lp.bottomMargin = dp(4);
        d.setLayoutParams(lp);
        return d;
    }

    // ─── RecyclerView Adapter ────────────────────────────────────────────────

    interface OnProductClick { void onClick(FirebaseReportService.UserProduct p); }

    static class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {
        private List<FirebaseReportService.UserProduct> items;
        private final OnProductClick listener;

        ProductAdapter(List<FirebaseReportService.UserProduct> items, OnProductClick l) {
            this.items    = items;
            this.listener = l;
        }

        void setData(List<FirebaseReportService.UserProduct> data) {
            this.items = data;
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MaterialCardView card = new MaterialCardView(parent.getContext());
            card.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            card.setRadius(dp(parent, 14));
            card.setCardElevation(dp(parent, 2));
            card.setCardBackgroundColor(Color.parseColor("#1E293B"));
            card.setRippleColor(android.content.res.ColorStateList
                    .valueOf(Color.parseColor("#334155")));

            LinearLayout inner = new LinearLayout(parent.getContext());
            inner.setOrientation(LinearLayout.HORIZONTAL);
            inner.setPadding(dp(parent, 16), dp(parent, 14), dp(parent, 16), dp(parent, 14));
            inner.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(inner);

            return new VH(card, inner);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            FirebaseReportService.UserProduct p = items.get(pos);
            h.inner.removeAllViews();
            Context ctx = h.inner.getContext();

            // Category icon circle
            TextView iconTv = new TextView(ctx);
            iconTv.setText(categoryIcon(p.category));
            iconTv.setTextSize(20);
            iconTv.setGravity(Gravity.CENTER);
            int sz = dp(h.inner, 48);
            android.graphics.drawable.GradientDrawable iconBg =
                    new android.graphics.drawable.GradientDrawable();
            iconBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            iconBg.setColor(Color.parseColor("#1A2F4A"));
            iconTv.setBackground(iconBg);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(sz, sz);
            ilp.setMarginEnd(dp(h.inner, 14));
            iconTv.setLayoutParams(ilp);
            h.inner.addView(iconTv);

            // Text column
            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            h.inner.addView(col);

            TextView nameTv = new TextView(ctx);
            nameTv.setText(p.name);
            nameTv.setTextColor(Color.parseColor("#F1F5F9"));
            nameTv.setTextSize(14);
            nameTv.setTypeface(nameTv.getTypeface(), Typeface.BOLD);
            col.addView(nameTv);

            TextView subTv = new TextView(ctx);
            subTv.setText(p.quantity + " " + p.unit + " · " + p.category);
            subTv.setTextColor(Color.parseColor("#64748B"));
            subTv.setTextSize(12);
            col.addView(subTv);

            // Price + savings
            LinearLayout priceCol = new LinearLayout(ctx);
            priceCol.setOrientation(LinearLayout.VERTICAL);
            priceCol.setGravity(Gravity.END);
            h.inner.addView(priceCol);

            TextView priceTv = new TextView(ctx);
            priceTv.setText(String.format(Locale.getDefault(), "₼%.2f", p.price));
            priceTv.setTextColor(Color.parseColor("#4ADE80"));
            priceTv.setTextSize(15);
            priceTv.setTypeface(priceTv.getTypeface(), Typeface.BOLD);
            priceTv.setGravity(Gravity.END);
            priceCol.addView(priceTv);

            if (p.getSavings() > 0) {
                TextView savTv = new TextView(ctx);
                savTv.setText(String.format(Locale.getDefault(),
                        "-%.0f%%", p.getSavingsPercent()));
                savTv.setTextColor(Color.parseColor("#F59E0B"));
                savTv.setTextSize(11);
                savTv.setGravity(Gravity.END);
                priceCol.addView(savTv);
            }

            h.card.setOnClickListener(v -> listener.onClick(p));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            MaterialCardView card;
            LinearLayout     inner;
            VH(MaterialCardView c, LinearLayout i) {
                super(c); card = c; inner = i;
            }
        }

        private String categoryIcon(String cat) {
            if (cat == null) return "📦";
            switch (cat.toLowerCase(Locale.getDefault())) {
                case "qida": case "food":       return "🛒";
                case "elektronika":             return "📱";
                case "geyim": case "paltar":    return "👕";
                case "ev": case "home":         return "🏠";
                case "saglik": case "health":   return "🏥";
                case "idman": case "sport":     return "⚽";
                case "kitab":                   return "📚";
                default:                        return "📦";
            }
        }

        private static int dp(LinearLayout v, int val) {
            return Math.round(val * v.getContext().getResources().getDisplayMetrics().density);
        }
        private static int dp(ViewGroup v, int val) {
            return Math.round(val * v.getContext().getResources().getDisplayMetrics().density);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

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

    static class ItemSpacing extends RecyclerView.ItemDecoration {
        private final int space;
        ItemSpacing(int space) { this.space = space; }
        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect r,
                                   @NonNull View v,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State s) {
            r.bottom = space;
        }
    }
}