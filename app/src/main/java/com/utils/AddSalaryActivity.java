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

    private static final String PREF_NAME = "salary_reminder_prefs";
    private static final String KEY_LAST_SALARY_MONTH = "last_salary_month";
    private static final String KEY_LAST_SALARY_YEAR = "last_salary_year";

    private EditText etSalaryAmount, etSalaryNote;
    private TextView tvSelectedDate, tvCurrentBalance, tvWarningMessage;
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
        checkIfSalaryAlreadyAddedThisMonth();
    }

    private void initViews() {
        etSalaryAmount = findViewById(R.id.etSalaryAmount);
        etSalaryNote = findViewById(R.id.etSalaryNote);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        tvWarningMessage = findViewById(R.id.tvWarningMessage);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnAddSalary = findViewById(R.id.btnAddSalary);
        btnCancel = findViewById(R.id.btnCancel);

        tvSelectedDate.setText(dateFormat.format(new Date()));

        btnCancel.setOnClickListener(v -> finish());
        btnAddSalary.setOnClickListener(v -> checkAndAddSalary());
    }

    /**
     * Bu ay artıq maaş əlavə edilibsə yoxla
     */
    private void checkIfSalaryAlreadyAddedThisMonth() {
        if (isSalaryAddedThisMonth()) {
            if (tvWarningMessage != null) {
                tvWarningMessage.setVisibility(View.VISIBLE);
                tvWarningMessage.setText("⚠️ Bu ay üçün artıq maaş əlavə edilib!\nNövbəti ayın 1-5 arasında yeniləyə bilərsiniz.");
            }
            btnAddSalary.setEnabled(false);
            btnAddSalary.setAlpha(0.5f);
        } else {
            if (tvWarningMessage != null) {
                tvWarningMessage.setVisibility(View.GONE);
            }
            btnAddSalary.setEnabled(true);
            btnAddSalary.setAlpha(1.0f);
        }
    }

    /**
     * SharedPreferences-də bu ay üçün maaş əlavə olunub-olunmadığını yoxla
     */
    private boolean isSalaryAddedThisMonth() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);

        int lastMonth = prefs.getInt(KEY_LAST_SALARY_MONTH, -1);
        int lastYear = prefs.getInt(KEY_LAST_SALARY_YEAR, -1);

        return (lastMonth == currentMonth && lastYear == currentYear);
    }

    /**
     * Maaş əlavə edildikdən sonra SharedPreferences-da qeyd et
     */
    private void markSalaryAddedThisMonth() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        Calendar now = Calendar.getInstance();
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_LAST_SALARY_MONTH, now.get(Calendar.MONTH));
        editor.putInt(KEY_LAST_SALARY_YEAR, now.get(Calendar.YEAR));
        editor.apply();

        // SalaryReminderReceiver-i də xəbərdar et (bu ay üçün xatırlatmanı sıfırla)
        SalaryReminderReceiver.onSalaryAdded(this);
    }

    /**
     * Seçilmiş tarixin ayın 1-5 arasında olub-olmadığını yoxla
     */
    private boolean isDateInSalaryPeriod(long dateMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        return dayOfMonth >= 1 && dayOfMonth <= 5;
    }

    /**
     * Maaş əlavə etməzdən əvvəl bütün qaydaları yoxla
     */
    private void checkAndAddSalary() {
        // 1. Bu ay artıq maaş əlavə olunub?
        if (isSalaryAddedThisMonth()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Xəbərdarlıq")
                    .setMessage("Bu ay üçün artıq maaş əlavə edilib!\nNövbəti ayın 1-5 arasında yenidən əlavə edə bilərsiniz.")
                    .setPositiveButton("Başa düşdüm", (dialog, which) -> finish())
                    .show();
            return;
        }

        // 2. Seçilmiş tarix ayın 1-5 arasındadır?
        if (!isDateInSalaryPeriod(selectedDate)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Xəbərdarlıq")
                    .setMessage("Maaş yalnız ayın 1-5 arasında əlavə edilə bilər!\nZəhmət olmasa düzgün tarix seçin.")
                    .setPositiveButton("Tamam", null)
                    .show();
            return;
        }

        // 3. Məbləğ yoxlaması
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

        // Bütün yoxlamalar keçdi -> maaşı əlavə et
        performAddSalary(amount);
    }

    private void performAddSalary(double amount) {
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
            // Maaş əlavə olunduğunu qeyd et
            markSalaryAddedThisMonth();

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Uğurlu!")
                    .setMessage(String.format("Maaş əlavə edildi\nYeni balans: ₼ %.2f", newBalance))
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Xəta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
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

                // Tarix dəyişdikdə, əgər ayın 1-5 arası deyilsə xəbərdarlıq göstər
                if (!isDateInSalaryPeriod(selectedDate)) {
                    Toast.makeText(this, "Xəbərdarlıq: Maaş yalnız ayın 1-5 arasında əlavə edilə bilər!", Toast.LENGTH_LONG).show();
                }
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });
    }
}