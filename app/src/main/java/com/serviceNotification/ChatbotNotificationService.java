package com.serviceNotification;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.main.MainActivity;
import com.model.MobileBERTQA;
import com.smart_ai_sales.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatbotNotificationService extends Service {

    private static final String TAG = "ChatbotNotifyService";
    private static final String CHANNEL_ID = "chatbot_notification_channel";
    private static final int NOTIFICATION_ID = 2001;

    private Handler handler;
    private ExecutorService executorService;
    private Runnable notificationRunnable;
    private MobileBERTQA mobileBERT;
    private Random random = new Random();

    // Müxtəlif suallar (random seçiləcək)
    private final String[] questions = {
            "Nə qədər xərclədim?",
            "Qənaətim nə qədərdir?",
            "Mənə məsləhət ver",
            "Balansım nə qədərdir?",
            "Risk analizi et",
            "Maliyyəmi analiz et",
            "Gəlirim nə qədərdir?",
            "Xərclərim çoxdur?",
            "Necə qənaət edim?",
            "Büdcəmi necə idarə edim?"
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("🤖 Chatbot Aktivdir", "Hər saat yeni məsləhət alacaqsınız"));

        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();
        mobileBERT = new MobileBERTQA(this);

        loadFinancialData();
        startNotificationScheduler();
    }

    private void loadFinancialData() {
        SharedPreferences prefs = getSharedPreferences("finance_data", MODE_PRIVATE);
        double totalIncome = prefs.getFloat("total_income", 0);
        double totalExpense = prefs.getFloat("total_expense", 0);
        double monthlySalary = prefs.getFloat("monthly_salary", 0);

        if (mobileBERT != null) {
            mobileBERT.updateFinancialData(totalIncome, totalExpense, monthlySalary);
        }

        Log.d(TAG, "📊 Maliyyə məlumatları yükləndi: Gəlir=" + totalIncome + ", Xərc=" + totalExpense);
    }

    private void startNotificationScheduler() {
        notificationRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "🔄 Hər saatlıq chatbot bildirişi başlayır...");

                // Maliyyə məlumatlarını yenilə
                loadFinancialData();

                // Random sual seç
                String randomQuestion = questions[random.nextInt(questions.length)];

                // Cavab al
                String answer = getChatbotAnswer(randomQuestion);

                // Bildiriş göndər
                sendChatbotNotification(randomQuestion, answer);

                // 1 saat sonra təkrar et
                handler.postDelayed(this, 60 * 60 * 1000); // 1 saat
            }
        };
        handler.post(notificationRunnable);
    }

    private String getChatbotAnswer(String question) {
        try {
            // Maliyyə kontekstini yarat
            SharedPreferences prefs = getSharedPreferences("finance_data", MODE_PRIVATE);
            double totalIncome = prefs.getFloat("total_income", 0);
            double totalExpense = prefs.getFloat("total_expense", 0);
            double monthlySalary = prefs.getFloat("monthly_salary", 0);
            double savings = totalIncome - totalExpense;

            String context = mobileBERT.createFinancialContext(totalIncome, totalExpense, monthlySalary, savings);
            String answer = mobileBERT.answerQuestion(question, context);

            return answer;
        } catch (Exception e) {
            Log.e(TAG, "Cavab alınarkən xəta", e);
            return getFallbackAnswer(question);
        }
    }

    private String getFallbackAnswer(String question) {
        String q = question.toLowerCase();

        if (q.contains("xərc") || q.contains("spend")) {
            SharedPreferences prefs = getSharedPreferences("finance_data", MODE_PRIVATE);
            double expense = prefs.getFloat("total_expense", 0);
            return String.format("💰 Ümumi xərcləriniz: %.2f AZN. Daha ətraflı üçün tətbiqi açın.", expense);
        }
        if (q.contains("qənaət") || q.contains("save")) {
            SharedPreferences prefs = getSharedPreferences("finance_data", MODE_PRIVATE);
            double income = prefs.getFloat("total_income", 0);
            double expense = prefs.getFloat("total_expense", 0);
            double savings = income - expense;
            return String.format("💚 Qənaətiniz: %.2f AZN. Qənaət etməyə davam edin!", savings);
        }
        if (q.contains("məsləhət") || q.contains("advice")) {
            return "💡 Məsləhət: Xərclərinizi gündəlik qeyd edin və 50/30/20 qaydasını tətbiq edin!";
        }
        if (q.contains("balans") || q.contains("balance")) {
            SharedPreferences prefs = getSharedPreferences("finance_data", MODE_PRIVATE);
            double income = prefs.getFloat("total_income", 0);
            double expense = prefs.getFloat("total_expense", 0);
            double balance = income - expense;
            return String.format("💳 Cari balansınız: %.2f AZN", balance);
        }
        if (q.contains("risk") || q.contains("analiz")) {
            SharedPreferences prefs = getSharedPreferences("finance_data", MODE_PRIVATE);
            double income = prefs.getFloat("total_income", 0);
            double expense = prefs.getFloat("total_expense", 0);
            double savingsRate = income > 0 ? ((income - expense) / income) * 100 : 0;

            if (savingsRate < 10) {
                return "⚠️ RİSK ANALİZİ: Qənaət dərəcəniz aşağıdır! Xərclərinizi azaldın.";
            } else if (savingsRate < 20) {
                return "🟡 RİSK ANALİZİ: Orta səviyyədə. Daha çox qənaət etməyə çalışın.";
            } else {
                return "🟢 RİSK ANALİZİ: Aşağı risk. Maliyyə vəziyyətiniz yaxşıdır!";
            }
        }

        return "🤖 Mən sizin AI maliyyə köməkçinizəm. Tətbiqi açaraq daha çox məlumat ala bilərsiniz!";
    }

    private void sendChatbotNotification(String question, String answer) {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        // Cavabı qısalt (bildiriş üçün)
        String shortAnswer = answer.length() > 80 ? answer.substring(0, 77) + "..." : answer;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat_bot)
                .setContentTitle("🤖 Chatbot Məsləhəti | " + time)
                .setContentText("❓ " + question)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("❓ Sual: " + question + "\n\n💬 Cavab: " + shortAnswer))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setColor(getColor(R.color.purple_500));

        // Klikləndikdə tətbiqi aç
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID + random.nextInt(1000), builder.build());
            Log.d(TAG, "✅ Chatbot bildirişi göndərildi: " + question);
        }
    }

    private android.app.Notification createNotification(String title, String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat_bot)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chatbot Bildirişləri",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("🤖 Hər saat Chatbot-dan məsləhət və maliyyə analizi");
            channel.enableVibration(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && notificationRunnable != null) {
            handler.removeCallbacks(notificationRunnable);
        }
        if (executorService != null) {
            executorService.shutdown();
        }
        if (mobileBERT != null) {
            mobileBERT.close();
        }
        Log.d(TAG, "Service destroyed");
    }
}
