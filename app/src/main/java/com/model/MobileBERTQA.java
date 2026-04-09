package com.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MobileBERTQA {

    private static final String TAG = "MobileBERTQA";
    private Context context;
    private Random random;

    // Maliyyə məlumatları (cache)
    private double cachedTotalIncome = 0;
    private double cachedTotalExpense = 0;
    private double cachedMonthlySalary = 0;
    private double cachedSavings = 0;

    // ✅ YENİ: Gündəlik xərclər üçün tarixçə
    private double[] dailyExpenses = new double[30];
    private long lastUpdateTime = 0;

    public MobileBERTQA(Context context) {
        this.context = context;
        this.random = new Random();
        loadCachedData();
        loadDailyExpenses();
        Log.d(TAG, "✅ MobileBERTQA hazırdır! Proqnoz sistemi aktiv.");
    }

    private void loadCachedData() {
        SharedPreferences prefs = context.getSharedPreferences("finance_data", Context.MODE_PRIVATE);
        cachedTotalIncome = prefs.getFloat("total_income", 0);
        cachedTotalExpense = prefs.getFloat("total_expense", 0);
        cachedMonthlySalary = prefs.getFloat("monthly_salary", 0);
        cachedSavings = cachedTotalIncome - cachedTotalExpense;

        Log.d(TAG, "📊 Yüklənmiş məlumatlar: Gəlir=" + cachedTotalIncome + ", Xərc=" + cachedTotalExpense);
    }

    private void loadDailyExpenses() {
        SharedPreferences prefs = context.getSharedPreferences("daily_expenses", Context.MODE_PRIVATE);
        for (int i = 0; i < 30; i++) {
            dailyExpenses[i] = prefs.getFloat("day_" + i, 0);
        }
        lastUpdateTime = prefs.getLong("last_update", 0);
    }

    private void saveDailyExpenses() {
        SharedPreferences prefs = context.getSharedPreferences("daily_expenses", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < 30; i++) {
            editor.putFloat("day_" + i, (float) dailyExpenses[i]);
        }
        editor.putLong("last_update", System.currentTimeMillis());
        editor.apply();
    }

    public void updateFinancialData(double totalIncome, double totalExpense, double monthlySalary) {
        this.cachedTotalIncome = totalIncome;
        this.cachedTotalExpense = totalExpense;
        this.cachedMonthlySalary = monthlySalary;
        this.cachedSavings = totalIncome - totalExpense;

        SharedPreferences prefs = context.getSharedPreferences("finance_data", Context.MODE_PRIVATE);
        prefs.edit()
                .putFloat("total_income", (float) totalIncome)
                .putFloat("total_expense", (float) totalExpense)
                .putFloat("monthly_salary", (float) monthlySalary)
                .apply();

        Log.d(TAG, "📊 Maliyyə məlumatları yeniləndi: Gəlir=" + totalIncome + ", Xərc=" + totalExpense);
    }

    // ✅ YENİ: Gündəlik xərc əlavə etmək üçün
    public void addDailyExpense(double amount) {
        // Array-i sağa sürüşdür
        for (int i = 28; i >= 0; i--) {
            dailyExpenses[i + 1] = dailyExpenses[i];
        }
        dailyExpenses[0] = amount;
        saveDailyExpenses();
        Log.d(TAG, "📝 Gündəlik xərc əlavə edildi: " + amount);
    }

    public String answerQuestion(String question, String contextText) {
        Log.d(TAG, "🔍 Sual: " + question);

        if (question == null || question.trim().isEmpty()) {
            return getRandomGreeting();
        }

        String q = question.toLowerCase().trim();

        // 1. SALAMLAŞMA
        if (containsAny(q, "salam", "hello", "hi", "hey", "merhaba", "привет")) {
            return getRandomGreeting();
        }

        // 2. KÖMƏK
        if (containsAny(q, "kömək", "help", "yardım", "nece", "how to", "помощь")) {
            return getHelpMessage();
        }

        // 3. XƏRC SUALLARI
        if (containsAny(q, "xərc", "expense", "spend", "spent", "harcama", "потратил", "расход")) {
            return getExpenseAnswer(q);
        }

        // 4. GƏLİR SUALLARI
        if (containsAny(q, "gəlir", "income", "earn", "maaş", "salary", "доход", "зарплата")) {
            return getIncomeAnswer(q);
        }

        // 5. QƏNAƏT SUALLARI
        if (containsAny(q, "qənaət", "saving", "save", "economy", "экономия")) {
            return getSavingsAnswer(q);
        }

        // 6. BALANS
        if (containsAny(q, "balans", "balance", "qalıq", "remain", "баланс", "остаток")) {
            return getBalanceAnswer();
        }

        // 7. MƏSLƏHƏT
        if (containsAny(q, "məsləhət", "advice", "tövsiyə", "recommend", "совет")) {
            return getAdvice();
        }

        // 8. PROQNOZ - ✅ DÜZƏLDİLDİ
        if (containsAny(q, "proqnoz", "forecast", "predict", "olacaq", "прогноз")) {
            return getAccurateForecast();  // ✅ Yeni metod
        }

        // 9. TƏŞƏKKÜR
        if (containsAny(q, "təşəkkür", "thank", "sağol", "thanks", "спасибо")) {
            return getThankYouMessage();
        }

        // 10. ANALİZ
        if (containsAny(q, "analiz", "analysis", "təhlil", "analiza", "анализ")) {
            return getAnalysisAnswer();
        }

        // 11. MÜQAYİSƏ
        if (containsAny(q, "müqayisə", "compare", "comparison", "arasında", "сравнение")) {
            return getComparisonAnswer();
        }

        // 12. DEFAULT CAVAB
        return getDefaultAnswer();
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String getExpenseAnswer(String question) {
        // ✅ Real məlumatları yoxla
        if (cachedTotalExpense == 0) {
            // SharedPreferences-dən təkrar yoxla
            SharedPreferences prefs = context.getSharedPreferences("finance_data", Context.MODE_PRIVATE);
            cachedTotalExpense = prefs.getFloat("total_expense", 0);

            if (cachedTotalExpense == 0) {
                return "📭 Hələ heç bir xərc məlumatı yoxdur. Əvvəlcə əməliyyatlar əlavə edin!\n\n💡 İpucu: Dashboard-da '➕ Məlumat Əlavə Et' kartına klikləyin.";
            }
        }

        // Günlük, həftəlik, aylıq sorğusu
        if (containsAny(question, "gün", "today", "bugün", "день")) {
            double todayExpense = dailyExpenses[0];
            return String.format("📆 Bugünkü xərcləriniz: %.2f AZN\n%s",
                    todayExpense,
                    todayExpense > 50 ? "⚠️ Bugünkü xərcləriniz yüksəkdir!" : "✅ Normal səviyyədədir.");
        } else if (containsAny(question, "həftə", "week", "неделя")) {
            double weekExpense = 0;
            for (int i = 0; i < 7; i++) {
                weekExpense += dailyExpenses[i];
            }
            return String.format("📊 Həftəlik xərcləriniz: %.2f AZN\n📈 Orta gündəlik: %.2f AZN",
                    weekExpense, weekExpense / 7);
        } else if (containsAny(question, "ay", "month", "месяц")) {
            return String.format("📈 Aylıq ümumi xərcləriniz: %.2f AZN", cachedTotalExpense);
        }

        return String.format("💰 Ümumi xərcləriniz: %.2f AZN\n📊 Ətraflı analiz üçün Dashboard-da 'AI Insights' kartını açın.",
                cachedTotalExpense);
    }

    private String getIncomeAnswer(String question) {
        if (cachedTotalIncome == 0) {
            SharedPreferences prefs = context.getSharedPreferences("finance_data", Context.MODE_PRIVATE);
            cachedTotalIncome = prefs.getFloat("total_income", 0);

            if (cachedTotalIncome == 0) {
                return "📭 Hələ heç bir gəlir məlumatı yoxdur. Maaş və digər gəlirləri əlavə edin!";
            }
        }

        if (containsAny(question, "maaş", "salary", "aylıq", "monthly")) {
            return String.format("💵 Aylıq maaşınız: %.2f AZN", cachedMonthlySalary);
        }

        return String.format("💵 Ümumi gəliriniz: %.2f AZN\n🏦 Aylıq maaş: %.2f AZN",
                cachedTotalIncome, cachedMonthlySalary);
    }

    private String getSavingsAnswer(String question) {
        double savings = cachedTotalIncome - cachedTotalExpense;
        double savingsRate = cachedTotalIncome > 0 ? (savings / cachedTotalIncome) * 100 : 0;

        if (cachedTotalIncome == 0) {
            SharedPreferences prefs = context.getSharedPreferences("finance_data", Context.MODE_PRIVATE);
            cachedTotalIncome = prefs.getFloat("total_income", 0);
            cachedTotalExpense = prefs.getFloat("total_expense", 0);
            savings = cachedTotalIncome - cachedTotalExpense;
            savingsRate = cachedTotalIncome > 0 ? (savings / cachedTotalIncome) * 100 : 0;

            if (cachedTotalIncome == 0) {
                return "📭 Qənaət hesablamaq üçün əvvəlcə gəlir məlumatlarınızı əlavə edin.";
            }
        }

        String status;
        if (savingsRate >= 20) {
            status = "🎉 Mükəmməl! Qənaət dərəcəniz çox yaxşıdır!";
        } else if (savingsRate >= 10) {
            status = "👍 Yaxşı! Ancaq daha da yaxşılaşdıra bilərsiniz.";
        } else if (savingsRate > 0) {
            status = "⚠️ Qənaət dərəcəniz aşağıdır. Xərclərinizi azaltmağa çalışın.";
        } else {
            status = "🔴 Xərcləriniz gəlirinizdən çoxdur! Təcili olaraq büdcənizi nəzərdən keçirin.";
        }

        return String.format("💰 Qənaətiniz: %.2f AZN\n📊 Qənaət dərəcəsi: %.1f%%\n%s",
                savings, savingsRate, status);
    }

    private String getBalanceAnswer() {
        double balance = cachedTotalIncome - cachedTotalExpense;
        String emoji = balance >= 0 ? "💚" : "🔴";
        return String.format("%s Hazırkı balansınız: %.2f AZN\n%s",
                emoji, balance,
                balance >= 0 ? "✅ Maliyyə vəziyyətiniz yaxşıdır!" : "⚠️ Xərcləriniz gəlirinizdən çoxdur!");
    }

    private String getAdvice() {
        double savings = cachedTotalIncome - cachedTotalExpense;
        double savingsRate = cachedTotalIncome > 0 ? (savings / cachedTotalIncome) * 100 : 0;

        String[] goodAdvice = {
                "💡 50/30/20 qaydasını tətbiq edin: 50% ehtiyaclar, 30% istəklər, 20% qənaət",
                "📝 Gündəlik xərclərinizi qeyd edin - fərqində olmadığınız xərcləri görəcəksiniz",
                "🏦 Təcili yardım fondu yaradın (3-6 aylıq xərciniz qədər)",
                "📊 Ay sonunda büdcənizi analiz edin və növbəti ay üçün plan qurun",
                "🛍️ Alış-verişdən əvvəl siyahı hazırlayın və lazımsız alışlardan çəkinin",
                "💳 Kredit kartı istifadəsini məhdudlaşdırın, mümkünsə nağd ödəniş edin"
        };

        if (savingsRate < 10) {
            return "⚠️ XƏBƏRDARLIQ: Qənaət dərəcəniz çox aşağıdır!\n\n" +
                    "🎯 Tövsiyələr:\n" +
                    "• Lazımsız abunəlikləri ləğv edin\n" +
                    "• Kafe/restoran xərclərini azaldın\n" +
                    "• Evdə yemək bişirməyə çalışın\n" +
                    "• Endirimləri izləyin və ağıllı alış-veriş edin\n\n" +
                    goodAdvice[random.nextInt(goodAdvice.length)];
        } else if (savingsRate < 20) {
            return "👍 Yaxşı gedirsiniz! Ancaq daha yaxşı ola bilər:\n\n" +
                    "🎯 Tövsiyələr:\n" +
                    "• Aylıq qənaət hədəfinizi 20%-ə çıxarın\n" +
                    "• Artıq pulunuzu investisiya etməyə başlayın\n" +
                    "• Passiv gəlir mənbələri araşdırın\n\n" +
                    goodAdvice[random.nextInt(goodAdvice.length)];
        } else {
            return "🎉 MÖHTƏŞƏM! Maliyyə vəziyyətiniz çox yaxşıdır!\n\n" +
                    "🎯 Tövsiyələr:\n" +
                    "• İnvestisiya portfelinizi şaxələndirin\n" +
                    "• Uzunmüddətli maliyyə hədəfləri qoyun\n" +
                    "• Karyeranızda irəliləmək üçün investisiya edin\n\n" +
                    goodAdvice[random.nextInt(goodAdvice.length)];
        }
    }

    // ✅ YENİ: DƏQİQ PROQNOZ METODU
    private String getAccurateForecast() {
        Log.d(TAG, "📊 Proqnoz hesablanır...");
        Log.d(TAG, "cachedTotalExpense: " + cachedTotalExpense);
        Log.d(TAG, "dailyExpenses[0]: " + dailyExpenses[0]);

        // 1. Məlumatları yenilə
        SharedPreferences prefs = context.getSharedPreferences("finance_data", Context.MODE_PRIVATE);
        double realExpense = prefs.getFloat("total_expense", 0);
        if (realExpense > 0) {
            cachedTotalExpense = realExpense;
        }

        // 2. Orta gündəlik xərci hesabla
        double totalDaysWithData = 0;
        double totalExpenseSum = 0;

        for (int i = 0; i < 30; i++) {
            if (dailyExpenses[i] > 0) {
                totalExpenseSum += dailyExpenses[i];
                totalDaysWithData++;
            }
        }

        // Əgər dailyExpenses-də məlumat yoxdursa, ümumi xərcdən hesabla
        double avgDailyExpense;
        if (totalDaysWithData > 0) {
            avgDailyExpense = totalExpenseSum / totalDaysWithData;
            Log.d(TAG, "DailyExpenses-dən hesablandı: orta=" + avgDailyExpense + ", gün sayı=" + totalDaysWithData);
        } else if (cachedTotalExpense > 0) {
            avgDailyExpense = cachedTotalExpense / 30;
            Log.d(TAG, "Ümumi xərcdən hesablandı: orta=" + avgDailyExpense);
        } else {
            avgDailyExpense = 20; // Default orta gündəlik xərc
            Log.d(TAG, "Default dəyər istifadə olunur: orta=" + avgDailyExpense);
        }

        // 3. Proqnozları hesabla
        double forecast3d = avgDailyExpense * 3;
        double forecast7d = avgDailyExpense * 7;
        double forecast30d = avgDailyExpense * 30;

        // 4. Qənaət potensialı (20% qənaətlə)
        double savingsPotential3d = avgDailyExpense * 0.2 * 3;
        double savingsPotential30d = avgDailyExpense * 0.2 * 30;

        // 5. Trend analizi (son 7 gün vs əvvəlki 7 gün)
        double lastWeek = 0;
        double prevWeek = 0;
        for (int i = 0; i < 7; i++) {
            lastWeek += dailyExpenses[i];
            prevWeek += dailyExpenses[i + 7];
        }

        String trend;
        if (lastWeek > prevWeek * 1.1) {
            trend = "📈 Xərclər ARTIR (⚠️ diqqət!)";
        } else if (lastWeek < prevWeek * 0.9) {
            trend = "📉 Xərclər AZALIR (✅ yaxşı)";
        } else {
            trend = "➡️ Xərclər SABİTdir";
        }

        // 6. Aylıq proqnoz (cari xərc trendinə görə)
        double monthlyForecast;
        if (cachedTotalExpense > 0) {
            monthlyForecast = cachedTotalExpense * (1 + (lastWeek - prevWeek) / Math.max(prevWeek, 1));
        } else {
            monthlyForecast = forecast30d;
        }

        String date = new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date());
        String date3d = getDateAfterDays(3);
        String date7d = getDateAfterDays(7);

        return String.format(
                "🔮 PROQNOZ (%s - %s)\n" +
                        "══════════════════════════════\n\n" +
                        "📊 ORTA GÜNDƏLİK XƏRC: %.2f AZN\n\n" +
                        "📈 3 GÜNLÜK PROQNOZ (%s - %s):\n" +
                        "   • Gözlənilən xərc: %.2f AZN\n" +
                        "   • Potensial qənaət: %.2f AZN\n\n" +
                        "📊 7 GÜNLÜK PROQNOZ (%s - %s):\n" +
                        "   • Gözlənilən xərc: %.2f AZN\n\n" +
                        "📅 AYLİQ PROQNOZ:\n" +
                        "   • Cari xərc: %.2f AZN\n" +
                        "   • Proqnoz: %.2f AZN\n" +
                        "   • Potensial qənaət: %.2f AZN\n\n" +
                        "📈 TREND ANALİZİ:\n" +
                        "   %s\n\n" +
                        "💡 TÖVSİYƏ:\n" +
                        "   • Gündəlik %.2f AZN qənaət etsəniz,\n" +
                        "     ayda %.2f AZN yığmış olarsınız!",
                date, date7d,
                avgDailyExpense,
                date, date3d, forecast3d, savingsPotential3d,
                date, date7d, forecast7d,
                cachedTotalExpense, monthlyForecast, savingsPotential30d,
                trend,
                avgDailyExpense * 0.2, savingsPotential30d
        );
    }

    // Köhnə getForecast metodunu saxla (fallback üçün)
    private String getForecast() {
        return getAccurateForecast();
    }

    private String getDateAfterDays(int days) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM", Locale.getDefault());
        return sdf.format(new Date(System.currentTimeMillis() + days * 24L * 60 * 60 * 1000));
    }

    private String getAnalysisAnswer() {
        double balance = cachedTotalIncome - cachedTotalExpense;
        String healthStatus;

        if (balance >= 1000) {
            healthStatus = "🌟 ƏLA - Maliyyə vəziyyətiniz çox güclüdür!";
        } else if (balance >= 500) {
            healthStatus = "👍 YAXŞI - Maliyyə vəziyyətiniz sabitdir.";
        } else if (balance >= 0) {
            healthStatus = "⚠️ ORTA - Daha çox qənaət etməyə çalışın.";
        } else {
            healthStatus = "🔴 ZƏİF - Xərclərinizi təcili nəzərdən keçirin!";
        }

        return String.format(
                "📊 MALİYYƏ ANALİZİ\n" +
                        "══════════════════════\n\n" +
                        "💰 Gəlir: %.2f AZN\n" +
                        "💸 Xərc: %.2f AZN\n" +
                        "💚 Balans: %.2f AZN\n" +
                        "📈 Sağlamlıq: %s\n\n" +
                        "💡 Daha ətraflı analiz üçün Dashboard-da 'AI Insights' bölməsini açın.",
                cachedTotalIncome, cachedTotalExpense, balance, healthStatus
        );
    }

    private String getComparisonAnswer() {
        if (cachedTotalIncome == 0 || cachedTotalExpense == 0) {
            SharedPreferences prefs = context.getSharedPreferences("finance_data", Context.MODE_PRIVATE);
            cachedTotalIncome = prefs.getFloat("total_income", 0);
            cachedTotalExpense = prefs.getFloat("total_expense", 0);

            if (cachedTotalIncome == 0 || cachedTotalExpense == 0) {
                return "📊 Müqayisə üçün kifayət qədər məlumat yoxdur.";
            }
        }

        double difference = cachedTotalIncome - cachedTotalExpense;
        String comparison;

        if (difference > 0) {
            comparison = String.format("✅ Gəliriniz xərclərinizdən %.2f AZN çoxdur. (%d%% daha çox)",
                    difference, (int)((difference / cachedTotalExpense) * 100));
        } else if (difference < 0) {
            comparison = String.format("⚠️ Xərcləriniz gəlirinizdən %.2f AZN çoxdur. (%d%% daha çox)",
                    Math.abs(difference), (int)((Math.abs(difference) / cachedTotalIncome) * 100));
        } else {
            comparison = "📊 Gəlir və xərcləriniz bərabərdir. Qənaət etmək üçün xərcləri azaldın!";
        }

        return String.format(
                "📊 GƏLİR VS XƏRC MÜQAYİSƏSİ\n" +
                        "══════════════════════\n\n" +
                        "💵 Gəlir: %.2f AZN\n" +
                        "💸 Xərc: %.2f AZN\n" +
                        "%s",
                cachedTotalIncome, cachedTotalExpense, comparison
        );
    }

    private String getRandomGreeting() {
        String[] greetings = {
                "👋 Salam! Mən sizin AI maliyyə köməkçinizəm. Necə kömək edə bilərəm?",
                "🤖 Bəli, buyurun! Maliyyə suallarınızı cavablandırmağa hazıram.",
                "💬 Salam! Xərc, gəlir və ya qənaət haqqında sualınız varmı?",
                "📊 Maliyyə köməkçiniz aktivdir! Nə bilmək istəyirsiniz?"
        };
        return greetings[random.nextInt(greetings.length)];
    }

    private String getHelpMessage() {
        return "🤖 MALİYYƏ KÖMƏKÇİSİ - Sual edə bilərsiniz:\n\n" +
                "💰 Xərc haqqında: \"Nə qədər xərc etdim?\"\n" +
                "💵 Gəlir haqqında: \"Gəlirim nə qədərdir?\"\n" +
                "💚 Qənaət: \"Nə qədər qənaət etdim?\"\n" +
                "📊 Balans: \"Balansım nə qədərdir?\"\n" +
                "💡 Məsləhət: \"Mənə məsləhət ver\"\n" +
                "🔮 Proqnoz: \"3 günlük proqnoz ver\"\n" +
                "📈 Analiz: \"Maliyyəmi analiz et\"\n" +
                "⚖️ Müqayisə: \"Gəlir və xərcləri müqayisə et\"\n\n" +
                "🌐 İstənilən dildə sual verə bilərsiniz (Az, En, Ru)!";
    }

    private String getThankYouMessage() {
        String[] thanks = {
                "😊 Buyurun! Başqa sualınız olarsa, çəkinməyin.",
                "👍 Rica edərəm! Maliyyə uğurlarınız bol olsun!",
                "💪 Hər zaman köməyə hazırıq! Sağ olun.",
                "📊 Başqa analiz və ya proqnoz istəyirsiniz?"
        };
        return thanks[random.nextInt(thanks.length)];
    }

    private String getDefaultAnswer() {
        String[] defaults = {
                "🤔 Sualınızı tam başa düşmədim. Xərc, gəlir, qənaət, balans, məsləhət və ya proqnoz haqqında soruşa bilərsiniz.\n\n💡 Məsələn: '3 günlük proqnoz ver'",
                "📊 Maliyyə mövzusunda suallar verin: Xərc, gəlir, qənaət, balans, məsləhət, proqnoz.\n\n💡 'Mənə kömək et' yazaraq bütün sual növlərini görə bilərsiniz.",
                "💡 İpucu: '3 günlük proqnoz ver' yazaraq xərc proqnozunuzu öyrənə bilərsiniz!",
                "🔍 Sualınızı bir az daha aydın şəkildə yazın. Məsələn: 'Nə qədər xərc etdim?'"
        };
        return defaults[random.nextInt(defaults.length)];
    }

    public String createFinancialContext(double totalIncome, double totalExpense, double monthlySalary, double savings) {
        updateFinancialData(totalIncome, totalExpense, monthlySalary);
        return String.format(
                "Total income: %.2f AZN. Total expenses: %.2f AZN. Monthly salary: %.2f AZN. Net savings: %.2f AZN.",
                totalIncome, totalExpense, monthlySalary, savings
        );
    }

    public void close() {
        Log.d(TAG, "MobileBERTQA closed.");
    }
}