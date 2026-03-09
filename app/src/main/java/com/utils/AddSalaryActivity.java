package com.utils;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smart_ai_sales.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddSalaryActivity extends AppCompatActivity {

    private EditText etSalaryAmount, etSalaryNote;
    private TextView tvSelectedDate, tvCurrentBalance;
    private Button btnSelectDate, btnAddSalary, btnCancel;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;
    private double currentBalance = 0;
    private long selectedDate = System.currentTimeMillis();

    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_salary);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("az"));

        initViews();
        loadCurrentBalance();
        setupDatePicker();
    }

    private void initViews() {
        etSalaryAmount = findViewById(R.id.etSalaryAmount);
        etSalaryNote = findViewById(R.id.etSalaryNote);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnAddSalary = findViewById(R.id.btnAddSalary);
        btnCancel = findViewById(R.id.btnCancel);

        tvSelectedDate.setText(dateFormat.format(new Date()));

        btnCancel.setOnClickListener(v -> finish());
        btnAddSalary.setOnClickListener(v -> addSalary());
    }

    private void loadCurrentBalance() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double balance = documentSnapshot.getDouble("currentBalance");
                        if (balance != null) {
                            currentBalance = balance;
                            tvCurrentBalance.setText(String.format(Locale.getDefault(),
                                    "₼ %.2f", currentBalance));
                        }
                    }
                });
    }

    private void setupDatePicker() {
        btnSelectDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Maaş tarixini seçin")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDate = selection;
                tvSelectedDate.setText(dateFormat.format(new Date(selection)));
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });
    }

    private void addSalary() {
        String amountStr = etSalaryAmount.getText().toString().trim();

        if (amountStr.isEmpty()) {
            etSalaryAmount.setError("Məbləğ daxil edin");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etSalaryAmount.setError("Məbləğ 0-dan böyük olmalıdır");
                return;
            }
        } catch (NumberFormatException e) {
            etSalaryAmount.setError("Düzgün məbləğ daxil edin");
            return;
        }

        double newBalance = currentBalance + amount;
        String note = etSalaryNote.getText().toString().trim();
        if (note.isEmpty()) {
            note = "Maaş əlavəsi";
        }

        // Maaş əməliyyatını əlavə et
        Map<String, Object> salaryTransaction = new HashMap<>();
        salaryTransaction.put("userId", userId);
        salaryTransaction.put("type", "income");
        salaryTransaction.put("category", "💰 Maaş");
        salaryTransaction.put("productName", "Maaş");
        salaryTransaction.put("amount", amount);
        salaryTransaction.put("quantity", 1);
        salaryTransaction.put("totalAmount", amount);
        salaryTransaction.put("note", note);
        salaryTransaction.put("date", selectedDate);
        salaryTransaction.put("dateString", dateFormat.format(new Date(selectedDate)));
        salaryTransaction.put("timeString", new SimpleDateFormat("HH:mm", new Locale("az")).format(new Date()));
        salaryTransaction.put("timestamp", System.currentTimeMillis());
        salaryTransaction.put("balanceBefore", currentBalance);
        salaryTransaction.put("balanceAfter", newBalance);

        // Firestore-a yaz
        db.runTransaction(firestoreTransaction -> {
            firestoreTransaction.update(db.collection("users").document(userId),
                    "currentBalance", newBalance);
            firestoreTransaction.set(db.collection("transactions").document(), salaryTransaction);
            return null;
        }).addOnSuccessListener(aVoid -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Uğurlu!")
                    .setMessage(String.format("Maaş əlavə edildi\nYeni balans: ₼ %.2f", newBalance))
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Xəta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}