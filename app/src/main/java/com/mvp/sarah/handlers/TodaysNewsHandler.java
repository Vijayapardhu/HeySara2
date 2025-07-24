package com.mvp.sarah.handlers;

import android.content.Context;
import android.os.AsyncTask;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TodaysNewsHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "today's news",
        "news headlines",
        "read the news",
        "today news in [location]"
    );
    private static final String NEWS_API_KEY = "4f97fca13227458c8b5e34b06dffb30a";
    private static final String DEFAULT_COUNTRY = "in";
    private static final Map<String, String> COUNTRY_CODES = new HashMap<>();
    static {
        COUNTRY_CODES.put("india", "in");
        COUNTRY_CODES.put("united states", "us");
        COUNTRY_CODES.put("usa", "us");
        COUNTRY_CODES.put("uk", "gb");
        COUNTRY_CODES.put("great britain", "gb");
        COUNTRY_CODES.put("canada", "ca");
        COUNTRY_CODES.put("australia", "au");
        // Add more as needed
    }

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("today's news") ||
               lower.contains("news headlines") ||
               lower.contains("read the news") ||
               lower.matches(".*today news in .*");
    }

    @Override
    public void handle(Context context, String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        String country = DEFAULT_COUNTRY;
        String query = null;
        // Parse location
        boolean isGlobal = lower.contains("worldwide") || lower.contains("global") || lower.contains("international");
        if (isGlobal) {
            country = null;
            query = null;
        } else if (lower.matches(".*today news in .+")) {
            String[] parts = lower.split("today news in ", 2);
            if (parts.length > 1) {
                String location = parts[1].trim();
                if (COUNTRY_CODES.containsKey(location)) {
                    country = COUNTRY_CODES.get(location);
                } else {
                    query = location;
                }
            }
        }
        FeedbackProvider.speakAndToast(context, "Fetching " + (isGlobal ? "worldwide top news" : (query != null ? "news for " + query : country.equals(DEFAULT_COUNTRY) ? "news for India" : "news for country code " + country)) + "...");
        new FetchNewsTask(context, country, query).execute();
    }

    private static class FetchNewsTask extends AsyncTask<Void, Void, String> {
        private final Context context;
        private final String country;
        private final String query;
        FetchNewsTask(Context ctx, String country, String query) {
            this.context = ctx; this.country = country; this.query = query;
        }
        @Override
        protected String doInBackground(Void... voids) {
            try {
                StringBuilder urlBuilder = new StringBuilder("https://newsapi.org/v2/top-headlines?");
                if (query != null && !query.isEmpty()) {
                    urlBuilder.append("q=").append(URLEncoder.encode(query, "UTF-8")).append("&");
                }
                if (country != null && !country.isEmpty()) {
                    urlBuilder.append("country=").append(country).append("&");
                }
                urlBuilder.append("apiKey=").append(NEWS_API_KEY);
                URL url = new URL(urlBuilder.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                JSONObject json = new JSONObject(sb.toString());
                JSONArray articles = json.getJSONArray("articles");
                StringBuilder headlines = new StringBuilder();
                int count = Math.min(3, articles.length());
                for (int i = 0; i < count; i++) {
                    JSONObject article = articles.getJSONObject(i);
                    String title = article.getString("title");
                    headlines.append(i + 1).append(": ").append(title).append(". ");
                }
                return "Top headlines: " + headlines.toString();
            } catch (Exception e) {
                return "Sorry, I couldn't fetch the news right now.";
            }
        }
        @Override
        protected void onPostExecute(String news) {
            FeedbackProvider.speakAndToast(context, news);
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 