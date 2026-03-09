package com.dashboard.dialog;


import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smart_ai_sales.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionsDialog extends DialogFragment {

    private List<Map<String, Object>> transactions;
    private String title;

    public TransactionsDialog(List<Map<String, Object>> transactions, String title) {
        this.transactions = transactions;
        this.title = title;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_transactions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvClose = view.findViewById(R.id.tvClose);
        RecyclerView rvTransactions = view.findViewById(R.id.rvTransactions);
        TextView tvEmpty = view.findViewById(R.id.tvEmpty);

        tvTitle.setText(title);

        if (transactions == null || transactions.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);

            TransactionsAdapter adapter = new TransactionsAdapter(transactions);
            rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
            rvTransactions.setAdapter(adapter);
        }

        tvClose.setOnClickListener(v -> dismiss());
    }

    private class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {
        private List<Map<String, Object>> transactions;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("az"));

        TransactionsAdapter(List<Map<String, Object>> transactions) {
            this.transactions = transactions;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> transaction = transactions.get(position);

            String type = (String) transaction.get("type");
            String category = (String) transaction.get("category");
            Double amount = (Double) transaction.get("amount");
            Long date = (Long) transaction.get("date");
            String note = (String) transaction.get("note");

            // Tip ikonu ve rengi
            if ("income".equals(type)) {
                holder.tvType.setText("Gəlir");
                holder.tvType.setTextColor(holder.itemView.getContext().getColor(R.color.success));
                holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(R.color.success));
                holder.tvAmount.setText(String.format(Locale.getDefault(), "+₼%.2f", amount != null ? amount : 0));
            } else {
                holder.tvType.setText("Xərc");
                holder.tvType.setTextColor(holder.itemView.getContext().getColor(R.color.expense_red));
                holder.tvAmount.setTextColor(holder.itemView.getContext().getColor(R.color.expense_red));
                holder.tvAmount.setText(String.format(Locale.getDefault(), "-₼%.2f", amount != null ? amount : 0));
            }

            // Kateqoriya
            holder.tvCategory.setText(category != null ? category : "Digər");

            // Tarix
            if (date != null) {
                holder.tvDate.setText(dateFormat.format(new Date(date)));
            }

            // Qeyd
            if (note != null && !note.isEmpty()) {
                holder.tvNote.setText(note);
                holder.tvNote.setVisibility(View.VISIBLE);
            } else {
                holder.tvNote.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvType, tvCategory, tvAmount, tvDate, tvNote;

            ViewHolder(View itemView) {
                super(itemView);
                tvType = itemView.findViewById(R.id.tvTransactionType);
                tvCategory = itemView.findViewById(R.id.tvTransactionCategory);
                tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
                tvDate = itemView.findViewById(R.id.tvTransactionDate);
                tvNote = itemView.findViewById(R.id.tvTransactionNote);
            }
        }
    }
}