package com.utils;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.smart_ai_sales.R;

public class SalaryReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "salary_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Alarm işə düşəndə bildiriş göstər
        showNotification(context);

        // Əgər istəsəniz, avtomatik olaraq AddSalaryActivity-ni aça bilərsiniz
        // autoOpenAddSalary(context);
    }

    private void showNotification(Context context) {
        createNotificationChannel(context);

        // Maaş əlavə etmə aktivitisinə keçid üçün intent
        Intent intent = new Intent(context, AddSalaryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Bildiriş yarat
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_salary)
                .setContentTitle("Maaş Günü!")
                .setContentText("Bu ayki maaşınızı əlavə etməyi unutmayın")
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
            String description = "Aylıq maaş xatırlatmaları üçün kanal";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void autoOpenAddSalary(Context context) {
        // Avtomatik olaraq maaş əlavə etmə səhifəsini aç
        Intent intent = new Intent(context, AddSalaryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}