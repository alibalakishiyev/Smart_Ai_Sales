package com.serviceNotification;



import android.content.Context;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class WorkManagerScheduler {

    private static final String TAG      = "WorkManagerScheduler";
    private static final String WORK_TAG = "finance_periodic_worker";

    // NOT: WorkManager minimum 15 dəqiqə dəstəkləyir.
    // Əgər daha qısa interval istəyirsinizsə FinanceMonitoringService-i
    // saxlayıb orada interval dəyişin.
    private static final int REPEAT_INTERVAL_MINUTES = 15;

    private WorkManagerScheduler() {}

    // -------------------------------------------------------
    // İşi planla — Application.onCreate() və ya
    // MainActivity.onCreate()-də BİR DƏFƏ çağır
    // -------------------------------------------------------
    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(
                        FinanceWorker.class,
                        REPEAT_INTERVAL_MINUTES,
                        TimeUnit.MINUTES
                )
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                30, TimeUnit.SECONDS    // xəta halında 30san sonra yenidən cəhd
                        )
                        .addTag(WORK_TAG)
                        .build();

        // KEEP: artıq planlanıbsa dəyişmə
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        WORK_TAG,
                        ExistingPeriodicWorkPolicy.KEEP,
                        request
                );

        Log.d(TAG, "Finance worker planlandı — hər "
                + REPEAT_INTERVAL_MINUTES + " dəqiqədə bir.");
    }

    // -------------------------------------------------------
    // İşi dayandır (Settings-dən söndürmək üçün)
    // -------------------------------------------------------
    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_TAG);
        Log.d(TAG, "Finance worker dayandırıldı.");
    }

    // -------------------------------------------------------
    // Dərhal bir dəfə işlət (test / əl ilə refresh üçün)
    // -------------------------------------------------------
    public static void runNow(Context context) {
        androidx.work.OneTimeWorkRequest oneTime =
                new androidx.work.OneTimeWorkRequest.Builder(FinanceWorker.class)
                        .addTag(WORK_TAG + "_manual")
                        .build();
        WorkManager.getInstance(context).enqueue(oneTime);
        Log.d(TAG, "Finance worker dərhal işlədildi.");
    }

    // -------------------------------------------------------
    // İşin statusunu yoxla (debug üçün)
    // -------------------------------------------------------
    public static void checkStatus(Context context) {
        try {
            List<WorkInfo> infos = WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWork(WORK_TAG).get();
            for (WorkInfo info : infos) {
                Log.d(TAG, "Work status: " + info.getState().name());
            }
        } catch (Exception e) {
            Log.e(TAG, "Status yoxlama xətası", e);
        }
    }
}
