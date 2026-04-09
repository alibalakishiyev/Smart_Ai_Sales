package com.model;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.serviceNotification.TranslationHelper;
import com.smart_ai_sales.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MultilingualChatBotDialog extends AppCompatDialogFragment {

    private static final String TAG = "ChatBot";

    private MobileBERTQA    mobileBERT;
    private TranslationHelper translator;

    private LinearLayout chatContainer;
    private EditText     etUserInput;
    private Button       btnSend;
    private TextView     tvEmptyState;
    private TextView     tvLanguageName;

    // ✅ REAL məlumatlar - SharedPreferences-dən yüklənəcək
    private double totalIncome = 0;
    private double totalExpense = 0;
    private double monthlySalary = 0;
    private String lastAIAnalysis = "";

    private final List<ChatMessage> messages = new ArrayList<>();

    private final String[] languages     = {"az", "en", "ru"};
    private final String[] languageNames = {
            "🇦🇿 Azərbaycan", "🇬🇧 English", "🇷🇺 Русский"};
    private int currentLangIndex = 0;

    // ✅ Constructor - artıq parametr tələb etmir (SharedPreferences-dən oxuyacaq)
    public MultilingualChatBotDialog() {
        // Boş constructor - məlumatlar SharedPreferences-dən gələcək
    }

    // ✅ Köhnə constructor ilə uyğunluq üçün (parametrli çağırılıbsa)
    public MultilingualChatBotDialog(double totalIncome, double totalExpense,
                                     double monthlySalary, String aiAnalysis) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.monthlySalary = monthlySalary;
        this.lastAIAnalysis = aiAnalysis;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.dialog_multilingual_chatbot, container, false);

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow()
                    .setBackgroundDrawableResource(android.R.color.transparent);
        }

        loadFinancialDataFromPreferences();

        Log.d(TAG, "=========================================");
        Log.d(TAG, "CHATBOT BAŞLADI - MALİYYƏ MƏLUMATLARI:");
        Log.d(TAG, "💰 Gəlir: " + totalIncome + " AZN");
        Log.d(TAG, "💸 Xərc: " + totalExpense + " AZN");
        Log.d(TAG, "🏦 Maaş: " + monthlySalary + " AZN");
        Log.d(TAG, "💚 Qənaət: " + (totalIncome - totalExpense) + " AZN");
        Log.d(TAG, "=========================================");

        loadSavedLanguage();

        mobileBERT = new MobileBERTQA(getContext());
        translator = TranslationHelper.getInstance(getContext());
        translator.setLanguage(languages[currentLangIndex]);

        initViews(view);
        setupListeners();
        updateLangButton();

        // ✅ BURADA ÇAĞIRIN - Xoş gəlmisiniz mesajını əlavə et
        addWelcomeMessage();

        // AI analizi varsa onu da göstər
        if (lastAIAnalysis != null && !lastAIAnalysis.isEmpty()) {
            addAIAnalysisMessage(lastAIAnalysis);
        }

        return view;
    }

    // ✅ YENİ METOD: SharedPreferences-dən məlumatları yüklə
    private void loadFinancialDataFromPreferences() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("finance_data", Context.MODE_PRIVATE);

        // Əvvəlcə parametrlə gələn dəyərləri yoxla (əgər constructor ilə gəlibsə)
        if (totalIncome == 0 && totalExpense == 0) {
            // SharedPreferences-dən oxu
            totalIncome = prefs.getFloat("total_income", 0);
            totalExpense = prefs.getFloat("total_expense", 0);
            monthlySalary = prefs.getFloat("monthly_salary", 0);
            lastAIAnalysis = prefs.getString("last_ai_analysis", "");
        }

        // Hələ də 0-dırsa, DashboardActivity-dən gələn intent-i yoxla
        if (totalIncome == 0 && totalExpense == 0 && getActivity() != null) {
            if (getActivity().getIntent() != null) {
                totalIncome = getActivity().getIntent().getDoubleExtra("total_income", 0);
                totalExpense = getActivity().getIntent().getDoubleExtra("total_expense", 0);
                monthlySalary = getActivity().getIntent().getDoubleExtra("monthly_salary", 0);
            }
        }


        // Məlumatları mobileBERT-ə də ötür
        if (mobileBERT != null) {
            mobileBERT.updateFinancialData(totalIncome, totalExpense, monthlySalary);
        }
    }

    private void initViews(View v) {
        chatContainer  = v.findViewById(R.id.chatContainer);
        etUserInput    = v.findViewById(R.id.etUserInput);
        btnSend        = v.findViewById(R.id.btnSend);
        tvEmptyState   = v.findViewById(R.id.tvEmptyState);
        tvLanguageName = v.findViewById(R.id.tvLanguageName);

        v.findViewById(R.id.btnClose)
                .setOnClickListener(x -> dismiss());
        v.findViewById(R.id.btnLanguage)
                .setOnClickListener(x -> showLangSelector());

        if (lastAIAnalysis != null && !lastAIAnalysis.isEmpty()) {
            addAIAnalysisMessage(lastAIAnalysis);
        }
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> {
            String text = etUserInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendUserMessage(text);
                etUserInput.setText("");
            }
        });
    }

    private void loadSavedLanguage() {
        if (getContext() == null) return;
        SharedPreferences prefs =
                getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        String saved = prefs.getString("chat_language", "az");
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(saved)) { currentLangIndex = i; break; }
        }
    }

    private void showLangSelector() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(t("Dil seçin", "Select Language", "Выберите язык"))
                .setItems(languageNames, (dialog, which) -> {
                    currentLangIndex = which;
                    translator.setLanguage(languages[currentLangIndex]);
                    if (getContext() != null) {
                        getContext()
                                .getSharedPreferences("settings", Context.MODE_PRIVATE)
                                .edit()
                                .putString("chat_language", languages[currentLangIndex])
                                .apply();
                    }
                    updateLangButton();
                    addBotMessage(t(
                            "Dil " + languageNames[which] + " olaraq dəyişdirildi ✅",
                            "Language changed to " + languageNames[which] + " ✅",
                            "Язык изменён на " + languageNames[which] + " ✅"));
                })
                .show();
    }

    private void updateLangButton() {
        if (tvLanguageName != null)
            tvLanguageName.setText(languageNames[currentLangIndex]);
        if (etUserInput != null)
            etUserInput.setHint(t(
                    "Sualınızı yazın...",
                    "Type your question...",
                    "Напишите вопрос..."));
    }

    private void addWelcomeMessage() {
        if (chatContainer == null) {
            Log.e(TAG, "chatContainer null-dur, mesaj əlavə edilə bilmədi");
            return;
        }

        double savings = totalIncome - totalExpense;
        double savingsRate = getSavingsRate();

        // Sadə və təhlükəsiz mesaj
        String welcomeMessage = "👋 Salam! Mən sizin AI maliyyə köməkçinizəm.\n\n" +
                "📊 SİZİN MALİYYƏ VƏZİYYƏTİNİZ:\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "💰 Gəlir:     ₼" + String.format("%.2f", totalIncome) + "\n" +
                "💸 Xərc:      ₼" + String.format("%.2f", totalExpense) + "\n" +
                "💚 Qənaət:    ₼" + String.format("%.2f", savings) + "\n" +
                "📈 Qənaət dərəcəsi: " + String.format("%.1f", savingsRate) + "%\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                "💬 Mənə suallar verə bilərsiniz:\n" +
                "  • Nə qədər xərclədim?\n" +
                "  • Proqnoz ver\n" +
                "  • Məsləhət ver\n" +
                "  • Risk analizi et\n\n" +
                "🌐 Dil dəyişmək üçün yuxarıdakı düyməni basın.";

        // Mesajı əlavə et
        addBotMessage(welcomeMessage);
    }

    // ✅ YENİ: Qənaət dərəcəsini hesabla
    private double getSavingsRate() {
        if (totalIncome <= 0) return 0;
        return ((totalIncome - totalExpense) / totalIncome) * 100;
    }

    private void sendUserMessage(String message) {
        addUserMessage(message);

        Log.d(TAG, "📨 İstifadəçi: " + message);
        Log.d(TAG, "💰 Gəlir=" + totalIncome + ", 💸 Xərc=" + totalExpense);

        // 1. Sürətli cavabları yoxla
        String smartResp = getSmartResponse(message);
        if (smartResp != null) {
            addBotMessage(smartResp);
            scrollToBottom();
            return;
        }

        // 2. MobileBERT-ə göndər
        String engQuery = translator.translateToEnglish(message);
        double savings  = totalIncome - totalExpense;

        String context = mobileBERT.createFinancialContext(
                totalIncome, totalExpense, monthlySalary, savings);
        String engResp  = mobileBERT.answerQuestion(engQuery, context);
        String finalResp = translator.translateFromEnglish(engResp);

        Log.d(TAG, "🤖 Cavab: " + finalResp);
        addBotMessage(finalResp);
        scrollToBottom();
    }

    // ✅ YENİLƏNMİŞ SMART RESPONSE (Proqnoz düzgün işləyir)
    private String getSmartResponse(String message) {
        String q = message.toLowerCase(new Locale("az")).trim();

        // ✅ REAL xərc dəyərini yenilə (hər sualda)
        loadFinancialDataFromPreferences();

        double savings     = totalIncome - totalExpense;
        double savingsRate = getSavingsRate();

        // Debug üçün
        Log.d(TAG, "📊 SmartResponse - Xərc: " + totalExpense + ", Qənaət dərəcəsi: " + savingsRate);

        // --- PROQNOZ (ƏN VACİB HİSSƏ) ---
        if (has(q, "proqnoz", "forecast", "прогноз", "olacaq", "nə qədər xərcləyərəm")) {
            // ✅ Əgər xərc 0-dırsa, test dəyəri istifadə et
            double actualExpense = totalExpense;
            if (actualExpense <= 0) {
                Log.d(TAG, "⚠️ Proqnoz üçün test dəyəri istifadə olunur: " + actualExpense);
            }

            double dailyAvg = actualExpense / 30.0;
            double forecast3d = dailyAvg * 3;
            double forecast7d = dailyAvg * 7;
            double forecast30d = dailyAvg * 30;
            double savingsPotential = dailyAvg * 0.2 * 7;

            String currentDate = new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date());
            String date3d = getDateAfterDays(3);
            String date7d = getDateAfterDays(7);

            return String.format(Locale.getDefault(),
                    t(
                            "🔮 PROQNOZ (%s - %s)\n" +
                                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                    "📊 Cari vəziyyət:\n" +
                                    "   💸 Ümumi xərc: ₼%.2f (30 gün)\n" +
                                    "   📅 Gündəlik orta: ₼%.2f\n\n" +
                                    "📈 3 GÜNLÜK (%s - %s):\n" +
                                    "   • Gözlənilən xərc: ₼%.2f\n" +
                                    "   • Potensial qənaət: ₼%.2f (20%%)\n\n" +
                                    "📈 7 GÜNLÜK (%s - %s):\n" +
                                    "   • Gözlənilən xərc: ₼%.2f\n\n" +
                                    "📈 30 GÜNLÜK PROQNOZ:\n" +
                                    "   • Gözlənilən xərc: ₼%.2f\n" +
                                    "   • Potensial qənaət: ₼%.2f (20%%)\n\n" +
                                    "💡 İpucu: Gündəlik ₼%.2f qənaət etsəniz,\n" +
                                    "   ayda ₼%.2f yığmış olarsınız!",

                            "🔮 FORECAST (%s - %s)\n" +
                                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                    "📊 Current status:\n" +
                                    "   💸 Total expense: ₼%.2f (30 days)\n" +
                                    "   📅 Daily average: ₼%.2f\n\n" +
                                    "📈 3 DAY (%s - %s):\n" +
                                    "   • Expected expense: ₼%.2f\n" +
                                    "   • Potential savings: ₼%.2f (20%%)\n\n" +
                                    "📈 7 DAY (%s - %s):\n" +
                                    "   • Expected expense: ₼%.2f\n\n" +
                                    "📈 30 DAY FORECAST:\n" +
                                    "   • Expected expense: ₼%.2f\n" +
                                    "   • Potential savings: ₼%.2f (20%%)\n\n" +
                                    "💡 Tip: If you save ₼%.2f daily,\n" +
                                    "   you'll save ₼%.2f per month!",

                            "🔮 ПРОГНОЗ (%s - %s)\n" +
                                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                    "📊 Текущая ситуация:\n" +
                                    "   💸 Всего расходов: ₼%.2f (30 дней)\n" +
                                    "   📅 Среднесуточные: ₼%.2f\n\n" +
                                    "📈 3 ДНЯ (%s - %s):\n" +
                                    "   • Ожидаемый расход: ₼%.2f\n" +
                                    "   • Потенциальная экономия: ₼%.2f (20%%)\n\n" +
                                    "📈 7 ДНЕЙ (%s - %s):\n" +
                                    "   • Ожидаемый расход: ₼%.2f\n\n" +
                                    "📈 ПРОГНОЗ НА 30 ДНЕЙ:\n" +
                                    "   • Ожидаемый расход: ₼%.2f\n" +
                                    "   • Потенциальная экономия: ₼%.2f (20%%)\n\n" +
                                    "💡 Совет: Если экономить ₼%.2f в день,\n" +
                                    "   за месяц накопится ₼%.2f!"
                    ),
                    currentDate, date7d,
                    actualExpense, dailyAvg,
                    currentDate, date3d, forecast3d, savingsPotential,
                    currentDate, date7d, forecast7d,
                    forecast30d, savingsPotential,
                    dailyAvg * 0.2, dailyAvg * 0.2 * 30
            );
        }

        // --- XƏRCLƏR (DETALLI) ---
        if (has(q, "xərc", "xərclədim", "spend", "expense", "расход", "потратил")) {
            double dailyAvg = totalExpense / 30.0;
            return String.format(Locale.getDefault(),
                    t(
                            "📊 XƏRC HESABATI\n" +
                                    "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                    "💸 Ümumi xərc (30 gün): ₼%.2f\n" +
                                    "📅 Gündəlik orta: ₼%.2f\n" +
                                    "💰 Gəlir: ₼%.2f\n" +
                                    "💚 Qənaət: ₼%.2f\n" +
                                    "📈 Qənaət dərəcəsi: %.1f%%\n\n" +
                                    "%s",

                            "📊 EXPENSE REPORT\n" +
                                    "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                    "💸 Total expense (30 days): ₼%.2f\n" +
                                    "📅 Daily average: ₼%.2f\n" +
                                    "💰 Income: ₼%.2f\n" +
                                    "💚 Savings: ₼%.2f\n" +
                                    "📈 Savings rate: %.1f%%\n\n" +
                                    "%s",

                            "📊 ОТЧЁТ О РАСХОДАХ\n" +
                                    "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                    "💸 Всего расходов (30 дней): ₼%.2f\n" +
                                    "📅 Среднесуточные: ₼%.2f\n" +
                                    "💰 Доходы: ₼%.2f\n" +
                                    "💚 Сбережения: ₼%.2f\n" +
                                    "📈 Норма сбережений: %.1f%%\n\n" +
                                    "%s"
                    ),
                    totalExpense, dailyAvg, totalIncome, savings, savingsRate,
                    getExpenseAdvice(savingsRate)
            );
        }

        // --- QƏNAƏT ---
        if (has(q, "qənaət", "save", "saving", "сбереж", "накопил")) {
            return String.format(Locale.getDefault(),
                    t(
                            "💰 QƏNAƏT HESABATI\n" +
                                    "━━━━━━━━━━━━━━━━━━\n" +
                                    "✅ Net qənaət: ₼%.2f\n" +
                                    "📈 Qənaət dərəcəsi: %.1f%%\n" +
                                    "💵 Gəlir: ₼%.2f\n" +
                                    "💸 Xərc: ₼%.2f\n\n" +
                                    "%s",

                            "💰 SAVINGS REPORT\n" +
                                    "━━━━━━━━━━━━━━━━━━\n" +
                                    "✅ Net savings: ₼%.2f\n" +
                                    "📈 Savings rate: %.1f%%\n" +
                                    "💵 Income: ₼%.2f\n" +
                                    "💸 Expenses: ₼%.2f\n\n" +
                                    "%s",

                            "💰 ОТЧЁТ О СБЕРЕЖЕНИЯХ\n" +
                                    "━━━━━━━━━━━━━━━━━━\n" +
                                    "✅ Чистые сбережения: ₼%.2f\n" +
                                    "📈 Норма сбережений: %.1f%%\n" +
                                    "💵 Доходы: ₼%.2f\n" +
                                    "💸 Расходы: ₼%.2f\n\n" +
                                    "%s"
                    ),
                    savings, savingsRate, totalIncome, totalExpense,
                    getSavingsAdvice(savingsRate)
            );
        }

        // --- BALANS ---
        if (has(q, "balans", "balance", "баланс", "nə qədər var")) {
            String emoji = savings >= 0 ? "✅" : "⚠️";
            return String.format(Locale.getDefault(),
                    t(
                            "💳 BALANS\n━━━━━━━━━━━━━━━━━━\n" +
                                    "%s Cari balans: ₼%.2f\n" +
                                    "💵 Gəlir: ₼%.2f\n" +
                                    "💸 Xərc: ₼%.2f\n" +
                                    "📈 Qənaət dərəcəsi: %.1f%%",

                            "💳 BALANCE\n━━━━━━━━━━━━━━━━━━\n" +
                                    "%s Current balance: ₼%.2f\n" +
                                    "💵 Income: ₼%.2f\n" +
                                    "💸 Expenses: ₼%.2f\n" +
                                    "📈 Savings rate: %.1f%%",

                            "💳 БАЛАНС\n━━━━━━━━━━━━━━━━━━\n" +
                                    "%s Текущий баланс: ₼%.2f\n" +
                                    "💵 Доходы: ₼%.2f\n" +
                                    "💸 Расходы: ₼%.2f\n" +
                                    "📈 Норма сбережений: %.1f%%"
                    ),
                    emoji, savings, totalIncome, totalExpense, savingsRate
            );
        }

        // --- MƏSLƏHƏT ---
        if (has(q, "məsləhət", "advice", "tövsiyə", "совет")) {
            return getFullAdvice(savingsRate);
        }

        // --- RİSK ANALİZİ ---
        if (has(q, "risk", "анализ рисков", "təhlükə")) {
            return getRiskReport(savingsRate);
        }

        // --- GƏLİR ---
        if (has(q, "gəlir", "income", "maaş", "salary", "доход", "зарплата")) {
            return String.format(Locale.getDefault(),
                    t(
                            "💵 GƏLİR HESABATI\n━━━━━━━━━━━━━━━━━━\n" +
                                    "💰 Ümumi gəlir: ₼%.2f\n" +
                                    "🏦 Aylıq maaş: ₼%.2f\n" +
                                    "💚 Qənaət: ₼%.2f\n" +
                                    "📈 Qənaət dərəcəsi: %.1f%%",

                            "💵 INCOME REPORT\n━━━━━━━━━━━━━━━━━━\n" +
                                    "💰 Total income: ₼%.2f\n" +
                                    "🏦 Monthly salary: ₼%.2f\n" +
                                    "💚 Savings: ₼%.2f\n" +
                                    "📈 Savings rate: %.1f%%",

                            "💵 ОТЧЁТ О ДОХОДАХ\n━━━━━━━━━━━━━━━━━━\n" +
                                    "💰 Всего доходов: ₼%.2f\n" +
                                    "🏦 Зарплата: ₼%.2f\n" +
                                    "💚 Сбережения: ₼%.2f\n" +
                                    "📈 Норма сбережений: %.1f%%"
                    ),
                    totalIncome, monthlySalary, savings, savingsRate
            );
        }

        // --- SALAMLAŞMA ---
        if (has(q, "salam", "hello", "hi", "привет")) {
            return t(
                    "👋 Salam! Maliyyə vəziyyətiniz:\n" +
                            String.format("💰 Gəlir: ₼%.2f | 💸 Xərc: ₼%.2f | 💚 Qənaət: ₼%.2f", totalIncome, totalExpense, savings) +
                            "\n\nNə bilmək istəyirsiniz? (Proqnoz, xərc, məsləhət...)",

                    "👋 Hello! Your financial status:\n" +
                            String.format("💰 Income: ₼%.2f | 💸 Expense: ₼%.2f | 💚 Savings: ₼%.2f", totalIncome, totalExpense, savings) +
                            "\n\nWhat would you like to know? (Forecast, expense, advice...)",

                    "👋 Привет! Ваше финансовое состояние:\n" +
                            String.format("💰 Доходы: ₼%.2f | 💸 Расходы: ₼%.2f | 💚 Сбережения: ₼%.2f", totalIncome, totalExpense, savings) +
                            "\n\nЧто вы хотите узнать? (Прогноз, расходы, совет...)"
            );
        }

        // --- KÖMƏK ---
        if (has(q, "kömək", "help", "помощь")) {
            return t(
                    "💡 Sual nümunələri:\n" +
                            "• Proqnoz ver\n" +
                            "• Nə qədər xərclədim?\n" +
                            "• Məsləhət ver\n" +
                            "• Risk analizi et\n" +
                            "• Balansım nə qədərdir?\n" +
                            "• Gəlirim nə qədərdir?",

                    "💡 Question examples:\n" +
                            "• Give a forecast\n" +
                            "• How much did I spend?\n" +
                            "• Give advice\n" +
                            "• Risk analysis\n" +
                            "• What is my balance?\n" +
                            "• What is my income?",

                    "💡 Примеры вопросов:\n" +
                            "• Дай прогноз\n" +
                            "• Сколько я потратил?\n" +
                            "• Дай совет\n" +
                            "• Анализ рисков\n" +
                            "• Каков мой баланс?\n" +
                            "• Каков мой доход?"
            );
        }

        return null;
    }

    private String getDateAfterDays(int days) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM", Locale.getDefault());
        return sdf.format(new Date(System.currentTimeMillis() + days * 24L * 60 * 60 * 1000));
    }

    private String getExpenseAdvice(double rate) {
        if (rate < 0) return "🔴 XƏBƏRDARLIQ: Xərclər gəlirdən çoxdur!";
        if (rate < 10) return "⚠️ Qənaət çox aşağıdır. Xərcləri azaldın!";
        if (rate < 20) return "👍 Yaxşıdır, amma 20%-ə çatmağa çalışın.";
        return "🎉 Əla! Qənaət dərəcəniz çox yaxşıdır.";
    }

    private String getSavingsAdvice(double rate) {
        if (rate >= 30) return "🌟 Möhtəşəm! İnvestisiya etməyə başlayın.";
        if (rate >= 20) return "✅ Əla! Bu tempdə davam edin.";
        if (rate >= 10) return "👍 Yaxşı, daha da artıra bilərsiniz.";
        return "⚠️ Xərcləri azaltmağa çalışın.";
    }

    private String getFullAdvice(double rate) {
        if (rate < 0) {
            return t(
                    "🔴 TƏCİLİ MƏSLƏHƏT:\n" +
                            "• Xərcləriniz gəlirinizdən çoxdur!\n" +
                            "• Dərhal büdcə planı qurun\n" +
                            "• Kredit kartı istifadəsini dayandırın\n" +
                            "• Qeyri-zəruri xərcləri kəsin",

                    "🔴 URGENT ADVICE:\n" +
                            "• Your expenses exceed your income!\n" +
                            "• Create a budget plan immediately\n" +
                            "• Stop using credit cards\n" +
                            "• Cut unnecessary expenses",

                    "🔴 СРОЧНЫЙ СОВЕТ:\n" +
                            "• Расходы превышают доходы!\n" +
                            "• Немедленно составьте бюджет\n" +
                            "• Прекратите использовать кредитные карты\n" +
                            "• Сократите ненужные расходы"
            );
        }

        if (rate < 10) {
            return t(
                    "⚠️ MƏSLƏHƏT:\n" +
                            "• 50/30/20 qaydasını tətbiq edin\n" +
                            "• Hər ay ən az 20% qənaət edin\n" +
                            "• Gündəlik xərcləri qeyd edin\n" +
                            "• Abunəlikləri yoxlayın",

                    "⚠️ ADVICE:\n" +
                            "• Apply the 50/30/20 rule\n" +
                            "• Save at least 20% each month\n" +
                            "• Track daily expenses\n" +
                            "• Review your subscriptions",

                    "⚠️ СОВЕТ:\n" +
                            "• Применяйте правило 50/30/20\n" +
                            "• Откладывайте минимум 20% каждый месяц\n" +
                            "• Записывайте ежедневные расходы\n" +
                            "• Проверьте подписки"
            );
        }

        return t(
                "✅ YAXŞI VƏZİYYƏT:\n" +
                        "• Qənaət dərəcəniz yaxşıdır\n" +
                        "• İnvestisiya etməyə başlayın\n" +
                        "• Təcili yardım fondu yaradın\n" +
                        "• Uzunmüddətli hədəflər qoyun",

                "✅ GOOD STATUS:\n" +
                        "• Your savings rate is good\n" +
                        "• Start investing\n" +
                        "• Create an emergency fund\n" +
                        "• Set long-term goals",

                "✅ ХОРОШЕЕ СОСТОЯНИЕ:\n" +
                        "• Ваша норма сбережений хорошая\n" +
                        "• Начинайте инвестировать\n" +
                        "• Создайте резервный фонд\n" +
                        "• Ставьте долгосрочные цели"
        );
    }

    private String getRiskReport(double rate) {
        String level, emoji, advice;
        if (rate < 0) {
            level = t("YÜKSƏK RİSK", "HIGH RISK", "ВЫСОКИЙ РИСК");
            emoji = "🔴";
            advice = t("Dərhal tədbir görün!", "Take action immediately!", "Немедленно примите меры!");
        } else if (rate < 10) {
            level = t("ORTA RİSK", "MEDIUM RISK", "СРЕДНИЙ РИСК");
            emoji = "🟡";
            advice = t("Xərcləri azaldın", "Reduce expenses", "Сократите расходы");
        } else {
            level = t("AŞAĞI RİSK", "LOW RISK", "НИЗКИЙ РИСК");
            emoji = "🟢";
            advice = t("Davam edin!", "Keep going!", "Продолжайте!");
        }

        return String.format(Locale.getDefault(),
                t(
                        "%s RİSK ANALİZİ\n━━━━━━━━━━━━━━━━━━\n" +
                                "📊 Səviyyə: %s\n" +
                                "📈 Qənaət dərəcəsi: %.1f%%\n" +
                                "💰 Gəlir: ₼%.2f\n" +
                                "💸 Xərc: ₼%.2f\n\n" +
                                "💡 %s",

                        "%s RISK ANALYSIS\n━━━━━━━━━━━━━━━━━━\n" +
                                "📊 Level: %s\n" +
                                "📈 Savings rate: %.1f%%\n" +
                                "💰 Income: ₼%.2f\n" +
                                "💸 Expenses: ₼%.2f\n\n" +
                                "💡 %s",

                        "%s АНАЛИЗ РИСКОВ\n━━━━━━━━━━━━━━━━━━\n" +
                                "📊 Уровень: %s\n" +
                                "📈 Норма сбережений: %.1f%%\n" +
                                "💰 Доходы: ₼%.2f\n" +
                                "💸 Расходы: ₼%.2f\n\n" +
                                "💡 %s"
                ),
                emoji, level, rate, totalIncome, totalExpense, advice
        );
    }

    private boolean has(String query, String... words) {
        for (String w : words) if (query.contains(w)) return true;
        return false;
    }

    private String t(String az, String en, String ru) {
        if (translator == null) return az;
        switch (translator.getCurrentLanguage()) {
            case "en": return en;
            case "ru": return ru;
            default:   return az;
        }
    }

    private void addUserMessage(String text) {
        if (chatContainer == null) return;
        messages.add(new ChatMessage(text, true));

        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.item_chat_user, chatContainer, false);
        ((TextView) v.findViewById(R.id.tvMessage)).setText(text);
        chatContainer.addView(v);
        hideEmpty();
    }

    private void addBotMessage(String text) {
        if (chatContainer == null) {
            Log.e(TAG, "chatContainer null-dur!");
            return;
        }
        messages.add(new ChatMessage(text, false));

        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.item_chat_bot, chatContainer, false);
        TextView tv = v.findViewById(R.id.tvMessage);
        if (tv == null) {
            Log.e(TAG, "tvMessage null-dur!");
            return;
        }

        tv.setText(text);
        tv.setMovementMethod(new ScrollingMovementMethod());
        chatContainer.addView(v);
        chatContainer.post(() -> {
            int count = chatContainer.getChildCount();
            if (count > 0) chatContainer.getChildAt(count - 1).requestLayout();
        });
        chatContainer.invalidate();
        hideEmpty();
    }

    private void addAIAnalysisMessage(String analysis) {
        if (chatContainer == null) return;
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.item_chat_bot, chatContainer, false);
        TextView tv = v.findViewById(R.id.tvMessage);
        tv.setText(t("🤖 AI Analiz Nəticəsi:\n\n",
                "🤖 AI Analysis Result:\n\n",
                "🤖 Результат AI Анализа:\n\n") + analysis);
        tv.setMovementMethod(new ScrollingMovementMethod());
        chatContainer.addView(v);
        hideEmpty();
    }

    private void hideEmpty() {
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
    }

    private void scrollToBottom() {
        if (chatContainer == null) return;
        chatContainer.post(() -> {
            int c = chatContainer.getChildCount();
            if (c > 0) chatContainer.getChildAt(c - 1).requestLayout();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setGravity(Gravity.BOTTOM);
            getDialog().getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mobileBERT != null) mobileBERT.close();
    }

    private static class ChatMessage {
        final String  message;
        final boolean isUser;
        ChatMessage(String m, boolean u) { message = m; isUser = u; }
    }
}