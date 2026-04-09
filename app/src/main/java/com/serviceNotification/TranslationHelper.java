package com.serviceNotification;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class TranslationHelper {

    private static TranslationHelper instance;
    private Context context;
    private Map<String, String> azToEn;
    private Map<String, String> enToAz;
    private Map<String, String> ruToEn;
    private Map<String, String> enToRu;

    private String currentLanguage = "az";

    private TranslationHelper(Context context) {
        this.context = context;
        loadDictionaries();
    }

    public static synchronized TranslationHelper getInstance(Context context) {
        if (instance == null) {
            instance = new TranslationHelper(context.getApplicationContext());
        }
        return instance;
    }

    private void loadDictionaries() {
        azToEn = new HashMap<>();
        enToAz = new HashMap<>();
        ruToEn = new HashMap<>();
        enToRu = new HashMap<>();

        // Fayllardan yükləməyə çalış
        loadTranslationDict("translations_az_en.txt", azToEn);
        loadTranslationDict("translations_en_az.txt", enToAz);
        loadTranslationDict("translations_ru_en.txt", ruToEn);
        loadTranslationDict("translations_en_ru.txt", enToRu);

        // Default tərcümələr
        addDefaultTranslations();
    }

    private void loadTranslationDict(String fileName, Map<String, String> dict) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(fileName))
            );
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    dict.put(parts[0].trim().toLowerCase(), parts[1].trim());
                }
            }
            reader.close();
        } catch (Exception e) {
            // Fayl yoxdur, default istifadə olunacaq
        }
    }

    private void addDefaultTranslations() {
        addDefaultAzEn();
        addDefaultEnAz();
        addDefaultRuEn();
        addDefaultEnRu();
    }

    private void addDefaultAzEn() {
        String[][] pairs = {
                // Ümumi sözlər
                {"salam", "hello"}, {"necesen", "how are you"}, {"xerc", "expense"},
                {"gelir", "income"}, {"qenaet", "savings"}, {"umumi", "total"},
                {"ayliq", "monthly"}, {"gunluk", "daily"}, {"heftelik", "weekly"},
                {"balans", "balance"}, {"mebleg", "amount"}, {"pul", "money"},
                {"xerc etdim", "spent"}, {"qazandim", "earned"}, {"budce", "budget"},
                {"tovsiye", "advice"}, {"proqnoz", "prediction"}, {"muqayise", "compare"},
                {"kateqoriya", "category"}, {"analiz", "analysis"}, {"maliyye", "finance"},
                {"investisiya", "investment"}, {"menfeet", "profit"}, {"zerer", "loss"},
                // Chatbot üçün əlavələr
                {"nə qədər", "how much"}, {"xərclədim", "did i spend"}, {"xərcim", "my expense"},
                {"maaş", "salary"}, {"aylıq maaş", "monthly salary"}, {"cari balans", "current balance"}
        };
        for (String[] pair : pairs) {
            azToEn.put(pair[0], pair[1]);
            enToAz.put(pair[1], pair[0]);
        }
    }

    private void addDefaultEnAz() {
        String[][] pairs = {
                {"how much", "nə qədər"}, {"tell me", "mənə de"}, {"help", "kömək"},
                {"please", "zəhmət olmasa"}, {"thank you", "təşəkkür edirəm"},
                {"good", "yaxşı"}, {"bad", "pis"}, {"great", "əla"},
                {"spending", "xərc"}, {"earning", "qazanc"}, {"saving", "qənaət"},
                {"more", "çox"}, {"less", "az"}, {"same", "eyni"},
                // Chatbot üçün
                {"did i spend", "xərclədim"}, {"my expenses", "xərclərim"},
                {"my income", "gəlirim"}, {"my savings", "qənaətim"},
                {"give me", "mənə ver"}, {"financial advice", "maliyyə məsləhəti"}
        };
        for (String[] pair : pairs) {
            enToAz.put(pair[0], pair[1]);
        }
    }

    private void addDefaultRuEn() {
        String[][] pairs = {
                {"здравствуйте", "hello"}, {"расход", "expense"}, {"доход", "income"},
                {"экономия", "savings"}, {"общий", "total"}, {"ежемесячный", "monthly"},
                {"ежедневный", "daily"}, {"баланс", "balance"}, {"сумма", "amount"},
                {"деньги", "money"}, {"потратил", "spent"}, {"заработал", "earned"},
                {"бюджет", "budget"}, {"совет", "advice"}, {"прогноз", "prediction"},
                {"сравнить", "compare"}, {"категория", "category"}, {"анализ", "analysis"},
                {"финансы", "finance"}, {"инвестиция", "investment"}, {"прибыль", "profit"},
                {"убыток", "loss"}, {"сколько", "how much"}, {"зарплата", "salary"}
        };
        for (String[] pair : pairs) {
            ruToEn.put(pair[0], pair[1]);
            enToRu.put(pair[1], pair[0]);
        }
    }

    private void addDefaultEnRu() {
        String[][] pairs = {
                {"how much", "сколько"}, {"tell me", "скажите мне"}, {"help", "помощь"},
                {"please", "пожалуйста"}, {"thank you", "спасибо"},
                {"good", "хорошо"}, {"bad", "плохо"}, {"great", "отлично"},
                {"spending", "расходы"}, {"earning", "заработок"}, {"saving", "экономия"},
                {"did i spend", "я потратил"}, {"my expenses", "мои расходы"},
                {"my income", "мой доход"}, {"give me advice", "дай мне совет"}
        };
        for (String[] pair : pairs) {
            enToRu.put(pair[0], pair[1]);
        }
    }

    public void setLanguage(String language) {
        this.currentLanguage = language;
        saveLanguagePreference();
    }

    private void saveLanguagePreference() {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit().putString("chat_language", currentLanguage).apply();
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * İstifadəçinin sualını İngilis dilinə tərcümə edir
     */
    public String translateToEnglish(String text) {
        if (currentLanguage.equals("en") || text == null || text.isEmpty()) {
            return text;
        }

        String lowerText = text.toLowerCase();

        // Əvvəlcə tam ifadələri yoxla
        String result = translatePhrases(lowerText);
        if (!result.equals(lowerText)) {
            return result;
        }

        // Sonra sözləri tərcümə et
        String[] words = lowerText.split("\\s+");
        StringBuilder translated = new StringBuilder();

        for (String word : words) {
            String translatedWord = word;
            if (currentLanguage.equals("az")) {
                translatedWord = azToEn.getOrDefault(word, word);
            } else if (currentLanguage.equals("ru")) {
                translatedWord = ruToEn.getOrDefault(word, word);
            }
            translated.append(translatedWord).append(" ");
        }

        return translated.toString().trim();
    }

    private String translatePhrases(String text) {
        Map<String, String> phrases = new HashMap<>();
        if (currentLanguage.equals("az")) {
            phrases.put("nə qədər xərclədim", "how much did i spend");
            phrases.put("nə qədər qənaət etdim", "how much did i save");
            phrases.put("mənə məsləhət ver", "give me advice");
            phrases.put("gəlir və xərcləri müqayisə et", "compare income and expenses");
            phrases.put("proqnoz ver", "give a prediction");
            phrases.put("cari balans", "current balance");
            phrases.put("aylıq maaş", "monthly salary");
        } else if (currentLanguage.equals("ru")) {
            phrases.put("сколько я потратил", "how much did i spend");
            phrases.put("сколько я сэкономил", "how much did i save");
            phrases.put("дай мне совет", "give me advice");
            phrases.put("сравни доходы и расходы", "compare income and expenses");
            phrases.put("дай прогноз", "give a prediction");
            phrases.put("текущий баланс", "current balance");
            phrases.put("месячная зарплата", "monthly salary");
        }

        for (Map.Entry<String, String> entry : phrases.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return text;
    }

    /**
     * Modeldən gələn İngilis cavabını istifadəçinin dilinə tərcümə edir
     */
    public String translateFromEnglish(String englishText) {
        if (currentLanguage.equals("en") || englishText == null || englishText.isEmpty()) {
            return englishText;
        }

        String lowerText = englishText.toLowerCase();

        // Əvvəlcə tam ifadələri yoxla
        String result = translatePhrasesFromEn(lowerText);
        if (!result.equals(lowerText)) {
            return result;
        }

        // Sonra sözləri tərcümə et
        String[] words = lowerText.split("\\s+");
        StringBuilder translated = new StringBuilder();

        for (String word : words) {
            String translatedWord = word;
            if (currentLanguage.equals("az")) {
                translatedWord = enToAz.getOrDefault(word, word);
            } else if (currentLanguage.equals("ru")) {
                translatedWord = enToRu.getOrDefault(word, word);
            }
            translated.append(translatedWord).append(" ");
        }

        String resultText = translated.toString().trim();
        resultText = capitalizeFirstLetter(resultText);

        return resultText;
    }

    private String translatePhrasesFromEn(String text) {
        Map<String, String> phrases = new HashMap<>();
        if (currentLanguage.equals("az")) {
            phrases.put("how much did i spend", "Nə qədər xərclədim?");
            phrases.put("how much did i save", "Nə qədər qənaət etdim?");
            phrases.put("give me advice", "Mənə məsləhət ver");
            phrases.put("compare income and expenses", "Gəlir və xərcləri müqayisə et");
            phrases.put("give a prediction", "Proqnoz ver");
            phrases.put("total income", "Ümumi gəlir");
            phrases.put("total expenses", "Ümumi xərclər");
            phrases.put("net savings", "Xalis qənaət");
            phrases.put("current balance", "Cari balans");
            phrases.put("monthly salary", "Aylıq maaş");
            phrases.put("excellent savings rate", "Əla qənaət nisbəti");
            phrases.put("good savings rate", "Yaxşı qənaət nisbəti");
            phrases.put("reduce expenses", "Xərcləri azaldın");
            phrases.put("you don't have any expenses", "Hələ heç bir xərciniz yoxdur");
            phrases.put("you don't have any income", "Hələ heç bir gəliriniz yoxdur");
        } else if (currentLanguage.equals("ru")) {
            phrases.put("how much did i spend", "Сколько я потратил?");
            phrases.put("how much did i save", "Сколько я сэкономил?");
            phrases.put("give me advice", "Дай мне совет");
            phrases.put("compare income and expenses", "Сравни доходы и расходы");
            phrases.put("give a prediction", "Дай прогноз");
            phrases.put("total income", "Общий доход");
            phrases.put("total expenses", "Общие расходы");
            phrases.put("net savings", "Чистая экономия");
            phrases.put("current balance", "Текущий баланс");
            phrases.put("monthly salary", "Месячная зарплата");
            phrases.put("excellent savings rate", "Отличная норма сбережений");
            phrases.put("good savings rate", "Хорошая норма сбережений");
            phrases.put("reduce expenses", "Сократите расходы");
        }

        for (Map.Entry<String, String> entry : phrases.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return text;
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}