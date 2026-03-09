package com.model;


import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Calendar;

public class FinanceMLModel {
    private static final String TAG = "FinanceMLModel";
    private static final String MODEL_FILE = "MLModel/finance_model.tflite";

    private Interpreter tflite;
    private boolean isInitialized = false;

    public FinanceMLModel(Context context) {
        try {
            tflite = new Interpreter(loadModelFile(context));
            isInitialized = true;
            Log.d(TAG, "Model initialized successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error loading model", e);
            isInitialized = false;
        }
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // BU METODU ƏLAVƏ EDİN
    public boolean isInitialized() {
        return isInitialized;
    }

    public float[] predictNextDay(float[][] historicalData) {
        // Fallback: Son 7 günün ortalaması
        float avgIncome = 0;
        float avgExpense = 0;
        int count = 0;

        int start = Math.max(0, historicalData.length - 7);
        for (int i = start; i < historicalData.length; i++) {
            avgIncome += historicalData[i][0];
            avgExpense += historicalData[i][1];
            count++;
        }

        if (count > 0) {
            avgIncome /= count;
            avgExpense /= count;
        }

        // Həftəsonu effekti
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        boolean isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);

        if (isWeekend) {
            avgIncome *= 0.7;
            avgExpense *= 1.3;
        }

        return new float[]{avgIncome, avgExpense, avgIncome - avgExpense};
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}