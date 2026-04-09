package com.serviceNotification;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.main.MainActivity;
import com.smart_ai_sales.R;

import java.util.List;
import java.util.Locale;
import java.util.Random;

public class FinanceNotificationHelper {

    private static final String CHANNEL_ID   = "finance_v2_channel";
    private static final String CHANNEL_NAME = "Maliyyə & Endirim Bildirişləri";

    private final Context context;
    private final NotificationManager manager;
    private final Random random = new Random();

    // Bildiriş ID-ləri — çakışmasın deyə ayrı aralıqlar
    private static final int ID_PRODUCT  = 2001;
    private static final int ID_RISK     = 2002;
    private static final int ID_SAVINGS  = 2003;
    private static final int ID_GENERAL  = 2004;

    public FinanceNotificationHelper(Context context) {
        this.context = context;
        this.manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
    }

    // -------------------------------------------------------
    // Kanal yarat (Android 8+)
    // -------------------------------------------------------
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(
                    "Hər 15 dəqiqədən bir məhsul analizi, risk xəbərdarlıqları");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 150, 300});
            channel.setLockscreenVisibility(
                    android.app.Notification.VISIBILITY_PUBLIC);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    // -------------------------------------------------------
    // 1. Məhsul analizi bildirişi — hər dövrdə göndərilir
    // -------------------------------------------------------
    public void sendProductAnalysisNotification(
            List<String> items,
            FinanceWorker.FinanceAnalysisResult analysis) {

        if (items == null || items.isEmpty()) return;

        // Məhsul adlarını birləşdir (maks 3)
        StringBuilder sb = new StringBuilder();
        int show = Math.min(3, items.size());
        for (int i = 0; i < show; i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i));
        }
        if (items.size() > 3) sb.append("...");

        String title = String.format(Locale.getDefault(),
                "🛒 %d məhsul analiz edildi", items.size());

        String body = String.format(Locale.getDefault(),
                "📦 %s\n📊 Trend: %s\n💸 Qənaət potensialı: ₼%.2f",
                sb, analysis.trend, analysis.savingsPotential);

        // Kateqoriya varsa əlavə et
        if (!analysis.topCategory.isEmpty()) {
            body += String.format(Locale.getDefault(),
                    "\n🏷️ Ən çox xərc: %s (₼%.2f)",
                    analysis.topCategory, analysis.topCategoryAmount);
        }

        send(title, body, ID_PRODUCT + random.nextInt(50));
    }

    // -------------------------------------------------------
    // 2. Risk xəbərdarlığı — riskLevel > 0.65 olduqda
    // -------------------------------------------------------
    public void sendRiskWarningNotification(
            FinanceWorker.FinanceAnalysisResult analysis) {

        String riskEmoji = analysis.riskLevel > 0.80f ? "🔴" : "🟡";
        String riskText  = analysis.riskLevel > 0.80f ? "YÜKSƏK" : "ORTA";

        String title = riskEmoji + " Maliyyə riski: " + riskText;

        String body = String.format(Locale.getDefault(),
                "Bu həftə xərc: ₼%.2f\nÖncəki həftə: ₼%.2f\n" +
                        "3 günlük proqnoz: ₼%.2f\n\n" +
                        "💡 Xərclərinizi nəzərdən keçirin!",
                analysis.lastWeekExpense,
                analysis.prevWeekExpense,
                analysis.forecast3d);

        send(title, body, ID_RISK);
    }

    // -------------------------------------------------------
    // 3. Qənaət motivasiya bildirişi — savingsRate > 20% olduqda
    // -------------------------------------------------------
    public void sendSavingsMotivationNotification(
            FinanceWorker.FinanceAnalysisResult analysis) {

        String title = "🌟 Əla! Maliyyə vəziyyətiniz yaxşıdır";

        String body = String.format(Locale.getDefault(),
                "Qənaət dərəcəniz: %.1f%%\n" +
                        "Gəlir: ₼%.2f | Xərc: ₼%.2f\n\n" +
                        "💡 İnvestisiya etməyi düşünün!",
                analysis.savingsRate,
                analysis.totalIncome,
                analysis.totalExpense);

        send(title, body, ID_SAVINGS);
    }

    // -------------------------------------------------------
    // 4. Ümumi xülasə bildirişi (əl ilə istifadə üçün)
    // -------------------------------------------------------
    public void sendGeneralSummaryNotification(
            FinanceWorker.FinanceAnalysisResult analysis) {

        String title = "📊 Maliyyə Xülasəsi";

        String healthEmoji;
        if      (analysis.savingsRate >= 20) healthEmoji = "🌟 Əla";
        else if (analysis.savingsRate >= 10) healthEmoji = "👍 Yaxşı";
        else if (analysis.savingsRate >= 0)  healthEmoji = "⚠️ Orta";
        else                                 healthEmoji = "🔴 Zəif";

        String body = String.format(Locale.getDefault(),
                "💵 Gəlir: ₼%.2f\n💸 Xərc: ₼%.2f\n" +
                        "💰 Qənaət: ₼%.2f (%.1f%%)\n" +
                        "📊 Maliyyə sağlamlığı: %s",
                analysis.totalIncome,
                analysis.totalExpense,
                analysis.totalIncome - analysis.totalExpense,
                Math.max(0, analysis.savingsRate),
                healthEmoji);

        send(title, body, ID_GENERAL);
    }

    // -------------------------------------------------------
    // Daxili: bildiriş göndər
    // -------------------------------------------------------
    private void send(String title, String body, int notifId) {
        if (manager == null) return;

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pi = PendingIntent.getActivity(
                context, notifId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setVibrate(new long[]{0, 300, 150, 300})
                        .setContentIntent(pi);

        manager.notify(notifId, builder.build());
    }
}
