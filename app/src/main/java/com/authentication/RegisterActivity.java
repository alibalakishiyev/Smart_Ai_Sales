package com.authentication;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.dashboard.DashboardActivity;
import com.smart_ai_sales.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private TextInputLayout layoutName, layoutEmail, layoutPassword, layoutConfirmPassword;
    private Button buttonRegister;
    private TextView textViewLogin, textViewTerms;
    private ProgressBar progressBar;
    private CardView cardView;
    private ScrollView scrollView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Animation fadeIn, slideUp;

    // Şifrə gücü üçün regex nümunəsi
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[0-9])" +         // ən azı 1 rəqəm
                    "(?=.*[a-zA-Z])" +      // ən azı 1 hərf
                    "(?=\\S+$)" +           // boşluq olmamalıdır
                    ".{6,}" +                // ən azı 6 simvol
                    "$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Animasiyaları yüklə
        loadAnimations();

        initViews();
        setupClickListeners();
        startEntryAnimation();
    }

    private void loadAnimations() {
        fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
    }

    private void initViews() {
        // TextInputLayout-lar
        layoutName = findViewById(R.id.layoutName);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword);

        // EditText-lər
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);

        // Button və TextView
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
        textViewTerms = findViewById(R.id.textViewTerms);

        // ProgressBar, CardView, ScrollView
        progressBar = findViewById(R.id.progressBar);
        cardView = findViewById(R.id.cardView);
        scrollView = findViewById(R.id.scrollView);

        // Xəta mesajlarını təmizləmək üçün listener-lər
        setupErrorClearListeners();
    }

    private void setupErrorClearListeners() {
        View.OnFocusChangeListener clearErrorListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (v == editTextName) {
                        layoutName.setError(null);
                    } else if (v == editTextEmail) {
                        layoutEmail.setError(null);
                    } else if (v == editTextPassword) {
                        layoutPassword.setError(null);
                    } else if (v == editTextConfirmPassword) {
                        layoutConfirmPassword.setError(null);
                    }
                }
            }
        };

        editTextName.setOnFocusChangeListener(clearErrorListener);
        editTextEmail.setOnFocusChangeListener(clearErrorListener);
        editTextPassword.setOnFocusChangeListener(clearErrorListener);
        editTextConfirmPassword.setOnFocusChangeListener(clearErrorListener);
    }

    private void setupClickListeners() {
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(RegisterActivity.this, R.anim.button_click));
                registerUser();
            }
        });

        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(AnimationUtils.loadAnimation(RegisterActivity.this, R.anim.button_click));
                navigateToLogin();
            }
        });

        textViewTerms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTermsAndConditions();
            }
        });
    }

    private void startEntryAnimation() {
        cardView.startAnimation(slideUp);
        scrollView.startAnimation(fadeIn);
    }

    private void registerUser() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim().toLowerCase();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Validasiya
        if (!validateInputs(name, email, password, confirmPassword)) {
            return;
        }

        // UI yenilə
        setLoadingState(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Email təsdiqləmə göndər
                                sendEmailVerification(user, name, email);
                            }
                        } else {
                            setLoadingState(false);
                            handleRegistrationError(task.getException());
                        }
                    }
                });
    }

    private boolean validateInputs(String name, String email, String password, String confirmPassword) {
        boolean isValid = true;

        // Ad validasiyası
        if (TextUtils.isEmpty(name)) {
            layoutName.setError("Ad Soyad daxil edin");
            isValid = false;
        } else if (name.length() < 3) {
            layoutName.setError("Ad Soyad minimum 3 simvol olmalıdır");
            isValid = false;
        } else {
            layoutName.setError(null);
        }

        // Email validasiyası
        if (TextUtils.isEmpty(email)) {
            layoutEmail.setError("Email daxil edin");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Düzgün email formatı daxil edin");
            isValid = false;
        } else {
            layoutEmail.setError(null);
        }

        // Şifrə validasiyası
        if (TextUtils.isEmpty(password)) {
            layoutPassword.setError("Şifrə daxil edin");
            isValid = false;
        } else if (password.length() < 6) {
            layoutPassword.setError("Şifrə minimum 6 simvol olmalıdır");
            isValid = false;
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            layoutPassword.setError("Şifrə ən azı 1 rəqəm və 1 hərf olmalıdır");
            isValid = false;
        } else {
            layoutPassword.setError(null);
        }

        // Şifrə təkrar validasiyası
        if (TextUtils.isEmpty(confirmPassword)) {
            layoutConfirmPassword.setError("Şifrəni təkrar daxil edin");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            layoutConfirmPassword.setError("Şifrələr uyğun gəlmir");
            isValid = false;
        } else {
            layoutConfirmPassword.setError(null);
        }

        return isValid;
    }

    private void sendEmailVerification(FirebaseUser user, String name, String email) {
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            saveUserToFirestore(user.getUid(), name, email);
                            Toast.makeText(RegisterActivity.this,
                                    "Təsdiqləmə emaili göndərildi. Emailinizi yoxlayın.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            setLoadingState(false);
                            Toast.makeText(RegisterActivity.this,
                                    "Email təsdiqləmə göndərilmədi: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());
        user.put("totalProfit", 0.0);
        user.put("totalSales", 0.0);
        user.put("totalExpenses", 0.0);
        user.put("isEmailVerified", false);
        user.put("accountStatus", "active");
        user.put("lastLogin", System.currentTimeMillis());
        user.put("userType", "standard");

        db.collection("users").document(userId)
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        setLoadingState(false);

                        if (task.isSuccessful()) {
                            // Məlumatlar uğurla saxlanıldı
                            showSuccessAndNavigate();
                        } else {
                            // Firestore xətası - user Firebase-də yaradılıb, amma məlumatlar saxlanılmadı
                            Toast.makeText(RegisterActivity.this,
                                    "Hesab yaradıldı, lakin məlumatlar saxlanılmadı. Dəstək ilə əlaqə saxlayın.",
                                    Toast.LENGTH_LONG).show();

                            // Yenə də Dashboard-a keç
                            navigateToDashboard();
                        }
                    }
                });
    }

    private void handleRegistrationError(Exception exception) {
        String errorMessage;

        if (exception instanceof FirebaseAuthWeakPasswordException) {
            errorMessage = "Şifrə çox zəifdir. Daha güclü şifrə seçin.";
            layoutPassword.setError(errorMessage);
        } else if (exception instanceof FirebaseAuthUserCollisionException) {
            errorMessage = "Bu email artıq qeydiyyatdan keçib.";
            layoutEmail.setError(errorMessage);
        } else {
            errorMessage = "Qeydiyyat xətası: " + exception.getMessage();
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            buttonRegister.setEnabled(false);
            buttonRegister.setAlpha(0.5f);
            textViewLogin.setEnabled(false);
            editTextName.setEnabled(false);
            editTextEmail.setEnabled(false);
            editTextPassword.setEnabled(false);
            editTextConfirmPassword.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            buttonRegister.setEnabled(true);
            buttonRegister.setAlpha(1.0f);
            textViewLogin.setEnabled(true);
            editTextName.setEnabled(true);
            editTextEmail.setEnabled(true);
            editTextPassword.setEnabled(true);
            editTextConfirmPassword.setEnabled(true);
        }
    }

    private void showSuccessAndNavigate() {
        // Uğurlu qeydiyyat animasiyası
        progressBar.setVisibility(View.VISIBLE);
        progressBar.animate()
                .alpha(0.0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this,
                                "Qeydiyyat uğurla tamamlandı!",
                                Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    }
                });
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(RegisterActivity.this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("show_welcome", true);
        intent.putExtra("user_name", editTextName.getText().toString().trim());
        startActivity(intent);
        finish();

        // Keçid animasiyası
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void showTermsAndConditions() {
        // İstifadə şərtləri dialogu və ya yeni aktiviti
        Toast.makeText(this, "İstifadə şərtləri", Toast.LENGTH_SHORT).show();
        // Burada TermsActivity açıla bilər
    }
}