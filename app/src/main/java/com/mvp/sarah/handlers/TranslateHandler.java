package com.mvp.sarah.handlers;

import android.content.Context;
import android.os.AsyncTask;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "translate 'hello' to spanish",
            "say 'good morning' in french"
    );
    private static final Pattern[] TRANSLATE_PATTERNS = new Pattern[] {
        Pattern.compile("(?:translate|say) '([^']+)' (?:to|in) ([a-zA-Z ]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:translate|say) ([^ ]+) (?:to|in) ([a-zA-Z ]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("how do you say '([^']+)' in ([a-zA-Z ]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("how do you say ([^ ]+) in ([a-zA-Z ]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("what is '([^']+)' in ([a-zA-Z ]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("what is ([^ ]+) in ([a-zA-Z ]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("translate ([^ ]+) ([a-zA-Z ]+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([a-zA-Z ]+) for '([^']+)'", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([a-zA-Z ]+) for ([^ ]+)", Pattern.CASE_INSENSITIVE)
    };
    private static final Map<String, String> langMap = new HashMap<>();
    static {
        langMap.put("french", "fr");
        langMap.put("spanish", "es");
        langMap.put("german", "de");
        langMap.put("italian", "it");
        langMap.put("japanese", "ja");
        langMap.put("korean", "ko");
        // Indian languages
        langMap.put("hindi", "hi");
        langMap.put("bengali", "bn");
        langMap.put("telugu", "te");
        langMap.put("marathi", "mr");
        langMap.put("tamil", "ta");
        langMap.put("urdu", "ur");
        langMap.put("gujarati", "gu");
        langMap.put("kannada", "kn");
        langMap.put("odia", "or");
        langMap.put("punjabi", "pa");
        langMap.put("malayalam", "ml");
        langMap.put("assamese", "as");
        langMap.put("maithili", "mai");
        langMap.put("santali", "sat");
        langMap.put("kashmiri", "ks");
        langMap.put("nepali", "ne");
        langMap.put("konkani", "kok");
        langMap.put("dogri", "doi");
        langMap.put("manipuri", "mni");
        langMap.put("bodo", "brx");
        langMap.put("sindhi", "sd");
        langMap.put("sanskrit", "sa");
        langMap.put("tulu", "tcy");
        langMap.put("meitei", "mni");
        langMap.put("rajasthani", "raj");
        langMap.put("bhili", "bhb");
        langMap.put("garhwali", "gbm");
        langMap.put("mizo", "lus");
        langMap.put("khasi", "kha");
        langMap.put("gondi", "gon");
        langMap.put("tripuri", "trp");
        langMap.put("sylheti", "syl");
        langMap.put("nagpuri", "nag");
        langMap.put("chhattisgarhi", "hne");
        langMap.put("magahi", "mag");
        langMap.put("ho", "hoc");
        langMap.put("bhojpuri", "bho");
        langMap.put("marwari", "mwr");
        langMap.put("awadhi", "awa");
        langMap.put("saraiki", "skr");
        langMap.put("lambadi", "lmn");
        langMap.put("kumaoni", "kfy");
        langMap.put("pahari", "phr");
        langMap.put("malvi", "mup");
        langMap.put("bhutia", "sjt");
        langMap.put("lepcha", "lep");
        langMap.put("angika", "anp");
        // Add more languages as needed
    }

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.startsWith("translate") || lower.startsWith("say") || lower.contains("translate") || lower.contains("say");
    }

    @Override
    public void handle(Context context, String command) {
        String textToTranslate = null;
        String targetLangName = null;
        for (Pattern pattern : TRANSLATE_PATTERNS) {
            Matcher matcher = pattern.matcher(command);
            if (matcher.find()) {
                if (pattern.pattern().contains("for")) {
                    // Patterns like 'hindi for hello'
                    targetLangName = matcher.group(1).toLowerCase().trim();
                    textToTranslate = matcher.group(2);
                } else {
                    textToTranslate = matcher.group(1);
                    targetLangName = matcher.group(2).toLowerCase().trim();
                }
                break;
            }
        }
        if (textToTranslate != null && targetLangName != null) {
            targetLangName = targetLangName.replaceAll(" language$", ""); // Remove trailing ' language' if present
            targetLangName = targetLangName.replaceAll(" +", " "); // Normalize spaces
            String targetLangCode = langMap.get(targetLangName);
            if (targetLangCode == null) {
                // Try to match ignoring spaces (e.g., 'bhojpuri' vs 'bhoj puri')
                targetLangCode = langMap.get(targetLangName.replaceAll(" ", ""));
            }
            if (targetLangCode == null) {
                FeedbackProvider.speakAndToast(context, "I don't know how to translate to " + targetLangName + " yet.");
                return;
            }
            new FetchTranslationTask(context, textToTranslate, targetLangCode).execute();
        } else {
            FeedbackProvider.speakAndToast(context, "Please say: translate 'phrase' to [language], or try 'how do you say hello in hindi'.");
        }
    }

    private static class FetchTranslationTask extends AsyncTask<Void, Void, String> {
        private final Context context;
        private final String text, langCode;

        FetchTranslationTask(Context context, String text, String langCode) {
            this.context = context;
            this.text = text;
            this.langCode = langCode;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // Step 1: Detect source language
                String detectUrlStr = "https://api.mymemory.translated.net/get?q=" +
                        URLEncoder.encode(text, "UTF-8") + "&langpair=auto|" + langCode;
                URL detectUrl = new URL(detectUrlStr);
                HttpURLConnection detectConn = (HttpURLConnection) detectUrl.openConnection();
                detectConn.setRequestMethod("GET");
                BufferedReader detectReader = new BufferedReader(new InputStreamReader(detectConn.getInputStream()));
                StringBuilder detectResponse = new StringBuilder();
                String detectLine;
                while ((detectLine = detectReader.readLine()) != null) {
                    detectResponse.append(detectLine);
                }
                detectReader.close();
                JSONObject detectJson = new JSONObject(detectResponse.toString());
                String detectedLang = detectJson.getJSONObject("responseData").getString("match");
                // fallback: try to get from responseData if match is empty
                if (detectedLang == null || detectedLang.isEmpty()) {
                    detectedLang = detectJson.optString("detectedLanguage", "en");
                }
                // Step 2: Translate using detected language
                String urlStr = "https://api.mymemory.translated.net/get?q=" +
                        URLEncoder.encode(text, "UTF-8") + "&langpair=" + detectedLang + "|" + langCode;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                JSONObject json = new JSONObject(response.toString());
                String translatedText = json.getJSONObject("responseData").getString("translatedText");
                String detectedLangName = detectedLang;
                // Try to map detectedLang to a language name
                for (Map.Entry<String, String> entry : langMap.entrySet()) {
                    if (entry.getValue().equalsIgnoreCase(detectedLang)) {
                        detectedLangName = entry.getKey();
                        break;
                    }
                }
                return "[Detected: " + detectedLangName + "] " + translatedText;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                FeedbackProvider.speakAndToast(context, result);
            } else {
                FeedbackProvider.speakAndToast(context, "Sorry, I couldn't translate that right now.");
            }
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 