package com.authentication;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import com.google.firebase.auth.UserProfileChangeRequest;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.smart_ai_sales.R;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class UserProfile extends AppCompatActivity {

    private TextView tvFullName, tvEmail, tvEmailStatus, tvUserId, tvMemberSince;
    private EditText etFirstName, etLastName, etGender, etMaritalStatus, etPhoneNumber, etBirthDate, etAddress;
    private CardView cardChangePassword, cardChangeEmail;
    private MaterialButton btnSaveProfile, btnDeleteAccount;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        initViews();
        loadUserData();
        setupClickListeners();
        setupSpinners();
    }

    private void initViews() {
        tvFullName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvEmailStatus = findViewById(R.id.tvEmailStatus);
        tvUserId = findViewById(R.id.tvUserId);
        tvMemberSince = findViewById(R.id.tvMemberSince);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etGender = findViewById(R.id.etGender);
        etMaritalStatus = findViewById(R.id.etMaritalStatus);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etBirthDate = findViewById(R.id.etBirthDate);
        etAddress = findViewById(R.id.etAddress);

        cardChangePassword = findViewById(R.id.cardChangePassword);
        cardChangeEmail = findViewById(R.id.cardChangeEmail);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
    }

    private void loadUserData() {
        if (currentUser != null) {
            // Load from SharedPreferences
            String firstName = sharedPreferences.getString("first_name", "");
            String lastName = sharedPreferences.getString("last_name", "");
            String fullName = firstName + " " + lastName;
            if (fullName.trim().isEmpty()) {
                fullName = sharedPreferences.getString("user_name", getString(R.string.guest));
            }
            tvFullName.setText(fullName.trim().isEmpty() ? getString(R.string.guest) : fullName);

            etFirstName.setText(firstName);
            etLastName.setText(lastName);
            etGender.setText(sharedPreferences.getString("gender", ""));
            etMaritalStatus.setText(sharedPreferences.getString("marital_status", ""));
            etPhoneNumber.setText(sharedPreferences.getString("phone_number", ""));
            etBirthDate.setText(sharedPreferences.getString("birth_date", ""));
            etAddress.setText(sharedPreferences.getString("address", ""));

            tvEmail.setText(currentUser.getEmail());

            if (currentUser.isEmailVerified()) {
                tvEmailStatus.setText("✅ " + getString(R.string.email_verified));
                tvEmailStatus.setTextColor(getColor(R.color.income_green));
            } else {
                tvEmailStatus.setText("⚠️ " + getString(R.string.email_not_verified));
                tvEmailStatus.setTextColor(getColor(R.color.expense_red));
                tvEmailStatus.setClickable(true);
                tvEmailStatus.setOnClickListener(v -> sendVerificationEmail());
            }

            String userId = currentUser.getUid();
            tvUserId.setText(getString(R.string.user_id) + ": " + userId.substring(0, 8) + "...");

            String memberSince = sharedPreferences.getString("member_since", "2026");
            tvMemberSince.setText(getString(R.string.member_since) + ": " + memberSince);
        }
    }

    private void setupSpinners() {
        // Gender dropdown
        etGender.setOnClickListener(v -> showGenderDialog());
        etGender.setFocusable(false);

        // Marital Status dropdown
        etMaritalStatus.setOnClickListener(v -> showMaritalStatusDialog());
        etMaritalStatus.setFocusable(false);

        // Birth date picker
        etBirthDate.setOnClickListener(v -> showDatePickerDialog());
        etBirthDate.setFocusable(false);
    }

    private void showGenderDialog() {
        String[] genders = {
                getString(R.string.male),
                getString(R.string.female),
                getString(R.string.other)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.gender))
                .setItems(genders, (dialog, which) -> {
                    etGender.setText(genders[which]);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showMaritalStatusDialog() {
        String[] statuses = {
                getString(R.string.single),
                getString(R.string.married),
                getString(R.string.divorced),
                getString(R.string.widowed)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.marital_status))
                .setItems(statuses, (dialog, which) -> {
                    etMaritalStatus.setText(statuses[which]);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    etBirthDate.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void sendVerificationEmail() {
        if (currentUser != null && !currentUser.isEmailVerified()) {
            currentUser.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(UserProfile.this,
                                    getString(R.string.verification_email_sent),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(UserProfile.this,
                                    getString(R.string.error) + ": " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void setupClickListeners() {
        cardChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        cardChangeEmail.setOnClickListener(v -> showChangeEmailDialog());
        btnSaveProfile.setOnClickListener(v -> saveProfileData());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void saveProfileData() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String gender = etGender.getText().toString().trim();
        String maritalStatus = etMaritalStatus.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String fullName = firstName + " " + lastName;

        // 1. Telefon yaddaşına (SharedPreferences) yaz
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("first_name", firstName);
        editor.putString("last_name", lastName);
        editor.putString("user_name", fullName);
        editor.putString("gender", gender);
        editor.putString("marital_status", maritalStatus);
        editor.putString("phone_number", phoneNumber);
        editor.putString("birth_date", birthDate);
        editor.putString("address", address);
        editor.apply();

        // 2. Firebase Cloud-a yaz
        saveToFirebaseCloud(firstName, lastName, fullName, gender, maritalStatus, phoneNumber, birthDate, address);

        // Update display name
        if (!fullName.trim().isEmpty()) {
            tvFullName.setText(fullName);
        }
    }

    private void saveToFirebaseCloud(String firstName, String lastName, String fullName,
                                     String gender, String maritalStatus, String phoneNumber,
                                     String birthDate, String address) {

        if (currentUser == null) {
            Toast.makeText(this, "İstifadəçi tapılmadı", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // 1. Firestore-a yaz
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("name", fullName);
        userData.put("gender", gender);
        userData.put("maritalStatus", maritalStatus);
        userData.put("phoneNumber", phoneNumber);
        userData.put("birthDate", birthDate);
        userData.put("address", address);
        userData.put("email", currentUser.getEmail());
        userData.put("updatedAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d("FIREBASE", "✅ Firestore-a yazıldı");
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE", "❌ Firestore xətası: " + e.getMessage());
                });

        // 2. Authentication profilini yenilə (düzgün yol)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            com.google.firebase.auth.UserProfileChangeRequest profileUpdates =
                    new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build();

            user.updateProfile(profileUpdates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("FIREBASE", "✅ Authentication profili yeniləndi: " + fullName);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FIREBASE", "❌ Authentication xətası: " + e.getMessage());
                    });
        }
    }

    private void loadFromSharedPreferences() {
        String firstName = sharedPreferences.getString("first_name", "");
        String lastName = sharedPreferences.getString("last_name", "");
        String fullName = firstName + " " + lastName;

        if (fullName.trim().isEmpty()) {
            fullName = sharedPreferences.getString("user_name", getString(R.string.guest));
        }
        tvFullName.setText(fullName.trim().isEmpty() ? getString(R.string.guest) : fullName);

        etFirstName.setText(firstName);
        etLastName.setText(lastName);
        etGender.setText(sharedPreferences.getString("gender", ""));
        etMaritalStatus.setText(sharedPreferences.getString("marital_status", ""));
        etPhoneNumber.setText(sharedPreferences.getString("phone_number", ""));
        etBirthDate.setText(sharedPreferences.getString("birth_date", ""));
        etAddress.setText(sharedPreferences.getString("address", ""));

        tvEmail.setText(currentUser.getEmail());

        if (currentUser.isEmailVerified()) {
            tvEmailStatus.setText("✅ " + getString(R.string.email_verified));
            tvEmailStatus.setTextColor(getColor(R.color.income_green));
        } else {
            tvEmailStatus.setText("⚠️ " + getString(R.string.email_not_verified));
            tvEmailStatus.setTextColor(getColor(R.color.expense_red));
            tvEmailStatus.setClickable(true);
            tvEmailStatus.setOnClickListener(v -> sendVerificationEmail());
        }

        String userId = currentUser.getUid();
        tvUserId.setText(getString(R.string.user_id) + ": " + userId.substring(0, 8) + "...");

        String memberSince = sharedPreferences.getString("member_since", "2026");
        tvMemberSince.setText(getString(R.string.member_since) + ": " + memberSince);
    }

    private void loadFromFirebaseCloud() {
        String userId = currentUser.getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Cloud-dan gələn məlumatlarla UI yenilə
                        String cloudFirstName = documentSnapshot.getString("firstName");
                        String cloudLastName = documentSnapshot.getString("lastName");
                        String cloudName = documentSnapshot.getString("name");
                        String cloudGender = documentSnapshot.getString("gender");
                        String cloudMaritalStatus = documentSnapshot.getString("maritalStatus");
                        String cloudPhone = documentSnapshot.getString("phoneNumber");
                        String cloudBirthDate = documentSnapshot.getString("birthDate");
                        String cloudAddress = documentSnapshot.getString("address");

                        // Əgər Cloud-da məlumat varsa, onları göstər və SharedPreferences-ə yaz
                        if (cloudName != null && !cloudName.isEmpty()) {
                            tvFullName.setText(cloudName);

                            // SharedPreferences-i də yenilə
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            if (cloudFirstName != null) editor.putString("first_name", cloudFirstName);
                            if (cloudLastName != null) editor.putString("last_name", cloudLastName);
                            if (cloudName != null) editor.putString("user_name", cloudName);
                            if (cloudGender != null) editor.putString("gender", cloudGender);
                            if (cloudMaritalStatus != null) editor.putString("marital_status", cloudMaritalStatus);
                            if (cloudPhone != null) editor.putString("phone_number", cloudPhone);
                            if (cloudBirthDate != null) editor.putString("birth_date", cloudBirthDate);
                            if (cloudAddress != null) editor.putString("address", cloudAddress);
                            editor.apply();

                            // UI yenilə
                            if (cloudFirstName != null) etFirstName.setText(cloudFirstName);
                            if (cloudLastName != null) etLastName.setText(cloudLastName);
                            if (cloudGender != null) etGender.setText(cloudGender);
                            if (cloudMaritalStatus != null) etMaritalStatus.setText(cloudMaritalStatus);
                            if (cloudPhone != null) etPhoneNumber.setText(cloudPhone);
                            if (cloudBirthDate != null) etBirthDate.setText(cloudBirthDate);
                            if (cloudAddress != null) etAddress.setText(cloudAddress);

                            Log.d("FIREBASE", "✅ Cloud-dan məlumatlar yükləndi");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE", "❌ Cloud-dan oxuma xətası: " + e.getMessage());
                });
    }

    private void showChangePasswordDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_change_password);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setGravity(Gravity.CENTER);

        EditText etCurrentPassword = dialog.findViewById(R.id.etCurrentPassword);
        EditText etNewPassword = dialog.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialog.findViewById(R.id.etConfirmPassword);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        btnConfirm.setOnClickListener(v -> {
            String currentPwd = etCurrentPassword.getText().toString().trim();
            String newPwd = etNewPassword.getText().toString().trim();
            String confirmPwd = etConfirmPassword.getText().toString().trim();

            if (currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPwd.equals(confirmPwd)) {
                Toast.makeText(this, getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPwd.length() < 6) {
                Toast.makeText(this, getString(R.string.password_min_length), Toast.LENGTH_SHORT).show();
                return;
            }

            String email = currentUser.getEmail();
            currentUser.reauthenticate(EmailAuthProvider.getCredential(email, currentPwd))
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            currentUser.updatePassword(newPwd)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Toast.makeText(UserProfile.this,
                                                    getString(R.string.password_changed),
                                                    Toast.LENGTH_LONG).show();
                                            dialog.dismiss();
                                        } else {
                                            Toast.makeText(UserProfile.this,
                                                    getString(R.string.error) + ": " + task1.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(UserProfile.this,
                                    getString(R.string.wrong_current_password),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showChangeEmailDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_change_email);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setGravity(Gravity.CENTER);

        EditText etNewEmail = dialog.findViewById(R.id.etNewEmail);
        EditText etPassword = dialog.findViewById(R.id.etPassword);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        btnConfirm.setOnClickListener(v -> {
            String newEmail = etNewEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (newEmail.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            String email = currentUser.getEmail();
            currentUser.reauthenticate(EmailAuthProvider.getCredential(email, password))
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            currentUser.verifyBeforeUpdateEmail(newEmail)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Toast.makeText(UserProfile.this,
                                                    getString(R.string.verification_email_sent_new_email),
                                                    Toast.LENGTH_LONG).show();
                                            dialog.dismiss();
                                        } else {
                                            Toast.makeText(UserProfile.this,
                                                    getString(R.string.error) + ": " + task1.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(UserProfile.this,
                                    getString(R.string.wrong_password),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.delete_account))
                .setMessage(getString(R.string.delete_account_confirmation))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    currentUser.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(UserProfile.this,
                                            getString(R.string.account_deleted),
                                            Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                    Intent intent = new Intent(UserProfile.this,
                                            com.authentication.LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(UserProfile.this,
                                            getString(R.string.error) + ": " + task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }
}