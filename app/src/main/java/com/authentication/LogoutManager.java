package com.authentication;


import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.authentication.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LogoutManager {

    private static LogoutManager instance;
    private FirebaseAuth mAuth;

    private LogoutManager() {
        mAuth = FirebaseAuth.getInstance();
    }

    public static synchronized LogoutManager getInstance() {
        if (instance == null) {
            instance = new LogoutManager();
        }
        return instance;
    }

    /**
     * Sadə logout - yalnız Firebase-dən çıxış edir
     */
    public void logout() {
        mAuth.signOut();
    }

    /**
     * Təsdiq dialoqu olmadan birbaşa logout edir və LoginActivity-ə yönləndirir
     */
    public void logoutAndRedirect(Context context) {
        mAuth.signOut();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).finish();
        }
    }

    /**
     * Toast mesajı ilə logout
     */
    public void logoutWithMessage(Context context, String message) {
        mAuth.signOut();

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).finish();
        }
    }

    /**
     * Təsdiq dialoqu göstərir, təsdiq olunarsa logout edir
     */
    public void showLogoutConfirmationDialog(Context context) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("🚪 Çıxış")
                .setMessage("Hesabdan çıxmaq istədiyinizə əminsiniz?")
                .setPositiveButton("Bəli", (dialog, which) -> {
                    logoutAndRedirect(context);
                    Toast.makeText(context, "Çıxış edildi", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Xeyr", null)
                .show();
    }

    /**
     * Cari istifadəçinin login olub-olmadığını yoxlayır
     */
    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    /**
     * Cari istifadəçinin email-ni qaytarır
     */
    public String getUserEmail() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getEmail();
        }
        return null;
    }

    /**
     * Cari istifadəçinin UID-sini qaytarır
     */
    public String getUserId() {
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        }
        return null;
    }
}
