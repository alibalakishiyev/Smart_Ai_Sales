package com.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.smart_ai_sales.R;

import java.util.Calendar;

public class SalaryReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "salary_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String PREF_NAME = "salary_reminder_prefs";
    private static final String KEY_LAST_REMINDER_MONTH = "last_reminder_month";
    private static final String KEY_LAST_REMINDER_YEAR = "last_reminder_year";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Bu ay üçün artıq xatırlatma edilibsə, təkrar göstərmə
        if (hasAlreadyRemindedThisMonth(context)) {
            return;
        }

        // Maaşın təyin edilməli olduğu günləri yoxla
        if (shouldShowReminder()) {
            showNotification(context);
            markRemindedThisMonth(context);
        }
    }

    /**
     * Maaşın təyin edilməli olduğu günlər:
     * - Hər ayın 1-5 i arasında
     * - Əgər 5-dən sonradırsa, xatırlatma göstərmə
     */
    private boolean shouldShowReminder() {
        Calendar calendar = Calendar.getInstance();
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        // Ayın 1-dən 5-dək xatırlatma göstər
        return dayOfMonth >= 1 && dayOfMonth <= 5;
    }

    /**
     * Bu ay artıq xatırlatma edilibsə true qaytar
     */
    private boolean hasAlreadyRemindedThisMonth(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);

        int lastMonth = prefs.getInt(KEY_LAST_REMINDER_MONTH, -1);
        int lastYear = prefs.getInt(KEY_LAST_REMINDER_YEAR, -1);

        return (lastMonth == currentMonth && lastYear == currentYear);
    }

    /**
     * Bu ay üçün xatırlatma edildiyini qeyd et
     */
    private void markRemindedThisMonth(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Calendar now = Calendar.getInstance();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_LAST_REMINDER_MONTH, now.get(Calendar.MONTH));
        editor.putInt(KEY_LAST_REMINDER_YEAR, now.get(Calendar.YEAR));
        editor.apply();
    }

    /**
     * Maaş əlavə edildikdən sonra çağırılacaq metod
     * Bu ay üçün xatırlatmanı sıfırlayır (növbəti ayadək)
     */
    public static void onSalaryAdded(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        // Xatırlatmanı sıfırla ki, eyni ayda təkrar göstərməsin
        editor.putInt(KEY_LAST_REMINDER_MONTH, Calendar.getInstance().get(Calendar.MONTH));
        editor.putInt(KEY_LAST_REMINDER_YEAR, Calendar.getInstance().get(Calendar.YEAR));
        editor.apply();
    }

    private void showNotification(Context context) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, AddSalaryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_salary)
                .setContentTitle("⏰ Maaş Günü!")
                .setContentText("Bu ayki maaşınızı əlavə etməyi unutmayın (Ayın 1-5 arası)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(context.getColor(R.color.income_green));

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Maaş Xatırlatma";
            String description = "Hər ayın 1-5 arasında maaş əlavə etmək üçün xatırlatma";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}