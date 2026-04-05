package com.report;


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
 * Lists all user receipts from Firestore.
 * Tapping a receipt opens a full detailed BottomSheet with all line items.
 */
public class ReceiptsFragment extends Fragment {

    private RecyclerView   recyclerView;
    private ProgressBar    progressBar;
    private TextView       tvEmpty, tvReceiptCount;
    private ReceiptAdapter adapter;
    private FirebaseReportService reportSvc;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_receipts, container, false);
        bindViews(root);
        reportSvc = new FirebaseReportService(requireContext());
        loadReceipts();
        return root;
    }

    private void bindViews(View v) {
        recyclerView    = v.findViewById(R.id.rvReceipts);
        progressBar     = v.findViewById(R.id.progressBar);
        tvEmpty         = v.findViewById(R.id.tvEmpty);
        tvReceiptCount  = v.findViewById(R.id.tvReceiptCount);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReceiptAdapter(new ArrayList<>(), this::showReceiptDetail);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new UserProductsFragment.ItemSpacing(dp(8)));
    }

    private void loadReceipts() {
        setLoading(true);
        reportSvc.loadReceipts(new FirebaseReportService.ReportCallback() {
            @Override public void onSuccess(FirebaseReportService.ReportData d) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    adapter.setData(d.receipts);
                    int cnt = d.receipts.size();
                    tvReceiptCount.setText(cnt + " qəbz");
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

    // ─── Receipt detail BottomSheet ──────────────────────────────────────────

    private void showReceiptDetail(FirebaseReportService.Receipt receipt) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                R.style.Theme_App_BottomSheetDialog);

        ScrollView sv = new ScrollView(requireContext());
        LinearLayout sheet = new LinearLayout(requireContext());
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(24), dp(20), dp(24), dp(48));
        sheet.setBackgroundColor(Color.parseColor("#0F172A"));
        sv.addView(sheet);

        // Handle
        View handle = new View(requireContext());
        android.graphics.drawable.GradientDrawable hBg =
                new android.graphics.drawable.GradientDrawable();
        hBg.setColor(Color.parseColor("#334155"));
        hBg.setCornerRadius(dp(2));
        handle.setBackground(hBg);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(48), dp(4));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.bottomMargin = dp(20);
        handle.setLayoutParams(hlp);
        sheet.addView(handle);

        // Header
        TextView storeTv = new TextView(requireContext());
        storeTv.setText("🏪  " + receipt.storeName);
        storeTv.setTextColor(Color.parseColor("#60A5FA"));
        storeTv.setTextSize(13);
        sheet.addView(storeTv);

        if (receipt.note != null && !receipt.note.isEmpty()) {
            TextView noteTv = new TextView(requireContext());
            noteTv.setText(receipt.note);
            noteTv.setTextColor(Color.parseColor("#94A3B8"));
            noteTv.setTextSize(12);
            sheet.addView(noteTv);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("az"));
        TextView dateTv = new TextView(requireContext());
        dateTv.setText("📅  " + (receipt.date > 0 ? sdf.format(new Date(receipt.date)) : "—"));
        dateTv.setTextColor(Color.parseColor("#64748B"));
        dateTv.setTextSize(12);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(4);
        dlp.bottomMargin = dp(20);
        dateTv.setLayoutParams(dlp);
        sheet.addView(dateTv);

        // Items header
        sheet.addView(sectionLabel("Məhsullar"));
        sheet.addView(divider());

        // Line items
        if (receipt.items.isEmpty()) {
            TextView noItems = new TextView(requireContext());
            noItems.setText("Məhsul siyahısı yoxdur");
            noItems.setTextColor(Color.parseColor("#475569"));
            noItems.setTextSize(13);
            noItems.setPadding(0, dp(12), 0, dp(12));
            sheet.addView(noItems);
        } else {
            for (FirebaseReportService.LineItem li : receipt.items) {
                sheet.addView(buildLineItem(li));
            }
        }

        sheet.addView(divider());

        // Total row
        LinearLayout totalRow = new LinearLayout(requireContext());
        totalRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(12);
        totalRow.setLayoutParams(tlp);

        TextView totalLabel = new TextView(requireContext());
        totalLabel.setText("Ümumi");
        totalLabel.setTextColor(Color.parseColor("#F1F5F9"));
        totalLabel.setTextSize(16);
        totalLabel.setTypeface(totalLabel.getTypeface(), Typeface.BOLD);
        totalLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        totalRow.addView(totalLabel);

        TextView totalAmt = new TextView(requireContext());
        totalAmt.setText(fmt(receipt.total));
        totalAmt.setTextColor(Color.parseColor("#4ADE80"));
        totalAmt.setTextSize(20);
        totalAmt.setTypeface(totalAmt.getTypeface(), Typeface.BOLD);
        totalRow.addView(totalAmt);
        sheet.addView(totalRow);

        dialog.setContentView(sv);
        dialog.show();
    }

    private View buildLineItem(FirebaseReportService.LineItem li) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        row.setLayoutParams(lp);

        // Bullet
        TextView bullet = new TextView(requireContext());
        bullet.setText("•");
        bullet.setTextColor(Color.parseColor("#3B82F6"));
        bullet.setTextSize(16);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.setMarginEnd(dp(10));
        bullet.setLayoutParams(blp);
        row.addView(bullet);

        // Name + qty
        LinearLayout info = new LinearLayout(requireContext());
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(info);

        TextView nameTv = new TextView(requireContext());
        nameTv.setText(li.name);
        nameTv.setTextColor(Color.parseColor("#E2E8F0"));
        nameTv.setTextSize(13);
        info.addView(nameTv);

        if (li.qty > 1) {
            TextView qtyTv = new TextView(requireContext());
            qtyTv.setText(li.qty + " × " + fmt(li.price));
            qtyTv.setTextColor(Color.parseColor("#64748B"));
            qtyTv.setTextSize(11);
            info.addView(qtyTv);
        }

        // Line total
        TextView lineTot = new TextView(requireContext());
        lineTot.setText(fmt(li.total()));
        lineTot.setTextColor(Color.parseColor("#94A3B8"));
        lineTot.setTextSize(13);
        row.addView(lineTot);

        return row;
    }

    private View sectionLabel(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text.toUpperCase(new Locale("az")));
        tv.setTextColor(Color.parseColor("#475569"));
        tv.setTextSize(11);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        lp.bottomMargin = dp(4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private View divider() {
        View d = new View(requireContext());
        d.setBackgroundColor(Color.parseColor("#1E293B"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(4);
        d.setLayoutParams(lp);
        return d;
    }

    // ─── RecyclerView Adapter ────────────────────────────────────────────────

    interface OnReceiptClick { void onClick(FirebaseReportService.Receipt r); }

    static class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.VH> {
        private List<FirebaseReportService.Receipt> items;
        private final OnReceiptClick listener;
        private final SimpleDateFormat sdf =
                new SimpleDateFormat("dd MMM yyyy", new Locale("az"));

        ReceiptAdapter(List<FirebaseReportService.Receipt> items, OnReceiptClick l) {
            this.items = items; this.listener = l;
        }

        void setData(List<FirebaseReportService.Receipt> data) {
            this.items = data; notifyDataSetChanged();
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
            FirebaseReportService.Receipt r = items.get(pos);
            h.inner.removeAllViews();
            android.content.Context ctx = h.inner.getContext();

            // Icon
            TextView icon = new TextView(ctx);
            icon.setText("🧾");
            icon.setTextSize(24);
            icon.setGravity(Gravity.CENTER);
            int sz = dp(h.inner, 48);
            android.graphics.drawable.GradientDrawable iconBg =
                    new android.graphics.drawable.GradientDrawable();
            iconBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            iconBg.setColor(Color.parseColor("#172033"));
            icon.setBackground(iconBg);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(sz, sz);
            ilp.setMarginEnd(dp(h.inner, 14));
            icon.setLayoutParams(ilp);
            h.inner.addView(icon);

            // Info
            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            h.inner.addView(col);

            TextView storeTv = new TextView(ctx);
            storeTv.setText(r.storeName.isEmpty() ? "Mağaza" : r.storeName);
            storeTv.setTextColor(Color.parseColor("#F1F5F9"));
            storeTv.setTextSize(14);
            storeTv.setTypeface(storeTv.getTypeface(), Typeface.BOLD);
            col.addView(storeTv);

            TextView subTv = new TextView(ctx);
            String dateLbl = r.date > 0 ? sdf.format(new Date(r.date)) : "—";
            subTv.setText(r.items.size() + " məhsul · " + dateLbl);
            subTv.setTextColor(Color.parseColor("#64748B"));
            subTv.setTextSize(12);
            col.addView(subTv);

            // Total
            TextView totalTv = new TextView(ctx);
            totalTv.setText(String.format(Locale.getDefault(), "₼%.2f", r.total));
            totalTv.setTextColor(Color.parseColor("#4ADE80"));
            totalTv.setTextSize(16);
            totalTv.setTypeface(totalTv.getTypeface(), Typeface.BOLD);
            h.inner.addView(totalTv);

            // Chevron
            TextView chev = new TextView(ctx);
            chev.setText("›");
            chev.setTextColor(Color.parseColor("#334155"));
            chev.setTextSize(22);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.setMarginStart(dp(h.inner, 8));
            chev.setLayoutParams(clp);
            h.inner.addView(chev);

            h.card.setOnClickListener(v -> listener.onClick(r));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            MaterialCardView card; LinearLayout inner;
            VH(MaterialCardView c, LinearLayout i) { super(c); card=c; inner=i; }
        }

        private static int dp(LinearLayout v, int val) {
            return Math.round(val * v.getContext().getResources().getDisplayMetrics().density);
        }
        private static int dp(ViewGroup v, int val) {
            return Math.round(val * v.getContext().getResources().getDisplayMetrics().density);
        }
    }

    private void setLoading(boolean b) {
        if (progressBar != null) progressBar.setVisibility(b ? View.VISIBLE : View.GONE);
    }
    private String fmt(double v) { return String.format(Locale.getDefault(), "₼%.2f", v); }
    private int dp(int v) {
        return Math.round(v * requireContext().getResources().getDisplayMetrics().density);
    }
}
